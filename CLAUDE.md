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
| Tests | JUnit 6, Mockito, AssertJ y ArchUnit. **Sin Testcontainers** (decisión de Peri): los tests de integración y de extremo a extremo usan la infraestructura de `compose.yaml` |

### Versiones verificadas (fase 0, en Maven Central)

| Pieza | Versión | Dónde se fija |
|---|---|---|
| Spring Boot | 4.1.2 (existe 4.2.0, pero se mantiene 4.1.2) | `parent` del POM raíz |
| Spring Framework / Data / AMQP / WS | 7.0.10 / 2026.0.2 / 4.1.2 / 5.0.3 | Gestionadas por Boot |
| Lombok | 1.18.48 (soporta Java 25) | Gestionada por Boot |
| JUnit Jupiter | **6.0.3** | Gestionada por Boot |
| AssertJ / Mockito | 3.27.7 / 5.23.0 | Gestionadas por Boot |
| Jackson | 3.1.7 (`tools.jackson.*`; las anotaciones siguen en `com.fasterxml.jackson.annotation`) | Gestionada por Boot |
| `openapi-generator-maven-plugin` | 7.27.0 | Propiedad del POM raíz |
| Plugin JAXB | `org.codehaus.mojo:jaxb2-maven-plugin` 4.3.0 (usa `jaxb-xjc` 4.0.9, igual que el runtime de Boot) | Propiedad del POM raíz |
| ArchUnit | 1.5.1, artefacto `archunit` **sin el motor de JUnit 5** (las reglas se comprueban desde `@Test` de JUnit 6) | Propiedad del POM raíz |
| Maven | 3.9.16 con Maven Wrapper 3.3.4 | `.mvn/wrapper/maven-wrapper.properties` |

Si una versión no se resuelve o hay dudas de compatibilidad, **preguntar a Peri en lugar de inventar otra**.

### Spring Boot 4.1.2: lo que cambia y ya está comprobado

- **Starters:** `spring-boot-starter-webmvc` (no `-web`), `-validation`, `-data-mongodb`, `-amqp`, `-webservices` (no `-web-services`), `-mail`, `-jackson`, `-actuator`. Para tests hay starters por tecnología: `spring-boot-starter-webmvc-test` y `spring-boot-starter-data-mongodb-test`, además de `spring-boot-starter-test`.
- **Paquetes de test:** `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest`, `org.springframework.boot.test.web.server.LocalServerPort` y `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- **Propiedades:** MongoDB pasa a `spring.mongodb.uri` y `spring.mongodb.database`. Siguen igual `spring.rabbitmq.*`, `spring.mail.*`, `spring.mvc.problemdetails.enabled`, `spring.webservices.path` y `spring.webservices.wsdl-locations`.
- **Autoconfiguración de MongoDB** (para excluirla en el perfil `in-memory`): `org.springframework.boot.mongodb.autoconfigure.Mongo*AutoConfiguration` y `org.springframework.boot.data.mongodb.autoconfigure.DataMongo*AutoConfiguration`.
- **Jackson 3 en Spring AMQP:** `JacksonJsonMessageConverter(JsonMapper)`. En `JsonNode`, `asText()` pasa a `asString()` y `fieldNames()` a `propertyNames()`.
- **Java 25 y Lombok:** desde Java 23 el procesamiento de anotaciones no se activa solo. Lombok está declarado en `annotationProcessorPaths` de `maven-compiler-plugin`, en el `pluginManagement` del POM raíz. Lombok 1.18.48 añade `@lombok.Generated` al código generado. Esa anotación queda en el bytecode, así que ArchUnit debe permitir el paquete `lombok..`.
- **Generador OpenAPI:**
  - opciones `useSpringBoot4`, `useJackson3`, `interfaceOnly`, `skipDefaultInterface`, `useTags`, `useBeanValidation`, `useResponseEntity` y `hideGenerationTimestamp`;
  - `openApiNullable=false`, `documentationProvider=none`, `annotationLibrary=none`, `generateJsonIncludeAnnotations=false` y `generateJsonSetterNullsAnnotations=false`.

  Están en el `pluginManagement` del POM raíz. El código generado usa `org.springframework.lang.Nullable`, que está deprecado; solo produce avisos.

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

- Hereda de `spring-boot-starter-parent` 4.1.2: todos los servicios comparten la gestión de versiones de Spring Boot, incluidas Lombok, JUnit y AssertJ.
- Declara `<modules>` con cada servicio y fija propiedades comunes: `java.version` 25 y las versiones de lo que Spring Boot no gestiona (ArchUnit, generador OpenAPI, plugin JAXB).
- En `pluginManagement`: `maven-compiler-plugin` con Lombok como procesador de anotaciones, y la configuración base de los generadores.
- En `plugins`: `maven-enforcer-plugin`, que exige Java 25 y Maven 3.9 con un mensaje claro.
- **Sin dependencias en el POM raíz**: cada módulo declara solo lo que usa.
- **Todos los módulos heredan directamente del POM raíz** (`relativePath` relativo) y declaran su `groupId`. El agregador de cada servicio hexagonal solo agrupa módulos; no es su padre.
- Comandos:
  - `./mvnw verify` construye todo.
  - Para un solo servicio: en `mvc`, `./mvnw -pl services/<servicio> -am verify`; en `hexagonal`, `./mvnw -pl services/<servicio>/boot -am verify`.
- **`.mvn/settings.xml`** es un `settings.xml` vacío que descarga de Maven Central. El `settings.xml` de Peri redirige todo al Nexus corporativo, que no es accesible sin VPN, así que hay que pasar `-s .mvn/settings.xml`. El sistema de permisos impidió crear `.mvn/maven.config` para aplicarlo siempre: Peri puede crearlo a mano.

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

Infraestructura en `compose.yaml`: MongoDB 27017, RabbitMQ 5672 (consola de gestión en 15672), Mailpit 1025 (SMTP) y 8025 (web). Todas las bases de datos viven en la misma instancia de MongoDB, pero cada servicio solo accede a la suya. Detalles:
- Los puertos se publican solo en `127.0.0.1`, porque en Podman sobre WSL las conexiones a `::1` se quedan colgadas.
- RabbitMQ usa el usuario `petstore` con contraseña `petstore`, porque `guest` no puede conectarse desde fuera del contenedor.
- El healthcheck de RabbitMQ es una comprobación TCP. `rabbitmq-diagnostics` se ejecutaría como root y crearía una cookie de Erlang que el servidor no puede leer.
- Los servicios todavía no están en `compose.yaml`: se arrancan con `java -jar`.

**Mensajería:** un exchange de tipo topic, `petstore.events`. Cada consumidor tiene su propia cola enlazada con la routing key del evento (`notification.pet-adopted`, `pet-registry.pet-adopted`) y su cola de mensajes fallidos (`<cola>.dlq`). Los mensajes van en JSON, y cada consumidor los deserializa a su propia clase sin depender de los nombres de clase del productor. Cómo está implementado:
- **Productor:** `JacksonJsonMessageConverter` con un `ClassMapper` vacío, para que **no envíe la cabecera `__TypeId__`**.
- **Consumidor:**
  - `setAlwaysConvertToInferredType(true)`: el tipo lo decide el parámetro del listener;
  - `@JsonIgnoreProperties(ignoreUnknown = true)` en su clase de mensaje;
  - `default-requeue-rejected: false` y 3 reintentos: después, el mensaje va a la DLQ. Un JSON mal formado va a la DLQ sin reintentos.

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

Cómo se hizo la migración (así está en las etiquetas):
- **De 01 a 05**, los módulos nuevos (`domain`, `application` e `infrastructure`) crecen **como subcarpetas de `services/adoption-service`, junto al `src/` del proyecto en capas**, y el POM raíz los declara uno a uno. Así cada paso solo añade un módulo y la aplicación antigua sigue funcionando.
- **En 02**, `PetService` implementa `AdoptPetUseCase`, y el controlador y el endpoint SOAP ya dependen de la interfaz.
- **En 03 y 04**, `AdoptPetService` y los adaptadores existen y tienen tests, pero la aplicación antigua no los usa. Además, no depende de `infrastructure`, así que no hay beans duplicados.
- **En 06**, `services/adoption-service/pom.xml` pasa a ser el agregador (`packaging pom`) y el código acoplado se borra. El endpoint SOAP antiguo vive en `boot` de forma provisional hasta 07, ya usando el caso de uso.
- **`AdoptionEndToEndTest`** no usa ninguna clase del servicio: solo HTTP, la colección `pets`, el exchange y los contratos. Pasa sin cambios en todas las etiquetas; en 06 se movió a `boot` como renombrado al 100 %.

Al final de cada fase posterior se crean las etiquetas `mvc/fase-N-<nombre>` y `hexagonal/fase-N-<nombre>`. Existen `mvc/fase-2-notificaciones` y `hexagonal/fase-2-notificaciones`.

Todas las etiquetas son anotadas, y cada una debe compilar, arrancar y pasar `./mvnw verify`. Las 10 etiquetas de la migración se verificaron desde un checkout limpio, todas en verde. El README tiene el bucle para repetirlo en bash y en PowerShell.

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
        ├── exception/      Excepciones, incluidas las que llevan estado HTTP (no se usa en adopción)
        └── config/         Configuración y datos de demo
```

En `adoption-service` no hay carpeta `exception/`: los errores REST salen como `ProblemDetail` con `spring.mvc.problemdetails.enabled=true`, sin manejador propio. Las clases generadas van a `com.example.petstore.adoption.api.contract` (OpenAPI) y a `com.example.petstore.adoption.soap.contract` (JAXB). Son los mismos paquetes que en `hexagonal`.

`notification-service` en `mvc` tiene `messaging/` (listener, `PetAdoptedMessage` y configuración de colas), `service/NotificationService` y `config/NotificationProperties`.

En `adoption-service`, el acoplamiento debe ser visible porque es el material del bloque de malas prácticas:

- La regla de negocio está en el servicio y compara un `String` del documento de MongoDB.
- El documento de MongoDB, con `@Data`, hace de modelo de negocio.
- El servicio lanza `ResponseStatusException`, así que **el endpoint SOAP tiene que capturar una excepción HTTP para convertirla en un fault SOAP**: es el ejemplo más claro de por qué el negocio no debe conocer el canal.
- El servicio depende directamente de `RabbitTemplate`.

En `notification-service`, el acoplamiento equivalente:
- `NotificationService` recibe el mensaje de RabbitMQ;
- construye el texto del email;
- usa `JavaMailSender` directamente;
- guarda las adopciones notificadas en un campo `Set`;
- comprueba el perfil activo con `Environment.matchesProfiles("log-notifications")`.

Tests:
- unitarios del servicio con Mockito;
- `AdoptionEndToEndTest` y `NotificationEndToEndTest`, de extremo a extremo y contra `compose.yaml`. Son idénticos en las dos ramas y no dependen de clases del servicio.

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
| `domain` | `domain.model`, `domain.exception`, `domain.event`, `domain.port` | `Pet`, `PetId`, `Owner` (records inmutables), `PetStatus`; `PetNotAvailableException`, `PetNotFoundException`; `PetAdopted`; `LoadPetPort`, `SavePetPort`, `PublishPetAdoptedPort` |
| `application` | `application.port.in`, `application.service` | `AdoptPetUseCase`, `AdoptPetCommand`, `AdoptionResult`; `AdoptPetService` |
| `infrastructure` | `infrastructure.mongodb`, `infrastructure.rabbitmq`, `infrastructure.inmemory`, `infrastructure.client` | `MongoLoadPetAdapter`, `MongoSavePetAdapter`, `PetDocument`, `PetMongoMapper`, `SpringDataPetRepository`; `RabbitPetAdoptedPublisher`, `PetAdoptedMessage`, configuración de exchange; `InMemoryLoadPetAdapter`, `InMemorySavePetAdapter`, `InMemoryPetStore` |
| `api-contract` | Generados desde `openapi/adoption-api.yaml` | Interfaz de la API y DTOs de petición y respuesta |
| `api-rest` | `api.rest` | Controlador que implementa la interfaz generada, mapper DTO a comando, manejador de errores (`ProblemDetail`) |
| `soap-contract` | Generados desde `adoption-types.xsd` | Clases JAXB de petición, respuesta y faults |
| `api-soap` | `api.soap` | Endpoint, mapper y traducción de excepciones de dominio a faults SOAP |
| `boot` | Paquete raíz del servicio y `boot.config` | `AdoptionServiceApplication`, registro de `AdoptPetService` como bean, `application.yml`, perfil `in-memory`, datos de demo, ArchUnit y tests de arranque |

**`AdoptionServiceApplication` está en el paquete raíz del servicio** (`com.example.petstore.adoption`), no en `...adoption.boot`. Así el escaneo de componentes alcanza `api.rest`, `api.soap` e `infrastructure`, y el test de extremo a extremo encuentra la configuración sin cambios. La configuración va en `...boot.config`: `UseCaseConfig`, con el `@Bean` del caso de uso y un `Clock`, y `DemoDataLoader`, que carga los datos a través de los puertos. El JAR ejecutable se llama `adoption-service-<versión>.jar` gracias a `finalName`.

Nombres reales en el código (la presentación debe usar estos):

| Pieza | Nombre |
|---|---|
| Puertos de salida | `LoadPetPort.loadPet(PetId)`, `SavePetPort.savePet(Pet)` y `PublishPetAdoptedPort.publish(PetAdopted)` |
| Regla de adopción | `Pet` es un record inmutable: `Pet.adopt(Owner)` devuelve una mascota nueva, ya adoptada. Fábrica `Pet.available(...)`; el constructor canónico valida la invariante. El evento se crea con `PetAdopted.of(mascotaAdoptada, instante)` |
| Comando | `AdoptPetCommand(PetId, Owner)`, con objetos valor |
| Adaptador REST | `AdoptionController implements AdoptionApi` (generada), con `AdoptionRestMapper` y `RestExceptionHandler extends ResponseEntityExceptionHandler` |
| Adaptador SOAP | `AdoptionEndpoint` con `AdoptionSoapMapper`. `AdoptionFaultResolver` traduce las excepciones de dominio a faults; el endpoint no captura excepciones |
| Publicación del evento | `RabbitPetAdoptedPublisher`, con `PetAdoptedMessage` y `RabbitConfig` |
| Persistencia en memoria | `InMemoryLoadPetAdapter` e `InMemorySavePetAdapter` comparten `InMemoryPetStore`, que guarda las mascotas sin copias porque son inmutables. El contrato común es `PetPersistenceContract`, en los tests de `infrastructure` |
| ArchUnit | `ArchitectureTest`, con 9 reglas, entre ellas «cada adaptador implementa un único puerto» |

`notification-service` (hexagonal):

| Módulo | Contenido |
|---|---|
| `domain` | `Adoption`, `AdoptionKey`, `EmailAddress` y `Notification.adoptionNotice(...)`, que fija el texto del aviso. Puertos `SendNotificationPort` y `NotifiedAdoptionsPort` |
| `application` | `NotifyAdoptionUseCase.notifyAdoption(NotifyAdoptionCommand)`, que devuelve `NotificationResult`, y `NotifyAdoptionService` |
| `infrastructure` | `rabbitmq.listener.PetAdoptedListener` y `PetAdoptedMessage`, `rabbitmq.RabbitConfig`, `mail.SmtpNotificationSender`, `logging.LogNotificationSender` (perfil `log-notifications`) e `inmemory.InMemoryNotifiedAdoptions` |
| `boot` | `NotificationServiceApplication`, `boot.config.UseCaseConfig` y `NotificationProperties` (`notification.shelter-email` y `notification.from-email`) |

Los **puertos de salida viven en `domain`** (`domain.port`), y los de entrada en `application` (`application.port.in`). En la charla hay que explicar que es una variante válida: lo que importa es que el núcleo define los puertos y la tecnología los implementa.

Los adaptadores de entrada por mensajería (listeners de RabbitMQ) también viven en `infrastructure`, en un subpaquete propio (`infrastructure.rabbitmq.listener`). El módulo `infrastructure` agrupa toda la tecnología de integración, de entrada y de salida, excepto las APIs que el servicio publica (`api-rest` y `api-soap`).

### Dependencias entre módulos (las impone Maven)

```text
domain          ← sin dependencias (ni Spring ni Lombok)
application     → domain
infrastructure  → domain, application (y Spring Data MongoDB, Spring AMQP, clientes)
api-contract    → solo lo necesario para el código generado
soap-contract   → solo Jakarta XML Binding
api-rest        → application, api-contract, Spring Web MVC
api-soap        → application, soap-contract, Spring Web Services
boot            → todos los anteriores; único módulo con spring-boot-maven-plugin
```

- `api-rest` y `api-soap` **no dependen de `infrastructure`**, ni al revés.
- Si alguien importa Spring o MongoDB en `domain` o en `application`, **el proyecto no compila**. Está comprobado: `package org.springframework.stereotype does not exist`. ArchUnit, en `boot`, comprueba además las reglas que Maven no puede ver (por ejemplo, que `domain.port` no use tipos de `infrastructure`). Usa el artefacto `archunit`, sin su motor, con `@Test` de JUnit 6, y en `application` permite `lombok..` por la anotación `@lombok.Generated`. También comprueba que cada adaptador de `infrastructure` implemente un único puerto.
- Solo `boot` produce el JAR ejecutable.

### Uso de Lombok

- **Dominio: sin Lombok.** El modelo de dominio son **records inmutables** (`Pet` incluido): los cambios devuelven un objeto nuevo, como `pet.adopt(owner)`. Así nadie se salta una regla con un setter, no hay objetos a medio cambiar, se comparten sin copias defensivas y los tests comparan valores. Las excepciones de dominio son clases normales sin Lombok. Comandos, resultados y eventos también son `record`.
- **Adaptadores:** `@RequiredArgsConstructor` para inyección por constructor y `@Data` en documentos de MongoDB si se quiere.
- **Rama `mvc`:** `@Data` en el documento usado como modelo de negocio. Es parte del material de malas prácticas.

### Reglas de arquitectura (no romperlas)

- `domain` no depende de `application`, de la infraestructura ni de Spring.
- `application` no depende de la infraestructura ni de Spring. Los servicios de aplicación no llevan anotaciones de Spring: se registran como beans en `boot`.
- Los puertos se nombran por capacidad, nunca por tecnología: `SavePetPort`, no `MongoPetRepositoryPort`.
- Los puertos usan tipos de dominio, nunca DTOs generados, clases JAXB, documentos de MongoDB ni mensajes de RabbitMQ.
- Los adaptadores solo traducen y no contienen reglas de negocio. Mappers, modelos técnicos, timeouts, reintentos y fallbacks viven en el adaptador.
- **Un adaptador, un puerto:** cada adaptador implementa una única interfaz de puerto y hace una sola cosa (por ejemplo, `MongoLoadPetAdapter` y `MongoSavePetAdapter`). Si dos adaptadores necesitan compartir algo, como el almacén en memoria, se extrae a una clase técnica que no es un puerto (`InMemoryPetStore`). ArchUnit lo comprueba.
- **El modelo de dominio es inmutable:** records, sin setters; los cambios de estado devuelven un objeto nuevo.
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

Detalles de los contratos de adopción, aprobados en la fase 1:
- **OpenAPI:** versión 3.0.3. `petId` es un `string`. `ownerName` no puede estar vacío ni tener solo espacios (patrón `.*\S.*`) y tiene como máximo 100 caracteres. `status` es el enumerado `PetStatus` (`AVAILABLE`, `ADOPTED`). Los errores usan el esquema `Problem` (RFC 9457) con `application/problem+json`; se llama así para no chocar con `ProblemDetail` de Spring. Los mensajes van en español: «La mascota 99 no existe» y «La mascota 1 no está disponible para adopción».
- **SOAP, ficheros:** `adoption.wsdl` (escrito a mano, WSDL 1.1, document/literal, SOAP 1.1) importa **`adoption-types.xsd`**. El XSD no se llama `adoption.xsd` porque Spring WS busca el WSDL y el XSD por el nombre sin extensión, y no puede publicar dos ficheros que se llamen igual.
- **SOAP, publicación:** `spring.webservices.wsdl-locations=classpath:/wsdl` publica `/ws/adoption.wsdl` y `/ws/adoption-types.xsd`.
- **SOAP, faults y validación:** los elementos de detalle son `PetNotFoundFault` y `PetNotAvailableFault`, con `petId` y `message`. Las peticiones se validan contra el XSD, y un nombre vacío devuelve un fault `Client` «Validation error».
- **Evento:** el contrato está en `contracts/events/pet-adopted.json`, con el ejemplo, el exchange, la routing key, los consumidores y las reglas de compatibilidad.
- **MongoDB:** la colección `pets` tiene los campos `_id`, `name`, `status` y `ownerName`, y debe conservar esa forma en las dos ramas.

## Fases

Cada fase termina con las dos ramas en verde, el README actualizado y una revisión de Peri antes de empezar la siguiente.

**Antes de la charla del 4 de diciembre hay que cerrar las fases 0, 1 y 2.** Las fases 3 y 4 son material ampliado, y solo se abordan si queda tiempo. La fase 5 son ideas, no se implementa.

| Fase | Estado |
|---|---|
| 0. Entorno y base | ✅ Hecha y revisada |
| 1. `adoption-service` | ✅ Hecha y revisada (de `00-start` a `09-final`) |
| 2. `notification-service` | ✅ Hecha y revisada (`mvc/fase-2-notificaciones` y `hexagonal/fase-2-notificaciones`) |
| Presentación | ✅ Adaptada al stack nuevo, en tres bloques y 50 minutos |
| Guía de teoría | ✅ Adaptada al stack nuevo y a los tres bloques |
| 3 y 4 | Material ampliado, sin empezar |

### Fase 0: entorno y base

- Java 25, Maven y Podman comprobados, con los problemas y arreglos que recoge la sección «Entorno de Peri».
- Versiones resueltas y fijadas en el POM raíz: la tabla está en «Versiones verificadas».
- POM raíz, Maven Wrapper, `compose.yaml` (MongoDB, RabbitMQ y Mailpit) y README base creados.
- El repositorio anterior, `petstore-hexagonal`, no está disponible en la máquina. El código se escribió a partir de este documento y de los contratos.

### Fase 1: `adoption-service` en las dos ramas

- Rama `mvc`: el servicio en capas, con REST (API first) y SOAP (contract first), MongoDB y RabbitMQ.
- Rama `hexagonal`: la migración de `00-start` a `09-final`, un commit y una etiqueta por paso.
- `main` apunta al final de `hexagonal`.

### Fase 2: `notification-service` (prioritaria para la charla)

Cuando se completa una adopción, envía un email al refugio con el asunto **"Ana ha adoptado a Luna"**. Se usa esta frase y no «Luna ha sido adoptada por Ana» porque no tiene género: funciona también con Thor. La dirección del refugio se configura en `application.yml` (`notification.shelter-email`). Los emails se ven en Mailpit (http://localhost:8025).

Decisiones tomadas al implementarla:
- **Idempotencia:** la clave es `petId` + `adoptedAt`, porque el evento no tiene identificador propio. La adopción se marca como notificada **después** de enviar el email, así que si el envío falla, el reintento lo vuelve a intentar.
- **Reintentos:** 3, con 500 ms entre ellos. Después, el mensaje va a `notification.pet-adopted.dlq`.
- **Puerto 8083:** con `webmvc` y `actuator`, el servicio solo publica `/actuator/health`.
- **`NotificationEndToEndTest`:**
  - publica en RabbitMQ y comprueba el email en la API de Mailpit;
  - usa nombres de mascota únicos y no borra los emails que haya en Mailpit;
  - no debe ejecutarse con `notification-service` arrancado, porque los dos consumirían de la misma cola.

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

Teclas: flechas o mando para navegar, S para ver la solución, N para las notas del ponente, P para el modo presentación, T para el cronómetro y C para cambiar entre modo claro y oscuro.

**Adaptada al stack nuevo** (Java 25, Spring Boot 4.1.2, RabbitMQ, SOAP, módulos Maven y Lombok), con los 12 cambios que estaban pendientes aplicados:
- **Identidad visual del cartel**, aprobada por Peri: verde petróleo `#0E1A1E`, núcleo naranja `#F47A20`, adaptadores turquesa `#2EC4B6` y texto crema `#F3EEE3`. **Oscuro por defecto**; el modo claro usa los mismos tonos y se recuerda en el navegador.
- **Tres bloques visibles** en el menú lateral, en la cabecera y en el pie de cada pantalla. El objeto `BLOCKS` y el campo `block` de cada módulo los definen.
- **Código en vivo marcado:** las pantallas con `live` muestran «Código en vivo · <etiqueta>», y sus notas indican la rama y la etiqueta de partida.
- **`SRC` es el código real del repositorio:** extractos, con lo omitido indicado con `// ...`. Incluye `mvc` (00-start), las etiquetas 01 a 08 y `notification-service`.
- **Comprobada sin errores en las 48 pantallas.** A 1920×1080 en modo presentación no desborda ninguna, ni en ancho ni en alto. A 1280×760, algunas piden scroll vertical. Las líneas de código largas se parten dentro del panel.

### Estructura: 50 minutos en tres bloques

| Bloque | Módulo | Min | En vivo |
|---|---|---|---|
| Introducción | 0. Portada (título del cartel, «Código en vivo», agenda de bloques) y pregunta para el final | 2 | |
| 1 · Puertos y adaptadores | 1. Funciona, pero está acoplada (rama `mvc`, incluido el endpoint SOAP que captura una excepción HTTP) | 5 | Opcional, `00-start` |
| | 2. Las piezas de la arquitectura hexagonal, con los módulos Maven sobre el hexágono | 6 | |
| | 3. Construir el núcleo: dominio, puerto de entrada, servicio y puertos de salida | 7 | `01-domain` a `03-output-ports` |
| | 4. Conectar los adaptadores: MongoDB, RabbitMQ, REST y SOAP | 7 | `04-mongodb-adapter` a `07-soap-adapter` |
| | 5. Un adaptador de entrada que no es REST: `notification-service` y la demo de Mailpit | 4 | `hexagonal/fase-2-notificaciones` |
| 2 · Buenas y malas prácticas | 6. `mvc` frente a `hexagonal`, lado a lado | 3 | |
| | 7. Errores frecuentes, buenas prácticas y ArchUnit | 6 | |
| 3 · Ventajas y decisiones de diseño | 8. Ventajas: sustituir infraestructura, testing por capas, ventajas y costes | 4 | `08-testing` |
| | 9. Decisiones: API first y contract first, decisiones de diseño, cuándo sí y cuándo no, DDD y SOLID (consulta) | 4 | |
| Cierre | 10. Cinco reglas, la pregunta del principio y para seguir | 2 | |

Las pantallas marcadas como **recortables** en las notas dicen cuánto tiempo ahorran. DDD y SOLID quedan como material de consulta.

Para comprobarla después de cambiarla, hay dos pasos. Primero, extraer el `<script>` y pasarlo por `node --check`. Después, ejecutarlo con un DOM mínimo y generar el `body()` de todas las pantallas, buscando `undefined`, `NaN` o `[object Object]`. Para verla, se puede servir `docs/` con `jwebserver` (con `-J-Djdk.net.unixdomain.tmpdir=...`).

### Guion de ensayo (`docs/guion-ensayo.md` y `docs/ensayo.ps1`)

El guion recorre la charla pantalla a pantalla: el minuto previsto, qué enseñar y los comandos exactos del código en vivo y de las demos. También incluye un plan B para cada fallo probable. `docs/ensayo.ps1` define las funciones de PowerShell que usa el guion (`Initialize-Charla`, `Start-Infraestructura`, `Use-Etiqueta`, `Test-Modulo`, `Start-Adopcion`, `Invoke-Adopcion`, etc.). Está guardado en UTF-8 **con BOM**, para que Windows PowerShell 5.1 lea bien los acentos.

El código en vivo se hace en una copia de trabajo aparte, `hexagonal-live`, junto al repositorio (`git worktree`), que salta de etiqueta en etiqueta. El repositorio principal se queda en `main`, porque al hacer checkout de una etiqueta antigua desaparece `docs/`.

**Si cambian las etiquetas, los nombres de los JAR o las rutas de los módulos, revisar `ensayo.ps1` y el guion.**

El cartel de difusión debe guardarse en `docs/banner-charla.png`.

## Guía de teoría (`docs/guia-teoria-arquitectura-hexagonal.md`)

Guía del ponente. Usa la analogía del restaurante: recetas como dominio, jefe de cocina como servicio de aplicación, comanda como comando, camarero como adaptador de entrada, "necesito tomates" como puerto de salida y proveedor como adaptador de salida. Incluye DDD, SOLID, cuándo sí y cuándo no, técnicas para que la charla no sea aburrida y respuestas a preguntas difíciles.

**Adaptada al stack nuevo:** RabbitMQ, SOAP, módulos Maven, Lombok y los nombres reales del código. Ampliaciones respecto a la versión anterior:
- La analogía incluye los nuevos canales: REST y SOAP son la mesa y el teléfono, y `notification-service` es el aviso al repartidor (la idempotencia es «ya llamé por la mesa 7», y la DLQ es la bandeja de incidencias).
- Un mapa de los tres bloques con las secciones y las etiquetas de cada uno.
- Secciones nuevas:
  - el mismo caso de uso por REST y SOAP;
  - módulos Maven;
  - contratos primero;
  - mensajería;
  - buenas y malas prácticas.
- Preguntas difíciles nuevas:
  - records en el dominio y por qué es inmutable;
  - un adaptador por puerto;
  - puertos de salida en `domain`;
  - módulos frente a paquetes;
  - por qué SOAP;
  - DTOs compartidos;
  - mensajes duplicados.

**Si cambia el código, revisar también los fragmentos y los nombres de clase de la guía.**

## Forma de trabajar

- Una fase cada vez. Al terminar, resume lo hecho y espera la revisión de Peri antes de seguir.
- Antes de cada etiqueta, `./mvnw verify` en verde y la aplicación arrancando contra `compose.yaml`. Los tests de integración y de extremo a extremo **necesitan `podman compose up -d`**. Cada uno usa su propia base de datos (`petstore-adoption-test`, `petstore-adoption-infra-test`) o su propia cola, y no toca los datos de la demo.
- Los tests de extremo a extremo de cada servicio son **el mismo fichero en las dos ramas** y no dependen de clases del servicio. Son la prueba de que el comportamiento externo no cambia.
- Las fases se implementan primero en `mvc` y después en `hexagonal`; el test de extremo a extremo se copia con `git show mvc:<ruta>`. Las dos ramas no se fusionan: los cambios comunes, como este documento, se llevan con `git cherry-pick`.
- Commits en español, en imperativo y descriptivos. Un commit por paso lógico.
- **No hacer push sin permiso.** Nunca `--force` ni reescritura de historial publicado sin confirmación explícita.
- No inventar versiones de dependencias: si una versión no se resuelve, preguntar.
- Si cambia el código mostrado en la presentación, actualizar `SRC` en el mismo commit.
- Configurar el email del autor de los commits con el de Peri antes del primer commit.

## Entorno de Peri

Windows con PowerShell y VS Code. Red corporativa con Zscaler y VPN GlobalProtect, que a veces provocan errores TLS con Git, Maven y Podman. En Podman, el certificado raíz corporativo debe instalarse dentro de la máquina virtual de Podman, no solo en Windows. Si una descarga, una imagen o un push fallan por certificados, sospecha primero del proxy.

Problemas encontrados en las fases 0 a 2 y cómo se resolvieron:

| Problema | Causa | Arreglo |
|---|---|---|
| `java -version` da 21 | `JAVA_HOME` apunta a `C:\dev-tools\java\jdk-21.0.9`. El JDK 25 está en `C:\dev-tools\java\jdk-25.0.4.1` | Usar el JDK 25 en los comandos (`$env:JAVA_HOME=...`). **Pendiente** que Peri cambie su `JAVA_HOME`. El enforcer avisa si se usa Java 21 |
| Maven no descarga sin VPN | `~/.m2/settings.xml` redirige todo al Nexus corporativo | `-s .mvn/settings.xml` en cada `mvnw`. **Pendiente** que Peri cree `.mvn/maven.config` |
| `Unable to establish loopback connection` al arrancar Tomcat o cualquier `Selector` de Java | El equipo bloquea los sockets AF_UNIX en `%TEMP%` (`AppData\Local\Temp`) | `-Djdk.net.unixdomain.tmpdir=C:\lperilla\tmp` en `JAVA_TOOL_OPTIONS`. Se usa solo en la sesión; **pendiente** que Peri lo fije en su variable de usuario |
| `win-sshproxy.exe failed to start` y sin `\\.\pipe\docker_engine` | Por la misma causa, queda un socket huérfano en `%TEMP%\podman` que no se puede borrar (Error 1920) y que reaparece cada vez que se para la máquina | Parar la máquina, **renombrar** `%TEMP%\podman` y volver a arrancarla |
| `netavark: nftables error` y ningún contenedor arranca | El kernel de WSL 2.6.1 no incluye `nft_fib` | `wsl --update`, que dejó WSL 3.0.5 con el kernel 6.18 |
| Contenedores en marcha, pero inaccesibles desde `localhost` en Windows | Regresión de Podman 6.0.x dentro de la máquina, [podman#29377](https://github.com/containers/podman/issues/29377) | Podman 6.2.0 dentro de la máquina (`sudo dnf upgrade podman netavark`) y `/usr/share/containers/containers.conf.d/999-podman-machine-wsl.conf` con `[engine] force_port_listen = true` |
| `localhost` se queda colgado y `127.0.0.1` funciona | En WSL, `::1` se acepta pero no responde | Puertos publicados en `127.0.0.1` en `compose.yaml` |
| Aparece un fichero `NUL` en el repositorio | `podman machine ssh` lanzado desde la carpeta del proyecto escribe ahí su `known_hosts`. `curl.exe -o NUL` en PowerShell hace lo mismo | Lanzar `podman machine ssh` desde otra carpeta y borrar `NUL` si aparece |
| Tildes rotas en la salida de `Invoke-RestMethod` | Windows PowerShell 5.1 decodifica mal el UTF-8 | Solo es la visualización: los datos son correctos |

Herramientas de la máquina: Git 2.54, Podman 6.1.4 (cliente) con la máquina WSL rootful `podman-machine-default`, y `podman compose`, que usa `docker-compose` 2.39 como proveedor.
