# CLAUDE.md: hexagonal-architecture-in-practice

Contexto para Claude Code. Responde siempre en español.

## Qué es este proyecto

Material y código de la charla técnica **"Arquitectura hexagonal en la práctica: del diseño al código"**, que da Luis Perilla (Peri) el **4 de diciembre de 2026**. Duración objetivo: 50 minutos más 10 de preguntas. La charla incluye **código en vivo**.

La charla se organiza en tres bloques, que son los del cartel de difusión y deben verse claramente en la presentación:

1. **Puertos y adaptadores**
2. **Buenas y malas prácticas**
3. **Ventajas y decisiones de diseño**

El caso central es **adoptar una mascota**, que se migra paso a paso desde un proyecto MVC en capas hasta una arquitectura hexagonal multimódulo, sin cambiar el comportamiento. Alrededor de él se construye un pequeño ecosistema de microservicios de una tienda de mascotas, por fases.

Regla de negocio central: una mascota solo puede adoptarse si está disponible. Al adoptarla, queda asociada a su nuevo dueño.

## Stack

| Pieza | Elección |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.2 |
| Construcción | Maven multimódulo, con Maven Wrapper en la raíz |
| Código repetitivo | Lombok |
| Contenedores | Podman (`podman compose` con `compose.yaml`) |
| Persistencia | MongoDB |
| Mensajería | RabbitMQ (Spring AMQP) |
| API síncrona | REST con **API first**: el contrato OpenAPI se escribe primero y el código se genera con `openapi-generator-maven-plugin` |
| API SOAP | SOAP con **contract first**: el XSD y el WSDL se escriben primero y las clases se generan con un plugin JAXB; endpoints con Spring Web Services |
| Correo de pruebas | Mailpit |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers (MongoDB y RabbitMQ) y ArchUnit |

### Notas de versión que hay que comprobar, no suponer

- **Spring Boot 4** cambia nombres de algunos starters, paquetes de las anotaciones de test (`@WebMvcTest`, `@DataMongoTest`) y pasa a Jackson 3. Comprobar cada uno en la documentación de la versión 4.1.2 antes de escribir código.
- **Java 25 y Lombok:** desde Java 23 el procesamiento de anotaciones no se activa solo. Declarar Lombok en `annotationProcessorPaths` de `maven-compiler-plugin` (en `pluginManagement` del POM raíz) y usar una versión de Lombok con soporte para Java 25.
- **Generadores:** usar versiones de `openapi-generator-maven-plugin` y del plugin JAXB compatibles con Spring Boot 4, Jakarta EE y Jackson 3.
- **Testcontainers** con la versión que gestiona Spring Boot 4.1.2; los nombres de artefactos y paquetes pueden haber cambiado respecto a versiones anteriores.
- Si una versión no se resuelve o hay dudas de compatibilidad, **preguntar a Peri en lugar de inventar otra**.

## Organización del repositorio

Monorepo con un **POM agregador** en la raíz y un servicio por carpeta. La estructura de carpetas es la misma en las dos ramas; lo que cambia es la arquitectura interna de cada servicio.

```text
hexagonal-architecture-in-practice/
├── pom.xml                   POM agregador (packaging pom)
├── mvnw, mvnw.cmd, .mvn/     Maven Wrapper
├── compose.yaml              MongoDB, RabbitMQ, Mailpit y los servicios (Podman)
├── requests.http             Peticiones REST de demo
├── soap-requests/            Sobres SOAP de ejemplo
├── contracts/events/         Ejemplo JSON y descripción de cada evento de RabbitMQ
├── services/
│   ├── adoption-service/
│   ├── notification-service/
│   ├── pet-registry-service/
│   └── adopter-service/
├── docs/                     Presentación, guía de teoría y cartel
├── README.md
└── CLAUDE.md
```

### POM raíz

- Hereda de `spring-boot-starter-parent` 4.1.2: todos los servicios comparten la gestión de versiones de Spring Boot, incluidas Lombok, Testcontainers, JUnit y AssertJ.
- Declara `<modules>` con cada servicio y fija propiedades comunes: `java.version` 25 y las versiones de lo que Spring Boot no gestiona (ArchUnit, generador OpenAPI, plugin JAXB).
- En `pluginManagement`: `maven-compiler-plugin` con Lombok como procesador de anotaciones, y la configuración base de los generadores.
- **Sin dependencias en el POM raíz**: cada módulo declara solo lo que usa.
- Comandos: `./mvnw verify` construye todo; `./mvnw -pl services/adoption-service -am verify` construye un solo servicio.

### Identificadores Maven

Los módulos de los servicios hexagonales **no llevan prefijo**: se llaman `domain`, `application`, `infrastructure`, etc. Para que no choquen entre servicios, **cada servicio usa su propio `groupId`**:

| Servicio | `groupId` | `artifactId` del agregador del servicio |
|---|---|---|
| Adopción | `com.example.petstore.adoption` | `adoption-service` |
| Notificaciones | `com.example.petstore.notification` | `notification-service` |
| Registro | `com.example.petstore.registry` | `pet-registry-service` |
| Adoptantes | `com.example.petstore.adopter` | `adopter-service` |

Así, `com.example.petstore.adoption:domain` y `com.example.petstore.notification:domain` son artefactos distintos en el mismo reactor. Los paquetes Java siguen el `groupId`: `com.example.petstore.adoption.domain.model`, etc.

**Sin código compartido entre servicios.** Cada servicio genera o define sus propias clases a partir de los contratos. Compartir una librería de DTOs acoplaría los despliegues: es una decisión de diseño que se presenta en la charla.

### Servicios

| Servicio | Puerto | Base de datos | RabbitMQ | Entrada | Arquitectura en `mvc` | Arquitectura en `hexagonal` |
|---|---|---|---|---|---|---|
| `adoption-service` | 8080 | `petstore-adoption` | Publica `pet.adopted` | REST y SOAP | Capas | Hexagonal multimódulo |
| `notification-service` | 8083 | Ninguna | Consume `pet.adopted` | Mensajes | Capas | Hexagonal multimódulo (sin módulos de API) |
| `pet-registry-service` | 8081 | `petstore-registry` | Consume `pet.adopted` | REST | Capas | **Capas también**, ver decisión de diseño |
| `adopter-service` | 8082 | `petstore-adopters` | Ninguno | SOAP | Capas | Hexagonal multimódulo |

Infraestructura en `compose.yaml`: MongoDB 27017, RabbitMQ 5672 (consola de gestión en 15672), Mailpit 1025 (SMTP) y 8025 (web). Todas las bases de datos viven en la misma instancia de MongoDB, pero cada servicio solo accede a la suya.

**Mensajería:** un exchange de tipo topic, `petstore.events`. Cada consumidor tiene su propia cola enlazada con la routing key del evento (`notification.pet-adopted`, `pet-registry.pet-adopted`) y su cola de mensajes fallidos (`<cola>.dlq`). Los mensajes van en JSON, y cada consumidor los deserializa a su propia clase sin depender de los nombres de clase del productor.

## Ramas

| Rama | Contenido |
|---|---|
| `mvc` | Todos los servicios en arquitectura MVC por capas técnicas, en un único módulo Maven por servicio |
| `hexagonal` | Nace de `mvc`. Primero la migración paso a paso de `adoption-service` (etiquetas `00-start` a `09-final`) y después el resto de fases |
| `main` | Apunta al final de `hexagonal`. Es la rama por defecto |

Las dos ramas **no se fusionan nunca** entre sí después de separarse. Cada fase se implementa en las dos ramas por separado, con el mismo comportamiento externo: mismos contratos OpenAPI, mismos WSDL, mismos eventos y mismas respuestas. Así `compose.yaml`, `requests.http` y los sobres SOAP funcionan igual en ambas, y se pueden comparar con `git diff mvc hexagonal -- services/<servicio>`.

### Etiquetas

Migración de `adoption-service` en la rama `hexagonal`. **Cada paso crea o completa un módulo Maven**, de modo que la estructura final se va construyendo delante de la audiencia:

| Etiqueta | Qué cambia | Módulos afectados |
|---|---|---|
| `00-start` | Punta de `mvc`: proyecto en capas con REST y SOAP | Un único módulo |
| `01-domain` | Modelo de dominio con la regla de adopción, en Java puro | `domain` |
| `02-input-port` | Caso de uso, comando y resultado; el servicio antiguo lo implementa como paso intermedio | `application` |
| `03-output-ports` | Puertos de salida en el dominio y servicio de aplicación, probado sin Spring | `domain`, `application` |
| `04-mongodb-adapter` | Adaptador de persistencia | `infrastructure` |
| `05-rabbitmq-adapter` | Adaptador de publicación de eventos | `infrastructure` |
| `06-rest-adapter` | Contrato OpenAPI, adaptador REST y arranque; se borra el código acoplado y el endpoint SOAP antiguo pasa a usar el caso de uso | `api-contract`, `api-rest`, `boot` |
| `07-soap-adapter` | Contrato SOAP y adaptador SOAP: **el mismo caso de uso, un segundo canal de entrada** | `soap-contract`, `api-soap` |
| `08-testing` | Adaptador en memoria, contrato común de persistencia y ArchUnit | `infrastructure`, `boot` |
| `09-final` | Documentación y material de la charla | |

Al final de cada fase posterior se crean las etiquetas `mvc/fase-N-<nombre>` y `hexagonal/fase-N-<nombre>`. Por ejemplo, `hexagonal/fase-2-notificaciones`.

Todas las etiquetas son anotadas, y cada una debe compilar, arrancar y pasar `./mvnw verify`.

## Estructura MVC de un servicio (rama `mvc`)

Un único módulo Maven por servicio, organizado por capa técnica, idiomático, como lo escribiría un equipo real. La generación de código a partir de los contratos también se usa aquí: API first y contract first no dependen de la arquitectura.

```text
services/adoption-service/
├── pom.xml
└── src/main/
    ├── resources/
    │   ├── openapi/adoption-api.yaml       Contrato REST (se genera en target/generated-sources)
    │   ├── wsdl/adoption.wsdl, adoption-types.xsd  Contrato SOAP (se genera en target/generated-sources)
    │   └── application.yml
    └── java/com/example/petstore/adoption/
        ├── AdoptionServiceApplication.java
        ├── controller/     Controladores REST que implementan las interfaces generadas
        ├── soap/           Endpoints SOAP
        ├── service/        Lógica de negocio y orquestación
        ├── repository/     Interfaces de Spring Data MongoDB
        ├── model/          Documentos de MongoDB (@Document, @Data)
        ├── client/         Clientes HTTP o SOAP hacia otros servicios
        ├── messaging/      Publicadores y listeners de RabbitMQ
        ├── exception/      Excepciones, incluidas las que llevan estado HTTP
        └── config/         Configuración y datos de demo
```

En `adoption-service`, el acoplamiento debe ser visible porque es el material del bloque de malas prácticas:

- La regla de negocio está en el servicio y compara un `String` del documento de MongoDB.
- El documento de MongoDB, con `@Data`, hace de modelo de negocio.
- El servicio lanza `ResponseStatusException`, así que **el endpoint SOAP tiene que capturar una excepción HTTP para convertirla en un fault SOAP**: es el ejemplo más claro de por qué el negocio no debe conocer el canal.
- El servicio depende directamente de `RabbitTemplate`.

Tests: unitarios del servicio con Mockito.

## Estructura hexagonal (rama `hexagonal`)

Cada servicio hexagonal es un agregador Maven (`packaging pom`) con estos módulos. Un servicio solo crea los módulos que necesita.

```text
services/adoption-service/
├── pom.xml              Agregador del servicio
├── api-contract/        Contrato OpenAPI y código generado (interfaces de API y DTOs)
├── soap-contract/       XSD y WSDL, y clases JAXB generadas
├── domain/              Modelo, reglas, excepciones, eventos y puertos de salida
├── application/         Casos de uso: puertos de entrada, comandos, resultados y servicios de aplicación
├── infrastructure/      TODOS los adaptadores de infraestructura: persistencia, mensajería y clientes externos
├── api-rest/            Adaptador REST: controladores, mappers y manejo de errores
├── api-soap/            Adaptador SOAP: endpoints, mappers y faults
└── boot/                Arranque, configuración y composición de beans
```

### Contenido de cada módulo

| Módulo | Paquetes principales | Contenido en `adoption-service` |
|---|---|---|
| `domain` | `domain.model`, `domain.exception`, `domain.event`, `domain.port` | `Pet`, `PetId`, `Owner`, `PetStatus`; `PetNotAvailableException`, `PetNotFoundException`; `PetAdopted`; `LoadPetPort`, `SavePetPort`, `PublishPetAdoptedPort` |
| `application` | `application.port.in`, `application.service` | `AdoptPetUseCase`, `AdoptPetCommand`, `AdoptionResult`; `AdoptPetService` |
| `infrastructure` | `infrastructure.mongodb`, `infrastructure.rabbitmq`, `infrastructure.inmemory`, `infrastructure.client` | `MongoPetAdapter`, `PetDocument`, `PetMongoMapper`, `SpringDataPetRepository`; `RabbitPetAdoptedPublisher`, `PetAdoptedMessage`, configuración de exchange; `InMemoryPetAdapter` |
| `api-contract` | Generados desde `openapi/adoption-api.yaml` | Interfaz de la API y DTOs de petición y respuesta |
| `api-rest` | `api.rest` | Controlador que implementa la interfaz generada, mapper DTO a comando, manejador de errores (`ProblemDetail`) |
| `soap-contract` | Generados desde `adoption-types.xsd` | Clases JAXB de petición, respuesta y faults |
| `api-soap` | `api.soap` | Endpoint, mapper y traducción de excepciones de dominio a faults SOAP |
| `boot` | `boot`, `boot.config` | `AdoptionServiceApplication`, registro de `AdoptPetService` como bean, `application.yml`, perfil `in-memory`, datos de demo, ArchUnit y tests de arranque |

Los **puertos de salida viven en `domain`** (`domain.port`), y los de entrada en `application` (`application.port.in`). En la charla hay que explicar que es una variante válida: lo que importa es que el núcleo define los puertos y la tecnología los implementa.

Los adaptadores de entrada por mensajería (listeners de RabbitMQ) también viven en `infrastructure`, en un subpaquete propio (`infrastructure.rabbitmq.listener`). El módulo `infrastructure` agrupa toda la tecnología de integración, de entrada y de salida, excepto las APIs que el servicio publica (`api-rest` y `api-soap`).

### Dependencias entre módulos (las impone Maven)

```text
domain          ← sin dependencias (solo Lombok como provided)
application     → domain
infrastructure  → domain, application (y Spring Data MongoDB, Spring AMQP, clientes)
api-contract    → solo lo necesario para el código generado
soap-contract   → solo Jakarta XML Binding
api-rest        → application, api-contract, Spring Web MVC
api-soap        → application, soap-contract, Spring Web Services
boot            → todos los anteriores; único módulo con spring-boot-maven-plugin
```

- `api-rest` y `api-soap` **no dependen de `infrastructure`**, ni al revés.
- Si alguien importa Spring o MongoDB en `domain` o en `application`, **el proyecto no compila**. ArchUnit, en `boot`, comprueba además las reglas que Maven no puede ver (por ejemplo, que `domain.port` no use tipos de `infrastructure`).
- Solo `boot` produce el JAR ejecutable.

### Uso de Lombok

- **Dominio:** se permite porque solo actúa en compilación, pero **nunca `@Data` ni `@Setter`** en entidades: los setters permitirían saltarse las invariantes. Sí `@Getter`, `@RequiredArgsConstructor`, `@EqualsAndHashCode` y `@ToString` cuando aporten. Los objetos valor, comandos y eventos se escriben como `record`.
- **Adaptadores:** `@RequiredArgsConstructor` para inyección por constructor y `@Data` en documentos de MongoDB si se quiere.
- **Rama `mvc`:** `@Data` en el documento usado como modelo de negocio. Es parte del material de malas prácticas.

### Reglas de arquitectura (no romperlas)

- `domain` no depende de `application`, de la infraestructura ni de Spring.
- `application` no depende de la infraestructura ni de Spring. Los servicios de aplicación no llevan anotaciones de Spring: se registran como beans en `boot`.
- Los puertos se nombran por capacidad, nunca por tecnología: `SavePetPort`, no `MongoPetRepositoryPort`.
- Los puertos usan tipos de dominio, nunca DTOs generados, clases JAXB, documentos de MongoDB ni mensajes de RabbitMQ.
- Los adaptadores solo traducen y no contienen reglas de negocio. Mappers, modelos técnicos, timeouts, reintentos y fallbacks viven en el adaptador.
- Las excepciones de dominio se traducen a HTTP solo en `api-rest` y a faults SOAP solo en `api-soap`.
- El código generado (OpenAPI y JAXB) **nunca** se usa fuera de su módulo de API ni de su cliente en `infrastructure`.

## Contratos

Los contratos REST y SOAP son la fuente de verdad (**API first** y **contract first**): se escriben antes que el código y viven en `api-contract` y `soap-contract` (o en `src/main/resources` en la rama `mvc`). Los cambios deben ser compatibles hacia atrás: solo se añaden campos u operaciones, nunca se renombran ni se eliminan.

| Contrato | Definición |
|---|---|
| REST de adopción | `POST /pets/{petId}/adoption`. Fases 1 a 3: cuerpo `{"ownerName"}`. Fase 4: `{"adopterId"}`. Respuestas 200, 400, 404, 409 y, desde la fase 4, 422 (adoptante no apto) y 503 (servicio de adoptantes no disponible) |
| SOAP de adopción | Operación `AdoptPet`, namespace `http://example.com/petstore/adoption`, en `/ws`. Petición `{petId, ownerName}`, respuesta `{petId, petName, status, ownerName}`. Faults `PetNotFound` y `PetNotAvailable` |
| REST del registro | `POST /pets`, `GET /pets/{id}`, `GET /pets?status=AVAILABLE`. Ficha: `{"id", "name", "species", "status"}` |
| SOAP de adoptantes | Operaciones `RegisterAdopter` y `CheckEligibility`. Respuesta de elegibilidad: `{eligible, reason}` |
| Evento `pet.adopted` | `{"petId", "petName", "ownerName", "adoptedAt"}`. Desde la fase 4 se añade `adopterId` |

## Fases

Cada fase termina con las dos ramas en verde, el README actualizado y una revisión de Peri antes de empezar la siguiente.

**Antes de la charla del 4 de diciembre hay que cerrar las fases 0, 1 y 2.** Las fases 3 y 4 son material ampliado, y solo se abordan si queda tiempo. La fase 5 son ideas, no se implementa.

### Fase 0: entorno y base

- Comprobar Java 25, Maven y Podman en la máquina de Peri. Configurar Testcontainers para Podman: `DOCKER_HOST` apuntando al socket de Podman y, si el contenedor Ryuk falla, desactivarlo. Seguir la documentación de Testcontainers para Podman.
- Comprobar que se resuelven Spring Boot 4.1.2, Lombok, los generadores y Testcontainers. Dejar las versiones en el POM raíz.
- Crear el POM raíz, el Maven Wrapper, `compose.yaml` (MongoDB, RabbitMQ y Mailpit) y el README base.
- Existe un repositorio anterior, `petstore-hexagonal` (Java 21, Spring Boot 3.5, Kafka, sin Lombok, un único módulo), con etiquetas `00-start` a `08-final`. **Sirve solo como referencia** de dominio, tests y casos de demo; el código se reescribe con el nuevo stack.

### Fase 1: `adoption-service` en las dos ramas

- Rama `mvc`: el servicio en capas, con REST (API first) y SOAP (contract first), MongoDB y RabbitMQ.
- Rama `hexagonal`: la migración de `00-start` a `09-final`, un commit y una etiqueta por paso.
- `main` apunta al final de `hexagonal`.

### Fase 2: `notification-service` (prioritaria para la charla)

Cuando se completa una adopción, envía un email al refugio: "Luna ha sido adoptada por Ana". La dirección del refugio se configura en `application.yml`. Los emails se ven en Mailpit (http://localhost:8025).

- Hexagonal, con los módulos `domain`, `application`, `infrastructure` y `boot`, sin módulos de API: este servicio no publica ninguna.
- En `infrastructure`: listener de RabbitMQ (adaptador de entrada) que invoca `NotifyAdoptionUseCase`, y dos implementaciones del puerto `SendNotificationPort`: SMTP (hacia Mailpit) y log (perfil `log-notifications`).
- Idempotencia: un mismo mensaje puede llegar dos veces. El caso de uso no debe enviar dos emails por la misma adopción. Para la demo basta un registro en memoria de mensajes procesados, detrás de un puerto.
- Cola de mensajes fallidos para los mensajes que no se puedan procesar.
- **Valor para la charla:** un adaptador de entrada que no es REST ni SOAP. Es la integración que se enseña en vivo.

### Fase 3: `pet-registry-service` (material ampliado)

El registro pasa a ser el dueño de las mascotas: alta, consulta y listado por estado (REST, API first). Escucha `pet.adopted` y marca la mascota como adoptada.

- **Decisión de diseño:** es un CRUD casi sin reglas, así que **se queda en capas también en la rama `hexagonal`**. Ilustra en código que aplicar hexagonal a un CRUD trivial no compensa.
- Cambios en `adoption-service`:
  - Deja de ser dueño de las mascotas. Las consulta al registro por HTTP mediante un puerto de salida (`FindPetPort`), con un cliente en `infrastructure.client` generado a partir del contrato OpenAPI del registro.
  - Guarda sus propias adopciones en un agregado `Adoption`.
  - Nueva regla, que se comprueba localmente: una mascota no puede tener dos adopciones. Cubre el intervalo en que el registro todavía no ha procesado el evento (consistencia eventual).
  - **Valor para la charla:** el caso de uso no cambia de forma; cambia el adaptador de MongoDB por uno HTTP.

### Fase 4: `adopter-service` (material ampliado)

Registro de adoptantes con nombre, email y fecha de nacimiento. Expone, por **SOAP con contract first**, el alta y la elegibilidad (mayor de edad y no bloqueado). Simula un sistema heredado.

- Hexagonal: `soap-contract`, `domain`, `application`, `infrastructure`, `api-soap` y `boot`. Las reglas de elegibilidad están en el dominio.
- Cambios en `adoption-service`:
  - El cuerpo de la petición REST pasa a `{"adopterId"}`, y la operación SOAP equivalente también.
  - Puerto de salida `CheckAdopterEligibilityPort`, implementado en `infrastructure.client` con un **cliente SOAP** generado desde el WSDL de adoptantes. Timeout, reintento y error 503 si el servicio no responde: todo en el adaptador, nada en el dominio.
  - El evento `pet.adopted` añade `adopterId`.
- Notificaciones puede usar el email del adoptante en lugar del email del refugio.
- **Valor para la charla:** SOAP como adaptador de salida. El dominio no sabe que habla con un sistema heredado.

### Fase 5: ideas, no implementar

- **`vet-service`:** vacunas e historial clínico. Ejemplo de capa anticorrupción: "mascota" no significa lo mismo en Veterinaria que en Adopción.
- **Pagos de la tasa de adopción:** proveedor externo con sandbox y compensación si el cobro falla.
- **Tienda:** reserva un kit de bienvenida al escuchar `pet.adopted`.
- **Citas:** visita previa al refugio antes de aprobar la adopción.

## Presentación (`docs/charla-arquitectura-hexagonal.html`)

Página web navegable en un único HTML, sin dependencias, que funciona sin conexión. El código que muestra está en el objeto `SRC` y el guion en `MODULES`. **Si cambia el código del repositorio, hay que actualizar `SRC` para que coincida.**

Teclas: flechas o mando para navegar, S para ver la solución, N para las notas del ponente, P para el modo presentación y T para el cronómetro.

La presentación actual se escribió para el stack anterior (Java 21, Kafka, sin Lombok, un único módulo). **Hay que adaptarla al stack nuevo.**

### Estructura actual y relación con los tres bloques

| Módulo actual | Bloque |
|---|---|
| 0. Portada y punto de partida | Introducción |
| 1. La aplicación que funciona, pero está acoplada (rama `mvc`) | Malas prácticas |
| 2. Teoría: las piezas de la arquitectura hexagonal | Puertos y adaptadores |
| 3 a 6. Pasos 1 a 4: dominio, puerto de entrada, puertos de salida, adaptadores | Puertos y adaptadores (código en vivo) |
| 7. El beneficio visible: sustituir infraestructura | Ventajas |
| 8. Testing por capas | Ventajas |
| 9. Hexagonal, DDD y SOLID | Decisiones de diseño |
| 10. Errores frecuentes y decisiones pragmáticas | Buenas y malas prácticas |
| 11. Cierre | Conclusiones |

### Cambios pendientes en la presentación

1. **Stack nuevo:** sustituir Kafka por RabbitMQ en código, diagramas y textos; usar Lombok en el código mostrado según las reglas de este documento; reflejar Java 25 y Spring Boot 4.1.2; actualizar las etiquetas (`05-rabbitmq-adapter`, `07-soap-adapter`, `08-testing`, `09-final`).
2. **Estructura de módulos:** una pantalla con los módulos (`api-contract`, `soap-contract`, `domain`, `application`, `infrastructure`, `api-rest`, `api-soap`, `boot`) colocados sobre el diagrama hexagonal, con las dependencias que impone Maven. Actualizar la tabla de puertos: los de salida se definen en `domain`.
3. **Adaptador SOAP:** una pantalla que muestre el mismo caso de uso servido por REST y por SOAP, y otra en el bloque de malas prácticas con el endpoint SOAP de la rama `mvc` capturando una excepción HTTP.
4. **API first y contract first:** una pantalla en el bloque de decisiones de diseño sobre escribir los contratos antes que el código, y por qué el código generado no debe salir de su módulo.
5. **Portada:** título y subtítulo del cartel, "Arquitectura hexagonal en la práctica" y "Del diseño al código", con la etiqueta "Código en vivo" y los tres bloques como agenda.
6. **Hacer visibles los tres bloques** en el menú lateral y en la cabecera de cada pantalla.
7. **Bloque "Buenas y malas prácticas":** añadir una pantalla de buenas prácticas en el mismo formato que los errores frecuentes, y una comparación lado a lado entre el código de `mvc` y el de `hexagonal`.
8. **Bloque "Ventajas y decisiones de diseño":** una pantalla de ventajas y costes y otra que reúna las decisiones de diseño: sin `@Data` en el dominio, puertos sin detalles técnicos, `LoadPetPort` y `SavePetPort` separados, publicación sin outbox en la demo, servicio de aplicación sin `@Service`, sin librería de DTOs compartida, un `groupId` por servicio, módulos solo donde hacen falta, el registro como MVC, y cuándo sí y cuándo no.
9. **Integración en vivo (tras la fase 2):** una pantalla sobre `notification-service` como adaptador de entrada por mensajería, con la demo de Mailpit.
10. **Momentos de código en vivo:** marcar en las notas del ponente qué pantallas se hacen en el IDE, con la rama y la etiqueta de partida de cada una.
11. **Tiempo:** la suma actual es de unos 65 minutos para 50 disponibles, y los cambios añaden pantallas. Al reorganizar en bloques, ajustar las duraciones para que sumen 50, usando las pantallas marcadas como recortables y dejando el resto como material de consulta.
12. **Identidad visual (consultar a Peri antes de aplicarla):** el cartel usa fondo oscuro verde petróleo (aprox. `#0E1A1E`), naranja para el núcleo (aprox. `#F47A20`), turquesa para los hexágonos exteriores (aprox. `#2EC4B6`) y texto crema (aprox. `#F3EEE3`). Si se aplica, conservar el modo claro y el oscuro.

El cartel de difusión debe guardarse en `docs/banner-charla.png`.

## Guía de teoría (`docs/guia-teoria-arquitectura-hexagonal.md`)

Guía del ponente. Usa la analogía del restaurante: recetas como dominio, jefe de cocina como servicio de aplicación, comanda como comando, camarero como adaptador de entrada, "necesito tomates" como puerto de salida y proveedor como adaptador de salida. Incluye DDD, SOLID, cuándo sí y cuándo no, técnicas para que la charla no sea aburrida y respuestas a preguntas difíciles.

Pendiente: adaptarla al stack nuevo (RabbitMQ, SOAP, módulos Maven y Lombok) y añadir los nuevos servicios con la misma analogía. Por ejemplo: REST y SOAP son la mesa y el teléfono, dos formas de pedir el mismo plato; Notificaciones es el aviso al repartidor.

## Forma de trabajar

- Una fase cada vez. Al terminar, resume lo hecho y espera la revisión de Peri antes de seguir.
- Antes de cada etiqueta, `./mvnw verify` en verde. Los tests con Testcontainers necesitan Podman en marcha.
- Commits en español, en imperativo y descriptivos. Un commit por paso lógico.
- **No hacer push sin permiso.** Nunca `--force` ni reescritura de historial publicado sin confirmación explícita.
- No inventar versiones de dependencias: si una versión no se resuelve, preguntar.
- Si cambia el código mostrado en la presentación, actualizar `SRC` en el mismo commit.
- Configurar el email del autor de los commits con el de Peri antes del primer commit.

## Entorno de Peri

Windows con PowerShell y VS Code. Red corporativa con Zscaler y VPN GlobalProtect, que a veces provocan errores TLS con Git, Maven y Podman. En Podman, el certificado raíz corporativo debe instalarse dentro de la máquina virtual de Podman, no solo en Windows. Si una descarga, una imagen o un push fallan por certificados, sospecha primero del proxy.
