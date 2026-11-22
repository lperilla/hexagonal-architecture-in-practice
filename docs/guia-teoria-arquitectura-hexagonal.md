# Arquitectura hexagonal explicada fácil

Guía del ponente para la charla **«Arquitectura hexagonal en la práctica: del diseño al código»**, del 4 de diciembre de 2026. Sirve para entender bien cada concepto y para contarlo sin leer definiciones.

Stack del ejemplo: Java 25, Spring Boot 4.1.2, Maven multimódulo, Lombok, MongoDB, RabbitMQ, REST (API first) y SOAP (contract first).

---

## Cómo usar esta guía

Cada concepto sigue el mismo esquema:

- **Qué es**, en una o dos frases.
- **En el restaurante**, una analogía que se repite en toda la guía.
- **En el Petstore**, la clase concreta del repositorio.
- **Cuéntalo en 30 segundos**, un guion para decirlo en voz alta.
- **Confusión típica**, lo que la gente suele entender mal.
- **Pregunta al público**, para que la charla no sea un monólogo.

No hace falta memorizar las definiciones. Si entiendes el restaurante, puedes reconstruir cualquier concepto en el momento.

### Mapa de la charla

La charla tiene los tres bloques del cartel. Esta tabla dice qué secciones de la guía preparan cada uno y qué etiquetas del repositorio se usan en vivo.

| Bloque | Minutos | Secciones de la guía | Etiquetas |
|---|---|---|---|
| Introducción | 2 | La idea en una frase | |
| 1 · Puertos y adaptadores | 29 | 1 a 9, 11 y 12 | `00-start` (rama `mvc`), de `01-domain` a `07-soap-adapter`, `hexagonal/fase-2-notificaciones` |
| 2 · Buenas y malas prácticas | 9 | 13 | `00-start` frente a `09-final` |
| 3 · Ventajas y decisiones de diseño | 8 | 10, 14, 15 y 16 | `08-testing` |
| Cierre | 2 | Chuleta y frase final | |

Para preparar la charla: la sección 17 (cómo hacer que no sea aburrida) y la 18 (preguntas difíciles).

---

## La idea en una frase

> El negocio va en el centro y no sabe nada de tecnología. La tecnología va en el borde y se adapta al negocio, nunca al revés.

La propuso Alistair Cockburn en 2005 con el nombre *Ports and Adapters*: puertos y adaptadores. El hexágono no significa nada especial. Es solo un dibujo con muchos lados para que quepan varias entradas y salidas, sin la idea de «arriba» y «abajo» de las capas clásicas.

---

## El hilo conductor: un restaurante

Usa esta analogía durante toda la charla. Funciona porque todo el mundo ha estado en un restaurante.

Un restaurante recibe pedidos por varios canales: **la mesa y el teléfono**. Son dos formas de pedir el mismo plato, y la cocina no debería cambiar su forma de trabajar según el canal. Tampoco debería cambiar sus recetas cada vez que cambia de proveedor de tomates. Y cuando un pedido sale, alguien **avisa al repartidor**, sin que la cocina sepa cómo trabaja la empresa de reparto.

| En el restaurante | En la arquitectura hexagonal | En el Petstore |
|---|---|---|
| Las recetas y normas de la casa («no se sirve pescado que no sea fresco») | Dominio | `Pet`, con la regla de `adopt()` |
| El jefe de cocina, que organiza cada pedido | Servicio de aplicación | `AdoptPetService` |
| «Preparar un menú del día» | Caso de uso | Adoptar una mascota |
| La comanda, una hoja estándar con el pedido | Comando | `AdoptPetCommand` |
| La ventanilla donde se entregan las comandas | Puerto de entrada | `AdoptPetUseCase` |
| El camarero de la mesa y la persona que atiende el teléfono | Adaptadores de entrada | `AdoptionController` (REST) y `AdoptionEndpoint` (SOAP) |
| «Necesito tomates», «necesito avisar de que ha salido un pedido» | Puertos de salida | `LoadPetPort`, `SavePetPort`, `PublishPetAdoptedPort` |
| El proveedor concreto y el tablón donde se anuncian los pedidos | Adaptadores de salida | `MongoLoadPetAdapter`, `MongoSavePetAdapter`, `RabbitPetAdoptedPublisher` (uno por puerto) |
| El encargado que decide con qué proveedor se trabaja | Composición | `UseCaseConfig`, en el módulo `boot` |
| La carta impresa, que se escribe antes de cocinar | Contrato (API first, contract first) | `adoption-api.yaml`, `adoption.wsdl` y `adoption-types.xsd` |
| El que lee el tablón y llama al repartidor | Otro servicio con su propio adaptador de entrada | `notification-service` y su `PetAdoptedListener` |

La frase clave: **la cocina dice «necesito tomates», no «necesito ir a tal supermercado»**. Si cambias de proveedor, las recetas siguen igual.

---

## 1. Dominio

**Qué es.** Las reglas y conceptos del negocio: qué cosas existen, qué se puede hacer con ellas y qué no está permitido. No usa ningún framework, y debería entenderlo alguien de negocio si se lo lees en voz alta.

**En el restaurante.** Las recetas y las normas de la casa. Son las mismas aunque cambies de horno o de proveedor.

**En el Petstore.** El record `Pet` y su método `adopt()`, en el módulo `domain`:

```java
public record Pet(PetId id, String name, PetStatus status, Owner owner) {

    public Pet {
        // ... validaciones, y la invariante: tiene dueño si y solo si está adoptada
    }

    public Pet adopt(Owner newOwner) {
        if (!isAvailable()) {
            throw new PetNotAvailableException(id);
        }
        return new Pet(id, name, PetStatus.ADOPTED, newOwner);
    }
}
```

También son dominio `PetId` y `Owner` (objetos valor), `PetStatus`, las excepciones `PetNotAvailableException` y `PetNotFoundException`, y el evento `PetAdopted`, que se crea con `PetAdopted.of(mascotaAdoptada, instante)`. Todo el modelo son **records inmutables**, y el módulo no tiene ninguna dependencia: ni Spring ni Lombok.

**Por qué el dominio es inmutable.** `adopt()` no cambia la mascota: devuelve otra, ya adoptada. Así:
- **Nadie se salta la regla.** No hay `setStatus()`: la única forma de obtener una mascota adoptada es `adopt()`.
- **No hay estados a medias.** El constructor valida todo, o no hay objeto. En la versión con setters, si algo falla entre `setStatus()` y `setOwnerName()`, la mascota queda adoptada y sin dueño.
- **Sin efectos colaterales.** Quien tiene una mascota sabe que no va a cambiar por debajo. El código se entiende leyendo una línea.
- **Concurrencia y caché sin miedo.** Se puede compartir entre hilos sin locks. El almacén en memoria la guarda tal cual, sin copias defensivas.
- **Tests más simples.** Se comparan valores: lo que entra y lo que sale.

El precio es que cada cambio crea un objeto nuevo, y hay que usar el que devuelve `adopt()`, no el original. El test `adoptingDoesNotChangeTheOriginalPet` lo deja claro.

**En el restaurante.** Una receta publicada no se tacha a bolígrafo: si cambia, se imprime la versión nueva. Quien tiene la anterior en la mano sabe exactamente lo que dice.

**Cuéntalo en 30 segundos.**
> «El dominio es lo que seguiría siendo verdad aunque mañana tiráramos Spring, MongoDB y RabbitMQ. Que una mascota adoptada no se puede volver a adoptar es una regla del negocio, no de la base de datos. Por eso vive en un módulo Maven sin dependencias, y se prueba en milisegundos. Y es inmutable: un objeto que no puede cambiar no puede quedar en un estado inválido.»

**Confusión típica.** Pensar que el dominio son «las entidades de la base de datos». `PetDocument`, con `@Document` y `@Data`, no es dominio: es cómo se guarda una mascota en MongoDB. `Pet` es qué es una mascota para el negocio.

**Pregunta al público.** «Si cambiamos MongoDB por PostgreSQL, ¿cambia la regla de que no se puede adoptar dos veces?» La respuesta obvia, que no, demuestra que la regla no pertenece a la base de datos.

---

## 2. Infraestructura

**Qué es.** Toda la tecnología concreta que podría cambiar sin que cambie el negocio: el framework web, los servicios SOAP, la base de datos, el broker de mensajes, el servidor de correo y la configuración.

**En el restaurante.** El horno, la cámara frigorífica, el datáfono, el teléfono, la empresa de reparto. Son necesarios, pero no definen qué cocina el restaurante.

**En el Petstore.** Spring MVC, Spring Web Services, MongoDB, RabbitMQ y el SMTP de Mailpit. En hexagonal, todo eso vive en los adaptadores (`infrastructure`, `api-rest` y `api-soap`) y en el arranque (`boot`).

**Cuéntalo en 30 segundos.**
> «La infraestructura es todo aquello de lo que podríamos decir "esto lo cambiamos el año que viene". Es importante, pero es un detalle. La idea es que esos detalles estén en el borde, donde cambiarlos sea barato.»

**Truco para clasificar cualquier clase.** Pregúntate: *si cambio la tecnología, ¿esta clase sigue teniendo sentido?* Si sigue teniéndolo, es dominio o aplicación. Si desaparece, es infraestructura.

**Pregunta al público.** Lee frases y que el público diga dónde van:
- «Una mascota solo se adopta si está disponible.» → Dominio.
- «Se guarda en la colección `pets`.» → Infraestructura.
- «Si no está disponible, devuelve un 409.» → Adaptador REST.
- «Si no está disponible, devuelve el fault `PetNotAvailable`.» → Adaptador SOAP.
- «Si el mensaje falla tres veces, va a la cola de mensajes fallidos.» → Infraestructura.

Este juego funciona muy bien para despertar a la sala.

---

## 3. Caso de uso y servicio de aplicación

**Qué es.**
- Un **caso de uso** es una intención del usuario, dicha en lenguaje de negocio: «adoptar una mascota». Es lo que la aplicación sabe hacer.
- El **servicio de aplicación** es la clase que lo lleva a cabo. Organiza los pasos, pero no toma las decisiones de negocio: se las pregunta al dominio.

**En el restaurante.** El caso de uso es «preparar un menú del día». El jefe de cocina es el servicio de aplicación: pide los ingredientes, sigue la receta, emplata y avisa de que el plato ha salido. No inventa la receta.

**En el Petstore.** `AdoptPetService`, en el módulo `application`:

```java
@Override
public AdoptionResult adopt(AdoptPetCommand command) {
    Pet pet = loadPetPort.loadPet(command.petId())
            .orElseThrow(() -> new PetNotFoundException(command.petId()));

    Pet adopted = pet.adopt(command.owner());   // decide el dominio

    savePetPort.savePet(adopted);
    publishPetAdoptedPort.publish(PetAdopted.of(adopted, clock.instant()));
    return AdoptionResult.from(adopted);
}
```

No lleva `@Service`: solo `@RequiredArgsConstructor` de Lombok. Se registra como bean en `boot`, en `UseCaseConfig`. El `Clock` también se inyecta, para que el test controle el tiempo.

**Cuéntalo en 30 segundos.**
> «El servicio de aplicación es un director de orquesta: carga la mascota, le dice "adóptate", la guarda y avisa al resto del sistema. Fijaos en que no hay ningún if de negocio, y en que no hay ni una clase de Spring. Si le preguntas, te dice: eso decídelo tú, mascota.»

**Cómo distinguir dominio y aplicación.**
- Si la frase suena a *«si pasa esto, no se puede»*, es dominio.
- Si suena a *«primero esto, luego aquello»*, es aplicación.

**Confusión típica.** Meter todas las reglas en el servicio y dejar las entidades como bolsas de getters y setters. Eso se llama *modelo anémico*, y es justo lo que hace el `PetService` de la rama `mvc`: `pet.setStatus(PetDocument.ADOPTED)`. Funciona, pero las reglas acaban repartidas y duplicadas.

---

## 4. Comando, consulta y resultado

**Qué es.**
- Un **comando** es un objeto inmutable con los datos que necesita una acción que cambia algo. Se nombra en imperativo: `AdoptPetCommand(PetId, Owner)`.
- Una **consulta** pide datos sin cambiar nada: `GetPetQuery`.
- Un **resultado** es lo que devuelve el caso de uso: `AdoptionResult`.

Los tres pertenecen al núcleo, no a la API. Son `record` de Java.

**En el restaurante.** La comanda. El cliente puede pedir en la mesa o por teléfono, pero a la cocina siempre le llega la misma hoja con el mismo formato. Al cocinero le da igual cómo se hizo el pedido.

**En el Petstore.** El recorrido de los datos es este:

```text
AdoptionRequest (REST)  o  AdoptPetRequest (SOAP)    → adaptador (DTO o clase JAXB generados)
AdoptPetCommand                                       → núcleo
AdoptionResult                                        → núcleo
AdoptionResponse (REST) o  AdoptPetResponse (SOAP)   → adaptador
```

**Cuéntalo en 30 segundos.**
> «¿Por qué no pasar directamente el request REST al servicio? Porque la adopción ya llega por dos canales, REST y SOAP, y mañana puede llegar por una cola. Si el núcleo recibe un comando, cada canal traduce lo suyo y el núcleo ni se entera de que el canal ha cambiado. Es la comanda del restaurante.»

**Y la validación, ¿dónde va?**
- El formato va en el adaptador: un nombre vacío lo rechaza `@Valid` en REST y la validación contra el XSD en SOAP. Las dos reglas salen del contrato.
- Las reglas de negocio (la mascota debe estar disponible) van en el dominio.

**Confusión típica.** Pensar que el comando es «otro DTO más, duplicado». Aquí se ve que no: hay dos DTO distintos (el JSON y la clase JAXB) y un solo comando.

---

## 5. Puertos

Un **puerto** es una interfaz que define una conversación con el exterior, en el idioma del núcleo. La regla de oro: **el puerto siempre lo define el núcleo**, nunca la tecnología.

**Analogía extra.** El puerto USB-C de un portátil. El portátil decide la forma del puerto, y los fabricantes de cables y adaptadores se ajustan a ella. El portátil no cambia su forma para cada cable.

### Puerto de entrada (driving, primario)

**Qué es.** Lo que la aplicación ofrece al mundo.

**En el restaurante.** La ventanilla donde se dejan las comandas, vengan de la mesa o del teléfono.

**En el Petstore.** En `application.port.in`:

```java
public interface AdoptPetUseCase {
    AdoptionResult adopt(AdoptPetCommand command);
}
```

Lo implementa el servicio de aplicación, y lo llaman los adaptadores de entrada: el controlador REST y el endpoint SOAP.

**El paso intermedio de la migración.** En la etiqueta `02-input-port`, el `PetService` antiguo implementa esta interfaz sin cambiar nada por dentro, y los dos canales ya dependen de ella. Se puede migrar sin parar el servicio.

### Puerto de salida (driven, secundario)

**Qué es.** Lo que la aplicación necesita del mundo.

**En el restaurante.** «Necesito tomates» o «necesito avisar de que ha salido un pedido». No dice qué proveedor ni qué tablón.

**En el Petstore.** En `domain.port`:

```java
public interface LoadPetPort { Optional<Pet> loadPet(PetId petId); }
public interface SavePetPort { void savePet(Pet pet); }
public interface PublishPetAdoptedPort { void publish(PetAdopted event); }
```

Los declara el núcleo y los implementa un adaptador de salida.

**¿Por qué en `domain` y no en `application`?** Es una variante válida y muy común. Lo que importa es que los defina el núcleo y que usen tipos de dominio. Si te lo preguntan, di eso, sin entrar en una discusión de carpetas.

### La tabla que lo aclara todo

| | Puerto de entrada | Puerto de salida |
|---|---|---|
| Qué expresa | Lo que la aplicación **ofrece** | Lo que la aplicación **necesita** |
| Quién lo define | El núcleo (`application.port.in`) | El núcleo (`domain.port`) |
| Quién lo implementa | El servicio de aplicación | Un adaptador de salida |
| Quién lo llama | Un adaptador de entrada | El servicio de aplicación |

**Cuéntalo en 30 segundos.**
> «Un puerto es un contrato que escribe el núcleo. El de entrada dice "esto sé hacer", y el de salida dice "esto necesito". Fijaos en el nombre: `SavePetPort`, no `MongoPetPort`. La cocina pide tomates, no un supermercado concreto.»

**Confusión típica.** Pensar que los puertos de salida los implementa el núcleo. Es justo al revés: el núcleo los declara y la tecnología los implementa. Esta es la fila de la tabla en la que más se equivoca la gente.

**Pregunta al público.** «¿`MongoPetRepositoryPort` es un buen nombre?» No lo es: mete la tecnología dentro del núcleo.

---

## 6. Adaptadores

**Qué es.** Código que traduce entre una tecnología concreta y un puerto. **Solo traduce**: no contiene reglas de negocio. Y **cada adaptador implementa un único puerto**: hace una sola cosa, se prueba por separado y se sustituye por separado. ArchUnit lo comprueba en cada build.

**En el restaurante.**
- Los adaptadores de entrada son el camarero de la mesa y la persona que atiende el teléfono. Cada uno recibe el pedido a su manera y lo convierte en la misma comanda.
- Los adaptadores de salida son el proveedor concreto y el tablón donde se anuncian los pedidos. Cambiar de proveedor no cambia las recetas.

**En el Petstore.**

| Adaptador | Módulo | Qué traduce |
|---|---|---|
| `AdoptionController` | `api-rest` | JSON ↔ comando. Implementa la interfaz `AdoptionApi`, generada desde el OpenAPI |
| `RestExceptionHandler` | `api-rest` | `PetNotFoundException` → 404 y `PetNotAvailableException` → 409, como `ProblemDetail` |
| `AdoptionEndpoint` | `api-soap` | XML (clases JAXB) ↔ comando |
| `AdoptionFaultResolver` | `api-soap` | Excepciones de dominio → faults `PetNotFound` y `PetNotAvailable` |
| `MongoLoadPetAdapter` | `infrastructure` | Implementa `LoadPetPort`: `PetDocument` → `Pet`, con `PetMongoMapper` |
| `MongoSavePetAdapter` | `infrastructure` | Implementa `SavePetPort`: `Pet` → `PetDocument`, con `PetMongoMapper` |
| `RabbitPetAdoptedPublisher` | `infrastructure` | `PetAdopted` → mensaje `pet.adopted` en el exchange `petstore.events` |
| `InMemoryLoadPetAdapter` e `InMemorySavePetAdapter` | `infrastructure` | Los mismos puertos que Mongo, uno cada uno, sobre `InMemoryPetStore` (perfil `in-memory`) |

**Cuéntalo en 30 segundos.**
> «Los adaptadores son traductores. El controlador traduce de HTTP al idioma del negocio, el endpoint SOAP de XML al idioma del negocio, y el adaptador de Mongo del idioma del negocio a documentos. Si un adaptador tiene un if de negocio, ese if está en el sitio equivocado: el camarero no decide si el pescado está fresco.»

**Confusión típica.** Pensar que los mappers son «código duplicado». Son el precio explícito de que el negocio no dependa de la tecnología. Ese precio merece la pena cuando el dominio tiene reglas, y no cuando es un CRUD trivial.

---

## 7. El mismo caso de uso, dos canales: REST y SOAP

**Qué es.** Un caso de uso no pertenece a ningún canal. Si el núcleo está bien separado, añadir un canal es añadir un adaptador.

**En el restaurante.** La mesa y el teléfono. El cliente pide el mismo plato de dos formas, y la cocina recibe la misma comanda. Si un plato no se puede servir, el camarero lo dice a su manera y el del teléfono a la suya, pero la cocina dice lo mismo: «no queda».

**En el Petstore.** Los dos adaptadores son casi idénticos:

```java
var result = adoptPetUseCase.adopt(mapper.toCommand(petId, adoptionRequest));   // REST
var result = adoptPetUseCase.adopt(mapper.toCommand(request));                  // SOAP
```

**El contraste con la rama `mvc`.** Allí el servicio lanza `ResponseStatusException`, que es una excepción HTTP, y el endpoint SOAP tiene que capturarla y adivinar qué significa cada código:

```java
} catch (ResponseStatusException e) {
    if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) { ... PetNotFound ... }
    if (e.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) { ... PetNotAvailable ... }
```

**Cuéntalo en 30 segundos.**
> «En la versión en capas, el negocio habla HTTP, y el canal SOAP tiene que traducir códigos HTTP a faults: es como si la cocina respondiera "error 409" y el del teléfono tuviera que adivinar qué significa. En la hexagonal, la cocina dice "no queda", y cada canal lo traduce a su idioma: un 409 en REST, un fault en SOAP.»

**Pregunta al público.** «¿Qué pasa en la versión en capas si mañana la adopción llega también por una cola?» Habría que hacer la misma traducción de códigos HTTP por tercera vez.

---

## 8. La regla de dependencias

Es la idea más importante de la charla. Si el público se lleva solo una cosa, que sea esta.

> Las dependencias de código apuntan hacia el núcleo. El dominio no importa nada de infraestructura.

### El truco: el flujo y la dependencia van en sentidos contrarios

```text
En ejecución (quién llama a quién):
AdoptPetService ──llama──▶ SavePetPort ──▶ MongoSavePetAdapter
El control va de dentro hacia fuera.

En el código (quién conoce a quién):
AdoptPetService ──▶ SavePetPort ◀──implementa── MongoSavePetAdapter
La dependencia va de fuera hacia dentro.
```

Esto se llama **inversión de dependencias**. En ejecución, el servicio acaba guardando en MongoDB, pero en el código solo conoce la interfaz `SavePetPort`. Por eso puedes cambiar MongoDB sin tocar el servicio.

**En el restaurante.** El jefe de cocina llama al proveedor para pedir tomates; el control va de la cocina hacia fuera. Pero quien se adapta es el proveedor: entrega en el formato y en el horario que fija la cocina.

**Cuéntalo en 30 segundos.**
> «Aquí está el truco de magia. En ejecución, el servicio llama a Mongo. Pero si abrís el código del servicio, no hay ni un import de Mongo: solo una interfaz que define el núcleo. Es Mongo quien se adapta al servicio. Por eso podemos cambiar Mongo por memoria y el servicio ni se entera.»

**Confusión típica.** Creer que la arquitectura hexagonal consiste en tener carpetas llamadas `domain`, `application` y `adapter`. Puedes tener esas carpetas y que el dominio importe clases de Mongo. Lo que cuenta es la dirección de las dependencias. En este repositorio la comprueban **Maven y ArchUnit** (sección 9).

---

## 9. Módulos Maven: la regla, vigilada por el compilador

**Qué es.** Cada pieza de la arquitectura es un módulo Maven, y cada módulo solo declara las dependencias que puede usar. La regla de dependencias deja de ser una convención: si un módulo no declara Spring, importar Spring no compila.

**En el restaurante.** Las zonas del local con sus puertas. La cocina no tiene puerta a la calle: para que entre algo, tiene que pasar por la ventanilla. Maven es la cerradura.

**En el Petstore.**

```text
services/adoption-service/   agregador (packaging pom)
├── api-contract/    OpenAPI → AdoptionApi y DTOs generados
├── soap-contract/   WSDL + XSD → clases JAXB generadas
├── domain/          records inmutables, sin dependencias (ni Spring ni Lombok)
├── application/     → domain
├── infrastructure/  → domain, application (MongoDB, RabbitMQ, memoria)
├── api-rest/        → application, api-contract
├── api-soap/        → application, soap-contract
└── boot/            → todos: arranque, composición y el único JAR ejecutable
```

Algunas cosas que conviene saber:
- Los módulos no llevan prefijo: cada servicio tiene su propio `groupId` (`com.example.petstore.adoption`, `com.example.petstore.notification`).
- `api-rest` y `api-soap` no ven `infrastructure`, ni al revés.
- **ArchUnit**, en `boot`, comprueba lo que Maven no ve: que los puertos no usen tipos técnicos, que el código generado no salga de su adaptador y que los adaptadores no se conozcan.

**El paso a paso de la migración.** De `01-domain` a `05-rabbitmq-adapter`, los módulos nuevos crecen como subcarpetas de `services/adoption-service`, junto al proyecto en capas, que sigue funcionando. En `06-rest-adapter`, el proyecto antiguo desaparece y `services/adoption-service` pasa a ser el agregador de los módulos.

**Cuéntalo en 30 segundos.**
> «Cuando la regla está en una wiki, alguien se la salta un viernes por la tarde. Cuando está en el POM, el proyecto no compila. Si alguien escribe `@Component` en `Pet`, el error es "package org.springframework.stereotype does not exist". Y lo que Maven no ve, lo vigila ArchUnit en cada build.»

**Confusión típica.** Pensar que hacen falta todos los módulos en todos los servicios. `notification-service` solo tiene `domain`, `application`, `infrastructure` y `boot`: no publica ninguna API, así que no necesita `api-*` ni `*-contract`.

---

## 10. Contratos primero: API first y contract first

**Qué es.** El contrato de la API se escribe y se revisa antes que el código, y el código se genera a partir de él. En REST se llama *API first* (OpenAPI); en SOAP, *contract first* (WSDL y XSD).

**En el restaurante.** La carta se imprime antes de abrir la cocina. Los clientes piden por la carta, y la cocina no cambia un plato de la carta sin avisar. Se pueden añadir platos, pero no quitar ni renombrar los que la gente ya pide.

**En el Petstore.**
- `adoption-api.yaml` (OpenAPI 3.0.3) genera la interfaz `AdoptionApi` y los DTOs. El controlador implementa esa interfaz.
- `adoption.wsdl` y `adoption-types.xsd` generan las clases JAXB. El WSDL se publica tal cual en `/ws/adoption.wsdl`.
- `contracts/events/pet-adopted.json` describe el evento: ejemplo, exchange, routing key, consumidores y reglas de compatibilidad.

**Lo que no depende de la arquitectura.** La rama `mvc` también genera desde los contratos. API first no es una idea hexagonal; lo hexagonal es **dónde se queda el código generado**: dentro de su módulo de API (`api-contract` con `api-rest`, `soap-contract` con `api-soap`), sin llegar nunca al dominio ni a los puertos.

**Cuéntalo en 30 segundos.**
> «El contrato es la promesa que hacemos a los clientes, y se revisa antes de escribir código. El código generado es un detalle del adaptador: si un DTO generado aparece en un puerto, el contrato de fuera acaba decidiendo el modelo de dentro.»

**Anécdota útil.** El XSD se llama `adoption-types.xsd` y no `adoption.xsd` porque Spring WS busca el WSDL y el XSD por el nombre sin extensión, y no puede publicar dos ficheros con el mismo nombre. Los contratos también tienen restricciones técnicas, y es mejor descubrirlas antes de programar.

**Sin librería de DTOs compartida.** Cada servicio genera sus propias clases a partir de los contratos. Compartir una librería de DTOs entre servicios obligaría a desplegarlos a la vez cada vez que cambia.

---

## 11. Mensajería: un adaptador de entrada que no es REST

**Qué es.** `notification-service` escucha el evento `pet.adopted` y envía un email al refugio: «Ana ha adoptado a Luna». Su canal de entrada no es HTTP, sino una cola de RabbitMQ.

**En el restaurante.** El aviso al repartidor. Cuando sale un pedido, la cocina lo anuncia en el tablón. Otra persona, que no trabaja en la cocina, lee el tablón y llama al repartidor. Si oye dos veces «mesa 7 lista», no llama dos veces. Y si un aviso es ilegible, lo deja en la bandeja de incidencias en lugar de tirarlo.

**En el Petstore.**

| Pieza | Clase | Restaurante |
|---|---|---|
| Adaptador de entrada | `PetAdoptedListener` (`@RabbitListener`) traduce el mensaje a `NotifyAdoptionCommand` | Quien lee el tablón |
| Caso de uso | `NotifyAdoptionUseCase` y `NotifyAdoptionService` | La decisión de avisar, una sola vez |
| Puerto de salida | `SendNotificationPort` | «Necesito avisar» |
| Adaptadores de salida | `SmtpNotificationSender` (Mailpit) y `LogNotificationSender` (perfil `log-notifications`) | El teléfono o una nota en el cuaderno |
| Idempotencia | `NotifiedAdoptionsPort`, con un registro en memoria | «Ya llamé por la mesa 7» |
| Cola de mensajes fallidos | Tres reintentos y después `notification.pet-adopted.dlq` | La bandeja de incidencias |

**Cuéntalo en 30 segundos.**
> «Un listener de RabbitMQ es un controlador cuyo cliente es una cola: recibe un mensaje, lo traduce a un comando y llama al caso de uso. Que un mensaje llegue dos veces es normal, así que el caso de uso no repite el email. Y los reintentos y la cola de fallidos se configuran en la infraestructura: el caso de uso no sabe que existen.»

**Consumidores tolerantes.** El servicio no comparte clases con `adoption-service`:
- deserializa el mensaje a su propia clase;
- ignora los campos que no conoce, como el futuro `adopterId`;
- no depende de la cabecera `__TypeId__`, que el productor ni siquiera envía.

Así el productor puede evolucionar sin romper a nadie.

**Pregunta al público.** «¿Qué pasa si el mismo mensaje llega dos veces?» Y después: «¿Y si el envío del email falla?». La adopción se marca como notificada solo después de enviar, así que el reintento lo vuelve a intentar.

---

## 12. Hexagonal frente a capas clásicas

```text
Capas clásicas (rama mvc)            Hexagonal (rama hexagonal)

Controller / Endpoint                REST ──▶            ◀── MongoDB
    ▼                                SOAP ──▶   NÚCLEO   ◀── RabbitMQ
Service (negocio)
    ▼
Repository (datos)

El negocio depende de los datos.     Todo depende del núcleo.
```

**Cuéntalo en 30 segundos.**
> «En capas, el negocio está encima de la base de datos y depende de ella. En hexagonal, la base de datos es un enchufe más, como la API REST o el servicio SOAP. El código del principio de la charla, el `PetService` de la rama `mvc`, era una arquitectura en capas.»

Si te preguntan por *Clean Architecture* (Robert C. Martin) u *Onion Architecture* (Jeffrey Palermo): son variantes con la misma regla de fondo, que las dependencias apuntan hacia el negocio. Cambian los nombres y el número de anillos.

---

## 13. Buenas y malas prácticas: lo que enseña la comparación

El bloque 2 compara el mismo caso en las dos ramas. No hace falta contarlo todo: elige dos o tres puntos según el público.

| Mala práctica (rama `mvc`) | Buena práctica (rama `hexagonal`) | En el restaurante |
|---|---|---|
| La regla compara un `String` del documento de MongoDB | `Pet.adopt()` protege la regla | La receta está en el recetario, no apuntada en la caja del proveedor |
| El documento con `@Data` hace de modelo de negocio | `PetDocument` vive solo en el adaptador | El albarán del proveedor no es la receta |
| El negocio lanza excepciones HTTP | Excepciones de dominio, traducidas en cada canal | La cocina dice «no queda», no «error 409» |
| El servicio usa `RabbitTemplate` | `PublishPetAdoptedPort` | La cocina anuncia «pedido listo», no llama a una empresa concreta |
| `@Service` y escaneo | Servicio sin Spring, compuesto en `boot` | El encargado decide los proveedores, no el cocinero |

**Cómo demostrar que no se ha roto nada.** Los tests de extremo a extremo (`AdoptionEndToEndTest` y `NotificationEndToEndTest`) no dependen de ninguna clase del servicio: solo de HTTP, MongoDB, RabbitMQ, Mailpit y los contratos. Son el mismo fichero en las dos ramas y pasan en todas las etiquetas. Las respuestas REST y los faults SOAP son idénticos byte a byte.

**Para compararlo uno mismo:**

```bash
git diff mvc hexagonal --stat -- services/adoption-service
```

---

## 14. DDD: qué tiene que ver

**La aclaración que hay que hacer siempre:** hexagonal y DDD no son lo mismo.

| | Arquitectura hexagonal | DDD (Domain-Driven Design) |
|---|---|---|
| Qué decide | Dónde está la frontera y hacia dónde apuntan las dependencias | Cómo modelar el negocio dentro de esa frontera |
| Pregunta que responde | ¿Qué puede cambiar sin tocar el negocio? | ¿Qué conceptos y reglas tiene el negocio? |
| Origen | Alistair Cockburn, 2005 | Eric Evans, 2003 |

Son independientes y se complementan. En el restaurante, la hexagonal es la distribución del local: la cocina separada del comedor y de los proveedores. DDD es cómo se escriben las recetas.

### DDD estratégico, solo dos ideas

- **Lenguaje ubicuo.** El código usa las palabras del negocio. Escribimos `pet.adopt(owner)` y no `pet.setStatus("ADOPTED")`. Si negocio dice «adoptar», el código dice «adoptar».
- **Contexto delimitado (bounded context).** Una palabra puede significar cosas distintas en distintas partes del negocio. Se ve ya en el repositorio: para Adopción, una adopción es una mascota con dueño; para Notificaciones, `Adoption` es un aviso pendiente con su clave de idempotencia. Son clases distintas en servicios distintos, y está bien que lo sean.

### DDD táctico en el Petstore

| Pieza | Qué es | En el restaurante | Petstore |
|---|---|---|---|
| Entidad | Tiene identidad que se mantiene aunque cambien sus datos | Una mesa concreta: la 7 sigue siendo la 7 aunque cambien los comensales | `Pet` |
| Objeto valor | Se define por sus atributos, es inmutable y no tiene identidad propia | Un billete de 10 €: da igual cuál sea | `PetId`, `Owner`, `EmailAddress` |
| Agregado | Grupo que se modifica como una unidad, siempre a través de su raíz | Una comanda con sus platos | `Pet` es la raíz |
| Evento de dominio | Algo relevante que ya ha ocurrido, dicho en pasado | «Mesa 7 servida» | `PetAdopted` |
| Repositorio | Colección de agregados. En hexagonal es un puerto de salida | La despensa | `LoadPetPort`, `SavePetPort` |
| Servicio de dominio | Regla que no pertenece de forma natural a ninguna entidad | Decidir qué mesa se asigna a un grupo | No lo necesitamos, así que no lo forzamos |

**Cuéntalo en 30 segundos.**
> «La hexagonal construye la caja fuerte; DDD decide qué guardamos dentro y cómo lo ordenamos. Podéis hacer una sin la otra, pero juntas funcionan muy bien. El punto donde se tocan es el repositorio: en DDD es una colección de mascotas, y en hexagonal es un puerto de salida.»

---

## 15. SOLID: ya lo habéis visto sin daros cuenta

No expliques SOLID en abstracto. Señala dónde aparece cada principio en el código que ya se ha visto en la charla.

| Principio | Qué dice | En el restaurante | En el Petstore |
|---|---|---|---|
| **S**: responsabilidad única | Una clase tiene un solo motivo para cambiar | El camarero no cocina y el cocinero no cobra | `AdoptionController` cambia si cambia la API, `MongoSavePetAdapter` si cambia cómo se guarda y `Pet` si cambia la regla. Por eso cada adaptador implementa un único puerto |
| **O**: abierto/cerrado | Se puede extender sin modificar lo que ya funciona | Añadir el teléfono sin reescribir las recetas | El canal SOAP y los adaptadores en memoria se añaden sin tocar `AdoptPetService` |
| **L**: sustitución de Liskov | Cualquier implementación de un contrato debe poder sustituir a otra sin romper nada | Cualquier proveedor de tomates debe traer tomates, no pimientos | `PetPersistenceContract`: el adaptador de Mongo y el de memoria pasan los mismos tests |
| **I**: segregación de interfaces | Interfaces pequeñas, pensadas para quien las usa | Al repartidor le das la dirección, no la carta entera | `LoadPetPort` y `SavePetPort` están separados |
| **D**: inversión de dependencias | Lo importante no depende de los detalles; ambos dependen de abstracciones | La cocina fija cómo quiere recibir el género y el proveedor se adapta | El núcleo define `SavePetPort` y Mongo lo implementa |

**La frase para cerrar este bloque:**
> «La D de SOLID es la arquitectura hexagonal aplicada a una clase. La arquitectura hexagonal es la D aplicada a toda la aplicación.»

---

## 16. Cuándo sí y cuándo no

Decir esto da credibilidad. Una charla que vende una arquitectura como solución universal genera desconfianza.

**Merece la pena cuando:**
- Hay reglas de negocio de verdad que conviene proteger y probar.
- Hay varios canales de entrada (REST, SOAP, mensajería, batch) o se esperan más.
- La tecnología puede cambiar: migraciones, nuevos proveedores, nueva nube.
- El sistema va a vivir años y lo van a tocar varios equipos.

**Probablemente sobra cuando:**
- Es un CRUD sin reglas: guardar y leer formularios. El registro de mascotas del material ampliado (fase 3) se queda en capas a propósito, también en la rama `hexagonal`.
- Es un prototipo o un script de vida corta.
- El equipo no lo conoce y el plazo es muy ajustado.

**Ventajas y costes, sin adornos.**

| Ventaja | Coste |
|---|---|
| El negocio se prueba en milisegundos, sin infraestructura | Más ficheros: puertos, mappers y modelos técnicos |
| Cambiar MongoDB o añadir un canal toca solo un adaptador | Mapeos explícitos entre dominio, DTOs y documentos |
| Las reglas de dependencia las vigila el compilador | Más módulos Maven y más configuración de construcción |
| Migración gradual: cada paso compila y pasa los tests | Disciplina: un atajo en un adaptador rompe el modelo |

**Cuéntalo en 30 segundos.**
> «Si vuestro servicio solo guarda y lee formularios, no os hace falta todo esto: tendréis el triple de archivos para nada. La arquitectura tiene que reducir el coste del cambio, no aumentar el número de archivos. Lo que justifica el Petstore no son las altas y bajas de mascotas, sino la regla de adopción, los dos canales y el evento.»

---

## 17. Cómo hacer que la charla no sea aburrida

1. **Empieza por el dolor, no por la definición.** Enseña primero el `PetService` de la rama `mvc` y el endpoint SOAP que captura una excepción HTTP. La teoría llega como respuesta a un problema que la sala ya ha visto.
2. **Usa una sola analogía y repítela.** El restaurante aparece en cada concepto. Cambiar de analogía en cada diapositiva confunde.
3. **Haz predicciones.** Antes de cada etiqueta, pregunta qué creen que va a tener que cambiar. Acertar o fallar hace que la gente preste atención.
4. **Juega a «¿dónde va esto?».** Lee cinco frases y que la sala diga si son dominio, aplicación o adaptador. Dura dos minutos y despierta a todo el mundo.
5. **Guarda dos momentos de magia.**
   - Quitar MongoDB en directo con el perfil `in-memory`, sin tocar el negocio.
   - Adoptar a Luna por REST y que aparezca el email en Mailpit.

   Anuncia el primero al principio: «dentro de media hora vamos a quitar la base de datos sin tocar el negocio».
6. **Haz el código en vivo por etiquetas.** Cada paso es un `git checkout` de la etiqueta siguiente: si algo falla en directo, saltas a la etiqueta y sigues. Las notas del ponente dicen la etiqueta de partida de cada pantalla.
7. **No leas las diapositivas.** Las tablas son para la sala. Tú cuenta la versión de 30 segundos.
8. **Admite los costes.** Decir en voz alta «esto tiene más archivos y más mappers» hace que te crean cuando dices que merece la pena.
9. **Cierra con la pregunta del principio.** Recuperar «¿qué cambia si sustituimos MongoDB?» da sensación de historia cerrada.

---

## 18. Preguntas difíciles que te pueden hacer

**«¿No es demasiado código para tan poca cosa?»**
Para este ejemplo, casi. Por eso existe el punto de «cuándo no». El coste se paga una vez; el beneficio aparece cada vez que cambia la tecnología o entra un canal nuevo, como ha pasado aquí con SOAP y con la mensajería.

**«¿Por qué no uso directamente el `MongoRepository` de Spring Data como puerto?»**
Porque entonces el núcleo conoce MongoDB: sus tipos, sus métodos y su paginación. El puerto propio dice solo lo que el negocio necesita, y Spring Data se usa dentro del adaptador (`SpringDataPetRepository` ni siquiera es público).

**«¿Puedo poner `@Document` o `@Entity` en la clase de dominio y ahorrarme el mapper?»**
Puedes, y es una decisión pragmática válida en sistemas pequeños. El precio es que el modelo de negocio queda condicionado por la base de datos: constructores vacíos, setters y tipos. Si el dominio es rico, compensa separarlos.

**«¿Por qué records y no clases con Lombok en el dominio?»**
Porque un record es inmutable por diseño: no hay setters que olvidar quitar, y la igualdad, `toString` y los accesores vienen de serie, sin ninguna librería. El dominio queda sin dependencias. Lombok se sigue usando donde no hace daño, en los adaptadores (`@RequiredArgsConstructor`, `@Data` en `PetDocument`) y en el servicio de aplicación.

**«¿Un record no es solo para DTOs? ¿Y la identidad de la entidad?»**
Un record es una clase inmutable con todos sus campos en el constructor; nada impide que tenga comportamiento, como `adopt()`. La igualdad de un record compara todos los campos: dos «fotos» de Luna, disponible y adoptada, no son iguales, y está bien, porque son estados distintos. La identidad de la entidad es su `PetId`: para saber si son la misma mascota, se comparan los identificadores.

**«¿Por qué un adaptador por puerto? ¿No es más sencillo un `MongoPetAdapter` que cargue y guarde?»**
Es más corto, pero esa clase tendría dos motivos para cambiar. Con un adaptador por puerto, cada clase hace una sola cosa, se prueba por separado y se puede sustituir por separado; por ejemplo, guardar en otra base de datos sin tocar la carga. ArchUnit lo comprueba en cada build.

**«¿Por qué los puertos de salida están en `domain` y no en `application`?»**
Es una variante válida. Lo que define la arquitectura es que el núcleo define los puertos con sus propios tipos; si están en un paquete o en otro es una decisión de organización.

**«¿Un módulo Maven por capa no es exagerado? ¿No bastan los paquetes?»**
Los paquetes son una convención; los módulos son una regla que comprueba el compilador. En un servicio pequeño, los paquetes y ArchUnit pueden bastar. Aquí se usan módulos para que la estructura se vea y para que romper la regla no compile.

**«¿Dónde va `@Transactional`?»**
En el servicio de aplicación, porque la transacción envuelve el caso de uso completo. Si quieres un núcleo sin Spring, como en este repositorio, puedes aplicarla desde la configuración envolviendo el servicio. Muchos equipos optan por la vía pragmática y anotan el servicio.

**«¿Hace falta la interfaz del puerto de entrada si solo hay una implementación?»**
No es imprescindible. La interfaz aporta un contrato claro, permitió el paso intermedio de la migración y facilita sustituir el caso de uso en el test del controlador (`@MockitoBean`). Es una decisión de equipo, no un dogma.

**«¿Un puerto por cada método?»**
No. Los puertos se diseñan por capacidad. `LoadPetPort` y `SavePetPort` podrían ser un único `PetRepository`; aquí se separan para que cada uso pida solo lo que necesita.

**«¿Por qué SOAP en 2026?»**
Porque sigue existiendo en muchas empresas, sobre todo en sistemas heredados. Y porque es el mejor ejemplo de que un canal es un detalle: el mismo caso de uso sirve REST y SOAP sin cambiar una línea del núcleo.

**«¿Por qué no compartir una librería de DTOs entre servicios?»**
Porque acopla los despliegues: un cambio en la librería obliga a actualizar y desplegar a todos a la vez. Cada servicio genera sus clases desde los contratos y los consumidores ignoran los campos que no conocen.

**«¿Hexagonal significa microservicios?»**
No. Es una forma de organizar el interior de una aplicación, sea un monolito o un microservicio. De hecho, facilita partir un monolito más adelante.

**«¿Y si MongoDB guarda pero RabbitMQ falla?»**
Es un problema real de consistencia que la hexagonal no resuelve por sí sola. En la demo se publica justo después de guardar, sin más. La solución habitual es el patrón *Transactional Outbox*: guardar el evento junto a la mascota y publicarlo después desde un proceso aparte.

**«¿Y si el mismo mensaje llega dos veces?»**
Lo normal con mensajería es «al menos una vez». Por eso el consumidor es idempotente: `notification-service` recuerda las adopciones notificadas (por mascota e instante) y no repite el email.

---

## Chuleta de una página

| Concepto | En una frase | Restaurante | Petstore |
|---|---|---|---|
| Dominio | Las reglas del negocio, sin tecnología | Recetas y normas de la casa | `Pet.adopt()` |
| Infraestructura | La tecnología que podría cambiar | Horno, teléfono, reparto | Spring, MongoDB, RabbitMQ, SMTP |
| Caso de uso | Una intención en lenguaje de negocio | Preparar un menú del día | Adoptar una mascota |
| Servicio de aplicación | Organiza los pasos del caso de uso | Jefe de cocina | `AdoptPetService` |
| Comando | Datos de una acción que cambia algo | La comanda | `AdoptPetCommand` |
| Puerto de entrada | Lo que la aplicación ofrece | Ventanilla de comandas | `AdoptPetUseCase` |
| Puerto de salida | Lo que la aplicación necesita | «Necesito tomates» | `SavePetPort` |
| Adaptador de entrada | Traduce un canal a un caso de uso | La mesa y el teléfono | `AdoptionController`, `AdoptionEndpoint`, `PetAdoptedListener` |
| Adaptador de salida | Implementa un único puerto con tecnología | Proveedor concreto | `MongoSavePetAdapter`, `RabbitPetAdoptedPublisher` |
| Regla de dependencias | Todo depende del núcleo, nunca al revés | El proveedor se adapta a la cocina | Ningún import de Mongo en el núcleo |
| Módulos Maven | La regla, vigilada por el compilador | Las puertas del local | `domain` no compila con Spring |
| Contratos primero | El contrato antes que el código | La carta impresa | OpenAPI, WSDL, XSD y el evento |
| Mensajería | Un canal de entrada más | El aviso al repartidor | `notification-service` |
| DDD | Cómo modelar el negocio por dentro | Cómo se escriben las recetas | Entidad, objeto valor, evento |
| SOLID (D) | Lo importante no depende de los detalles | La cocina fija el formato del pedido | El núcleo define los puertos |

**La frase final de la charla:**
> Hexagonal no consiste en dibujar un hexágono. Consiste en conservar las decisiones de negocio cuando cambian las decisiones tecnológicas.
