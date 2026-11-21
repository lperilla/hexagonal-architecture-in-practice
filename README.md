# Arquitectura hexagonal en la práctica: del diseño al código

Código y material de la charla técnica **"Arquitectura hexagonal en la práctica: del diseño al código"** (4 de diciembre de 2026).

El caso central es **adoptar una mascota**: un servicio que se migra paso a paso desde un proyecto MVC en capas hasta una arquitectura hexagonal multimódulo, sin cambiar su comportamiento. Alrededor de él se construye un pequeño ecosistema de microservicios de una tienda de mascotas.

La charla se organiza en tres bloques:

1. Puertos y adaptadores
2. Buenas y malas prácticas
3. Ventajas y decisiones de diseño

## Stack

Java 25 · Spring Boot 4.1.2 · Maven multimódulo · Lombok · MongoDB · RabbitMQ · REST (API first, OpenAPI) · SOAP (contract first, Spring Web Services) · Mailpit · JUnit 6, Mockito, AssertJ y ArchUnit · Podman.

## Requisitos

| Herramienta | Versión | Comprobación |
|---|---|---|
| JDK | 25 | `java -version` (y `JAVA_HOME` apuntando a ese JDK) |
| Podman | 6.x, con `podman compose` | `podman --version` y `podman compose version` |
| Git | cualquiera reciente | `git --version` |

No hace falta instalar Maven: el repositorio incluye el Maven Wrapper (`./mvnw` en bash y `.\mvnw.cmd` en PowerShell), que descarga Maven 3.9.16.

La construcción falla con un mensaje claro si no se usa Java 25.

### Podman en Windows

- La máquina de Podman tiene que estar en marcha: `podman machine start`.
- **WSL 2.7.5 o superior.** Podman 6 usa nftables para la red de los contenedores, y el kernel de versiones anteriores de WSL no incluye los módulos necesarios (`nft_fib`). Con un WSL antiguo, los contenedores no arrancan y aparece `netavark (exit code 1): nftables error`. Comprueba la versión con `wsl --version` y actualiza con `wsl --update`.
- **Puertos accesibles desde Windows.** Con Podman 6, los contenedores pueden arrancar bien y aun así ser inaccesibles desde `localhost` en Windows ([podman#29377](https://github.com/containers/podman/issues/29377)). Las máquinas creadas con una imagen reciente ya traen el arreglo. En una máquina más antigua hay que hacer dos cosas:
  1. Que Podman dentro de la máquina sea la versión 6.1.1 o superior. Compruébalo con `podman version`, en el campo `Server`, y actualízalo con `podman machine ssh sudo dnf -y upgrade podman netavark`.
  2. Crear dentro de la máquina el fichero `/usr/share/containers/containers.conf.d/999-podman-machine-wsl.conf` con este contenido:

     ```toml
     [engine]
     force_port_listen = true
     ```

  Después, reinicia la máquina. `compose.yaml` publica los puertos solo en `127.0.0.1`, porque en WSL las conexiones a `::1` se quedan colgadas.
- Si al arrancar la máquina aparece `win-sshproxy.exe failed to start`, suele deberse a un socket huérfano en `%TEMP%\podman`: para la máquina, renombra esa carpeta y vuelve a arrancarla.
- **`Unable to establish loopback connection` al arrancar un servicio.** Java crea un socket AF_UNIX interno en `%TEMP%`, y algunos equipos corporativos lo bloquean. Indica otra carpeta, que debe existir, añadiendo `-Djdk.net.unixdomain.tmpdir=C:\ruta\a\otra\carpeta` a la variable de entorno `JAVA_TOOL_OPTIONS`.
- En redes corporativas con proxy TLS (por ejemplo, Zscaler), el certificado raíz corporativo debe instalarse **dentro** de la máquina de Podman, no solo en Windows.

### Maven y mirrors corporativos

`.mvn/settings.xml` es un `settings.xml` vacío: descarga todo de Maven Central. Si tu `~/.m2/settings.xml` redirige las descargas a un mirror corporativo que no está accesible, usa el del proyecto:

```bash
./mvnw -s .mvn/settings.xml verify
```

## Infraestructura

`compose.yaml` levanta MongoDB, RabbitMQ y Mailpit:

```bash
podman compose up -d      # arrancar
podman compose ps         # estado
podman compose down       # parar (añade -v para borrar los datos de MongoDB)
```

| Servicio | Puerto | Acceso |
|---|---|---|
| MongoDB | 27017 | `mongodb://localhost:27017` |
| RabbitMQ (AMQP) | 5672 | usuario `petstore`, contraseña `petstore` |
| RabbitMQ (consola) | 15672 | http://localhost:15672 |
| Mailpit (SMTP) | 1025 | |
| Mailpit (web) | 8025 | http://localhost:8025 |

## Construcción

```bash
./mvnw verify                                        # todo el monorepo
./mvnw -pl services/adoption-service/boot -am verify # un solo servicio (boot y todo lo que necesita)
```

En PowerShell, sustituye `./mvnw` por `.\mvnw.cmd`.

Los tests de extremo a extremo (`AdoptionEndToEndTest` y `NotificationEndToEndTest`) usan la infraestructura de `compose.yaml`: levántala antes con `podman compose up -d`.
- El de adopción usa su propia base de datos, `petstore-adoption-test`, y no toca los datos de la demo.
- El de notificaciones publica eventos en RabbitMQ y comprueba los emails en la API de Mailpit. Cada test usa una mascota con un nombre único, así que no borra los emails que haya. No lo ejecutes con `notification-service` arrancado: los dos consumirían de la misma cola.

## Servicios

| Servicio | Puerto | Qué hace |
|---|---|---|
| `adoption-service` | 8080 | Adopción por REST y SOAP. Publica `pet.adopted` |
| `notification-service` | 8083 | Escucha `pet.adopted` y envía un email al refugio («Ana ha adoptado a Luna»). Solo publica `/actuator/health` |

`notification-service`:
- **Cola:** `notification.pet-adopted`, enlazada al exchange `petstore.events` con la routing key `pet.adopted`.
- **Reintentos:** un mensaje que no se puede procesar se reintenta 3 veces y después va a `notification.pet-adopted.dlq`.
- **Idempotencia:** el mismo evento recibido dos veces solo genera un email. La clave es `petId` + `adoptedAt`, y el registro está en memoria.
- **Perfil `log-notifications`:** escribe la notificación en el log en lugar de enviarla.
- **Configuración:** la dirección del refugio está en `notification.shelter-email`, en `application.yml`.

En la rama `hexagonal`, el listener de RabbitMQ es un **adaptador de entrada** igual que un controlador REST: traduce el mensaje a `NotifyAdoptionCommand` y llama a `NotifyAdoptionUseCase`. El envío por SMTP y por log son dos adaptadores del mismo puerto, `SendNotificationPort`. El registro de adopciones notificadas está detrás de `NotifiedAdoptionsPort`.

## Demo

Arranca la infraestructura y los servicios:

```bash
podman compose up -d
./mvnw -DskipTests package
java -jar services/adoption-service/boot/target/adoption-service-0.0.1-SNAPSHOT.jar
java -jar services/notification-service/boot/target/notification-service-0.0.1-SNAPSHOT.jar
```

Al arrancar, el servicio de adopción carga tres mascotas disponibles si no existen: 1 Luna, 2 Thor y 3 Kira.

| Canal | Dónde | Casos |
|---|---|---|
| REST | `requests.http` (REST Client de VS Code o IntelliJ) | 200, 409, 404 y 400 en `POST /pets/{petId}/adoption` |
| SOAP | `soap-requests/` | Adopción, `PetNotAvailable`, `PetNotFound` y nombre vacío. WSDL en http://localhost:8080/ws/adoption.wsdl |
| Evento | Consola de RabbitMQ (http://localhost:15672) | Cada adopción publica `pet.adopted` en el exchange `petstore.events` (contrato en `contracts/events/`) |
| Email | Mailpit (http://localhost:8025) | Cada adopción genera un email al refugio: «Ana ha adoptado a Luna» |

Para avisar solo por log, sin Mailpit:

```bash
java -jar services/notification-service/boot/target/notification-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=log-notifications
```

Para volver al estado inicial, borra la base de datos y reinicia el servicio:

```bash
podman exec petstore-mongodb mongosh --quiet petstore-adoption --eval "db.dropDatabase()"
```

### Perfil `in-memory`: sustituir la infraestructura

Con el perfil `in-memory`, la persistencia pasa a `InMemoryLoadPetAdapter` e `InMemorySavePetAdapter`, que implementan los mismos puertos que los adaptadores de MongoDB, uno cada uno. Se excluye la autoconfiguración de MongoDB, así que el servicio arranca sin base de datos. El dominio y los casos de uso no cambian. RabbitMQ sigue siendo necesario para publicar el evento.

```bash
java -jar services/adoption-service/boot/target/adoption-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=in-memory
```

## Ramas

| Rama | Contenido |
|---|---|
| `mvc` | Todos los servicios en arquitectura MVC por capas técnicas, con un único módulo Maven por servicio |
| `hexagonal` | Nace de `mvc` (etiqueta `00-start`): la migración paso a paso de `adoption-service` a una arquitectura hexagonal multimódulo y, después, el resto de fases |
| `main` | Apunta al final de `hexagonal`. Es la rama por defecto |

Las dos ramas no se fusionan nunca. Tienen el mismo comportamiento externo (mismos contratos OpenAPI, WSDL y eventos, y mismas respuestas), así que `compose.yaml`, `requests.http` y los sobres SOAP funcionan igual en ambas. Los tests de extremo a extremo son la prueba: `AdoptionEndToEndTest` pasa sin cambios en todas las etiquetas, y `NotificationEndToEndTest` es el mismo fichero en las dos ramas.

Cada fase posterior a la migración se implementa en las dos ramas por separado y termina con una etiqueta en cada una:

| Fase | `mvc` | `hexagonal` |
|---|---|---|
| 2. `notification-service` | `mvc/fase-2-notificaciones` | `hexagonal/fase-2-notificaciones` |

## Etiquetas de la migración

Cada paso crea o completa un módulo Maven. Todas las etiquetas compilan, arrancan y pasan `./mvnw verify`.

| Etiqueta | Qué cambia | Módulos |
|---|---|---|
| `00-start` | Punto de partida: el servicio en capas, con REST y SOAP. Funciona, pero el negocio conoce HTTP, MongoDB y RabbitMQ | Un único módulo |
| `01-domain` | Modelo de dominio con la regla de adopción, en Java puro | `domain` |
| `02-input-port` | Caso de uso, comando y resultado. El servicio antiguo implementa el puerto como paso intermedio | `application` |
| `03-output-ports` | Puertos de salida en el dominio y servicio de aplicación, probado sin Spring | `domain`, `application` |
| `04-mongodb-adapter` | Adaptador de persistencia | `infrastructure` |
| `05-rabbitmq-adapter` | Adaptador de publicación del evento | `infrastructure` |
| `06-rest-adapter` | Contrato OpenAPI, adaptador REST y arranque. Se borra el código acoplado y el endpoint SOAP antiguo pasa a usar el caso de uso | `api-contract`, `api-rest`, `boot` |
| `07-soap-adapter` | Contrato SOAP y adaptador SOAP: el mismo caso de uso, un segundo canal de entrada | `soap-contract`, `api-soap` |
| `08-testing` | Adaptador en memoria, contrato común de persistencia y ArchUnit | `infrastructure`, `boot` |
| `09-final` | Documentación y material de la charla | |

Durante los pasos 01 a 05, los módulos nuevos crecen junto al proyecto en capas, dentro de `services/adoption-service`, y el POM raíz los declara uno a uno. En el paso 06, `services/adoption-service` pasa a ser el agregador de sus módulos.

Para ver un paso concreto:

```bash
git diff 05-rabbitmq-adapter 06-rest-adapter --stat
git checkout 03-output-ports
```

## Comparar `mvc` y `hexagonal`

```bash
git diff mvc hexagonal --stat -- services/adoption-service
git diff mvc hexagonal -- services/adoption-service
```

El mismo caso de uso, `POST /pets/{petId}/adoption` y `AdoptPet`, en las dos arquitecturas:

| Pieza | `mvc` | `hexagonal` |
|---|---|---|
| Regla de negocio | `PetService` compara un `String` del documento de MongoDB y lo cambia con setters | `Pet.adopt()` en `domain`: `Pet` es un record inmutable y adoptar devuelve una mascota nueva |
| Errores | `ResponseStatusException`; el endpoint SOAP traduce códigos HTTP a faults | `PetNotFoundException` y `PetNotAvailableException`, traducidas en `api-rest` y en `api-soap` |
| Persistencia | El documento con `@Data` es el modelo de negocio | `LoadPetPort` y `SavePetPort`, cada uno con su adaptador: `MongoLoadPetAdapter` y `MongoSavePetAdapter`, o sus equivalentes en memoria |
| Evento | El servicio usa `RabbitTemplate` directamente | `PublishPetAdoptedPort`, implementado por `RabbitPetAdoptedPublisher` |
| Composición | `@Service` y escaneo de componentes | `AdoptPetService` sin anotaciones, registrado en `boot` |

## Verificar todas las etiquetas

Con la infraestructura levantada (`podman compose up -d`) y el árbol de trabajo limpio.

Bash:

```bash
for tag in 00-start 01-domain 02-input-port 03-output-ports 04-mongodb-adapter \
           05-rabbitmq-adapter 06-rest-adapter 07-soap-adapter 08-testing 09-final; do
  git checkout -q "$tag" && ./mvnw -q clean verify > /dev/null 2>&1 \
    && echo "$tag: OK" || { echo "$tag: FALLA"; break; }
done
git checkout -q hexagonal
```

PowerShell:

```powershell
$tags = '00-start','01-domain','02-input-port','03-output-ports','04-mongodb-adapter',
        '05-rabbitmq-adapter','06-rest-adapter','07-soap-adapter','08-testing','09-final'
foreach ($tag in $tags) {
    git checkout -q $tag
    .\mvnw.cmd -q clean verify *> $null
    if ($LASTEXITCODE -ne 0) { "${tag}: FALLA"; break }
    "${tag}: OK"
}
git checkout -q hexagonal
```

Si tu `~/.m2/settings.xml` usa un mirror corporativo, añade `-s .mvn/settings.xml` a cada `mvnw`.

## Organización

```text
hexagonal-architecture-in-practice/
├── pom.xml          POM agregador: Spring Boot 4.1.2, Java 25 y versiones de plugins
├── mvnw, mvnw.cmd   Maven Wrapper
├── .mvn/            Configuración del wrapper y settings.xml del proyecto
├── compose.yaml     Infraestructura (Podman)
├── requests.http    Peticiones REST de la demo
├── soap-requests/   Sobres SOAP de la demo
├── contracts/       Contratos de los eventos de RabbitMQ
├── services/        Un servicio por carpeta
│   ├── adoption-service/
│   └── notification-service/
└── docs/            Presentación, guía de teoría y cartel
```

### Módulos de `notification-service`

Solo los módulos que necesita: no publica ninguna API, así que no tiene `api-*` ni `*-contract`.

```text
services/notification-service/   Agregador (packaging pom)
├── domain/          Adoption, AdoptionKey, EmailAddress, Notification y los puertos SendNotificationPort y NotifiedAdoptionsPort. Sin Spring
├── application/     NotifyAdoptionUseCase, NotifyAdoptionCommand, NotificationResult y NotifyAdoptionService. Sin Spring
├── infrastructure/  rabbitmq.listener (adaptador de entrada), mail (SMTP), logging (perfil log-notifications) e inmemory
└── boot/            Arranque, composición y propiedades (dirección del refugio)
```

### Módulos de `adoption-service`

```text
services/adoption-service/       Agregador (packaging pom)
├── api-contract/    Contrato OpenAPI y código generado (interfaz de la API y DTOs)
├── soap-contract/   XSD y WSDL, y clases JAXB generadas
├── domain/          Modelo inmutable (records), reglas, excepciones, eventos y puertos de salida. Sin dependencias
├── application/     Casos de uso: puerto de entrada, comando, resultado y AdoptPetService. Sin Spring
├── infrastructure/  Adaptadores MongoDB, RabbitMQ y en memoria: uno por puerto
├── api-rest/        Adaptador REST: controlador, mapper y errores ProblemDetail
├── api-soap/        Adaptador SOAP: endpoint, mapper y faults
└── boot/            Arranque y composición. Único módulo con el JAR ejecutable
```

Maven impone las dependencias entre módulos: si alguien importa Spring en `domain` o en `application`, el proyecto no compila. `ArchitectureTest`, en `boot`, comprueba con ArchUnit lo que Maven no ve: que los puertos no usen tipos técnicos, que el código generado no salga de su adaptador, que los adaptadores no dependan entre sí y que cada adaptador implemente un único puerto.

```text
domain          ← sin dependencias
application     → domain
infrastructure  → domain, application
api-contract    → spring-web, jakarta.validation, jackson-annotations
soap-contract   → jakarta.xml.bind
api-rest        → application, api-contract
api-soap        → application, soap-contract
boot            → todos
```
