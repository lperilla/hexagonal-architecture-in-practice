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
./mvnw -pl services/adoption-service -am verify      # un solo servicio
```

En PowerShell, sustituye `./mvnw` por `.\mvnw.cmd`.

Los tests de extremo a extremo (`AdoptionEndToEndTest`) usan la infraestructura de `compose.yaml`: levántala antes con `podman compose up -d`. Usan su propia base de datos, `petstore-adoption-test`, y no tocan los datos de la demo.

## Demo

Arranca la infraestructura y el servicio de adopción:

```bash
podman compose up -d
./mvnw -pl services/adoption-service -am -DskipTests package
java -jar services/adoption-service/target/adoption-service-0.0.1-SNAPSHOT.jar
```

Al arrancar, el servicio carga tres mascotas disponibles si no existen: 1 Luna, 2 Thor y 3 Kira.

| Canal | Dónde | Casos |
|---|---|---|
| REST | `requests.http` (REST Client de VS Code o IntelliJ) | 200, 409, 404 y 400 en `POST /pets/{petId}/adoption` |
| SOAP | `soap-requests/` | Adopción, `PetNotAvailable`, `PetNotFound` y nombre vacío. WSDL en http://localhost:8080/ws/adoption.wsdl |
| Evento | Consola de RabbitMQ | Cada adopción publica `pet.adopted` en el exchange `petstore.events` (contrato en `contracts/events/`) |

Para volver al estado inicial, borra la base de datos y reinicia el servicio:

```bash
podman exec petstore-mongodb mongosh --quiet petstore-adoption --eval "db.dropDatabase()"
```

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
│   └── adoption-service/
└── docs/            Presentación, guía de teoría y cartel
```
