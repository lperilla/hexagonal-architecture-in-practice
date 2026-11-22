# Guion de ensayo de la charla

Guion para ensayar y dar la charla **«Arquitectura hexagonal en la práctica: del diseño al código»** del 4 de diciembre de 2026: qué hacer en cada pantalla, con qué comandos y qué hacer si algo falla.

Todos los comandos son de PowerShell y se han probado en la máquina de la charla. Las funciones vienen de [`docs/ensayo.ps1`](ensayo.ps1).

---

## Cómo está montado

Hay dos carpetas:

| Carpeta | Para qué | En qué estado |
|---|---|---|
| `hexagonal-architecture-in-practice` | La presentación (`docs/`), el script de apoyo y los sobres SOAP | Siempre en `main`: no se toca durante la charla |
| `hexagonal-live`, al lado | El código en vivo, abierto en VS Code | Va saltando de etiqueta en etiqueta |

Son dos copias del mismo repositorio (`git worktree`). Se separan porque al hacer checkout de una etiqueta antigua desaparece la carpeta `docs/`, y con ella la presentación.

**La regla de oro del código en vivo:** no se escribe código, se salta de etiqueta. Cada paso es `Use-Etiqueta` más enseñar los ficheros en VS Code. Si algo se tuerce, `Reset-CopiaEnVivo` y la siguiente etiqueta.

---

## 1. Una sola vez, antes del primer ensayo

### Entorno permanente

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\dev-tools\java\jdk-25.0.4.1', 'User')
```

```powershell
[Environment]::SetEnvironmentVariable('JAVA_TOOL_OPTIONS', '-Dfile.encoding=UTF8 -Djdk.net.unixdomain.tmpdir=C:\lperilla\tmp', 'User')
```

Cierra y abre PowerShell y VS Code para que cojan las variables. `Initialize-Charla` las fija igualmente en cada sesión, así que el ensayo funciona aunque no lo hagas.

### Copia en vivo y dependencias

Desde la raíz del repositorio:

```powershell
. .\docs\ensayo.ps1
```

```powershell
Initialize-Charla; Start-Infraestructura; New-CopiaEnVivo
```

```powershell
Test-Etiquetas
```

`Test-Etiquetas` verifica las 11 etiquetas con `clean verify`, en unos 8 minutos. Deja en tu equipo todas las dependencias de Maven, de modo que el día de la charla se puede compilar **sin conexión**. Todas deben salir `OK`.

Abre la copia en vivo en VS Code y déjala abierta:

```powershell
code ..\hexagonal-live
```

---

## 2. El día de la charla, 30 minutos antes

```powershell
. .\docs\ensayo.ps1
```

```powershell
Initialize-Charla; Start-Infraestructura
```

`Start-Infraestructura` arranca la máquina de Podman y aparta el socket huérfano de `%TEMP%\podman`, si lo hay. Después levanta MongoDB, RabbitMQ y Mailpit y espera a que estén sanos. Debe terminar con los tres contenedores `Up`.

Prepara los JAR de la versión final, que son los que se usan en las demos:

```powershell
Use-Etiqueta hexagonal/fase-2-notificaciones; Build-EnVivo; Reset-Demo
```

Deja la copia en vivo en el punto de partida y abre la presentación y las consolas:

```powershell
Use-Etiqueta 00-start; Open-Charla; Open-Consolas
```

Comprobaciones finales:

- La presentación está en **modo oscuro**. Pulsa P para el modo presentación y T para arrancar el cronómetro al empezar.
- Mailpit (http://localhost:8025) y RabbitMQ (http://localhost:15672, usuario y clave `petstore`) abren.
- VS Code muestra `hexagonal-live` en `00-start`.
- La terminal tiene la letra grande, para que la sala la lea.

---

## 3. Pantalla a pantalla

La columna **Minuto** es el plan acumulado: el cronómetro de la presentación se pone en rojo si vas por detrás. Con la tecla N ves las notas del ponente de cada pantalla.

### Introducción y bloque 1 (minutos 0 a 31)

| Minuto | Pantalla | Qué hacer | Comandos |
|---|---|---|---|
| 0 | `#m0-s1` Portada | Presentar el caso en una frase y los tres bloques | |
| 1 | `#m0-s2` Pregunta | Que alguien responda en voz alta. **No dar la respuesta** | |
| 2 | `#m1-s1` a `#m1-s3` | Enseñar el código de la rama `mvc` en VS Code (`controller`, `model`, `service`) | `Use-Etiqueta 00-start` (ya está) |
| 5 | `#m1-s4` SOAP captura HTTP | **Pantalla clave.** Abrir `soap/AdoptionEndpoint.java` y leer las líneas del `catch` | |
| 6 | `#m1-s5` | Pregunta de comprobación (S para la solución) | |
| 7 | `#m2-s1` a `#m2-s8` | Teoría. Recortables: `#m2-s4` y `#m2-s6`. La clave es `#m2-s7` (regla de dependencias) y `#m2-s8` (módulos) | |
| 13 | `#m3-s1` Paso 1 | Saltar a la etiqueta y enseñar `Pet.java`. Remarcar que es un **record inmutable**: `adopt()` devuelve otra mascota, no hay setters | `Use-Etiqueta 01-domain` |
| 14 | `#m3-s2` | Ejecutar el test del dominio, unos 10 s y sin Spring. Señalar `adoptingDoesNotChangeTheOriginalPet` | `Test-Modulo services/adoption-service/domain` |
| 15 | `#m3-s3` Por qué inmutable | La frase de arriba y una o dos filas de la tabla: nadie se salta la regla, no hay estados a medias | |
| 16 | `#m3-s4` Paso 2 | Enseñar el puerto de entrada y `PetService implements AdoptPetUseCase` | `Use-Etiqueta 02-input-port` |
| 18 | `#m3-s5`, `#m3-s6` Paso 3 | Enseñar `AdoptPetService` (sin `@Service`, guarda la mascota que devuelve `adopt()`) y los tres puertos. Ejecutar el test del caso de uso | `Use-Etiqueta 03-output-ports` y `Test-Modulo services/adoption-service/application` |
| 20 | `#m4-s1` Paso 4 | MongoDB: `MongoLoadPetAdapter` y `MongoSavePetAdapter`, **un puerto cada uno**, y `PetMongoMapper` | `Use-Etiqueta 04-mongodb-adapter` |
| 22 | `#m4-s2` Paso 5 | RabbitMQ: `RabbitPetAdoptedPublisher`. Outbox en 30 segundos | `Use-Etiqueta 05-rabbitmq-adapter` |
| 23 | `#m4-s3` Paso 6 | **El paso grande:** enseñar el resumen del diff, el controlador y `UseCaseConfig` | `Show-Paso 06-rest-adapter` y `Use-Etiqueta 06-rest-adapter` |
| 25 | `#m4-s4` | Recortable: `RestExceptionHandler` | |
| 25 | `#m4-s5` Paso 7 | SOAP: el mismo caso de uso. Comparar con el endpoint de `00-start` | `Use-Etiqueta 07-soap-adapter` |
| 26 | `#m4-s6` | Estructura final de módulos en el explorador de VS Code | |
| 27 | `#m5-s1` a `#m5-s3` | `notification-service`: el listener como adaptador de entrada. Recortable: `#m5-s3` | `Use-Etiqueta hexagonal/fase-2-notificaciones` |
| 29 | `#m5-s4` Demo | **Momento de magia 1** (ver abajo) | |

**Demo de notificaciones (`#m5-s4`), unos 2 minutos:**

```powershell
Start-Adopcion; Start-Notificaciones
```

```powershell
Invoke-Adopcion 1 Ana
```

Enseñar Mailpit: llega «Ana ha adoptado a Luna». Repetir la adopción: 409, y ningún email nuevo.

```powershell
Invoke-Adopcion 1 Bea
```

Si sobra tiempo, publica un mensaje roto desde la consola de RabbitMQ:
1. Ir a **Exchanges** → `petstore.events` → **Publish message**.
2. Routing key `pet.adopted`, propiedad `content_type` = `application/json`, payload `{"petId":"9","petName":"Roto"}`.
3. Enseñarlo en la cola `notification.pet-adopted.dlq`, después de tres reintentos.

### Bloque 2 (minutos 31 a 40)

| Minuto | Pantalla | Qué hacer | Comandos |
|---|---|---|---|
| 31 | `#m6-s1`, `#m6-s2` | Lado a lado en la presentación. No hace falta VS Code | |
| 33 | `#m6-s3` | Recortable: la tabla pieza a pieza | |
| 34 | `#m7-s1` a `#m7-s3` | Errores y buenas prácticas. Recortable: `#m7-s2`. Elegir dos o tres puntos | |
| 38 | `#m7-s4` ArchUnit | Opcional: el error de compilación en directo (ver abajo) | |

**Demo opcional: Spring en el dominio no compila, unos 40 segundos.**
1. Ejecuta `Use-Etiqueta 08-testing`.
2. Añade a `domain/.../model/Pet.java` la línea `import org.springframework.stereotype.Component;` y la anotación `@Component` encima de `public record Pet`.
3. Ejecuta `Test-Modulo services/adoption-service/domain`. Sale `package org.springframework.stereotype does not exist` y `BUILD FAILURE`.
4. Deshazlo:

```powershell
Reset-CopiaEnVivo
```

### Bloque 3 y cierre (minutos 40 a 50)

| Minuto | Pantalla | Qué hacer | Comandos |
|---|---|---|---|
| 40 | `#m8-s1` | **Momento de magia 2:** quitar MongoDB (ver abajo) | |
| 42 | `#m8-s2` | Testing por capas. Si da tiempo, el test del caso de uso | `Test-Modulo services/adoption-service/application` |
| 43 | `#m8-s3` | Ventajas y costes: decirlos en voz alta | |
| 44 | `#m9-s1` a `#m9-s4` | Decisiones. Elegir dos o tres filas de `#m9-s2`. `#m9-s4` es material de consulta | |
| 48 | `#m10-s1` a `#m10-s3` | Cinco reglas, la pregunta del principio (S para la solución) y para seguir | |
| 50 | | Turno de preguntas: la guía de teoría tiene las difíciles (sección 18) | |

**Demo: quitar MongoDB sin tocar el negocio (`#m8-s1`), unos 2 minutos.** La copia en vivo sigue en `hexagonal/fase-2-notificaciones`, que incluye el perfil `in-memory`.

```powershell
Stop-Servicios; podman stop petstore-mongodb
```

```powershell
Start-Adopcion -InMemory
```

```powershell
Invoke-Adopcion 3 Ana
```

Responde 200 con la base de datos parada. Una segunda adopción da 409:

```powershell
Invoke-Adopcion 3 Bea
```

Después, vuelve a dejarlo todo como estaba:

```powershell
Stop-Servicios; podman start petstore-mongodb
```

---

## 4. Si algo falla en directo

| Síntoma | Qué hacer |
|---|---|
| La terminal se queda colgada | Ctrl+C y seguir con la siguiente pantalla: las diapositivas tienen el mismo código |
| `Use-Etiqueta` avisa de cambios sin guardar | `Reset-CopiaEnVivo` y repetir |
| `Build-EnVivo` o `Test-Modulo` fallan sin conexión | Repetir `Build-EnVivo -Online`. Si no hay red, enseñar el test en las diapositivas |
| Un servicio «no ha arrancado» | Mirar `C:\lperilla\tmp\adopcion.log` o `notificaciones.log`. Si dice `Port 8080 was already in use`: `Stop-Servicios` y volver a arrancarlo |
| `Invoke-Adopcion` da `HTTP 000` | El servicio no está en marcha: `Start-Adopcion` |
| La adopción da 409 desde el principio | La mascota ya estaba adoptada: `Stop-Servicios; Reset-Demo; Start-Adopcion` |
| No llega el email | `notification-service` no está en marcha (`Start-Notificaciones`), o Mailpit no está abierto. Plan B: `Start-Notificaciones -SoloLog` y enseñar el log |
| Podman no arranca o `win-sshproxy` falla | `podman machine stop` y repetir `Start-Infraestructura`, que aparta el socket huérfano |
| Los contenedores no responden en `localhost` | Ver la tabla de problemas de Podman en el README. Plan B: hacer la charla solo con diapositivas y código en VS Code |
| Vas tarde | Las notas (N) dicen qué pantallas son recortables y cuánto ahorran. Prioridad: no recortar `#m1-s4`, `#m2-s7`, `#m4-s5`, `#m5-s4` ni `#m8-s1` |

---

## 5. Después de cada ensayo

Anota en qué minuto real terminas cada bloque y compáralo con el plan:

| Hasta | Plan | Ensayo 1 | Ensayo 2 |
|---|---|---|---|
| Fin de la introducción (`#m0-s2`) | 2 | | |
| Fin del bloque 1 (`#m5-s4`) | 31 | | |
| Fin del bloque 2 (`#m7-s4`) | 40 | | |
| Fin del bloque 3 (`#m9-s4`) | 48 | | |
| Fin del cierre | 50 | | |

Si el bloque 1 se alarga, lo habitual es recortar teoría (`#m2-s4`, `#m2-s6`) y pasar más rápido por los pasos 4 y 5. Para recuperar el estado inicial al acabar:

```powershell
Stop-Servicios; Reset-Demo; Use-Etiqueta 00-start
```

---

## Funciones de `docs/ensayo.ps1`

| Función | Qué hace |
|---|---|
| `Initialize-Charla` | Java 25 y la carpeta de sockets AF_UNIX, para esta sesión |
| `Start-Infraestructura` | Podman (con el arreglo del socket huérfano) y `compose.yaml` |
| `New-CopiaEnVivo` | Crea `..\hexagonal-live` en `00-start` |
| `Use-Etiqueta <etiqueta>` | Lleva la copia en vivo a una etiqueta |
| `Show-Paso <etiqueta>` | Resumen de cambios respecto a la etiqueta anterior |
| `Reset-CopiaEnVivo` | Descarta lo escrito en directo |
| `Test-Modulo <ruta>` | Tests de un módulo, sin conexión (10 a 15 s) |
| `Build-EnVivo [-Tests] [-Online]` | Compila la copia en vivo (por defecto sin tests ni conexión) |
| `Start-Adopcion [-InMemory]` | Arranca `adoption-service` |
| `Start-Notificaciones [-SoloLog]` | Arranca `notification-service` |
| `Stop-Servicios` | Para los servicios arrancados |
| `Reset-Demo` | Borra los datos de la demo (vuelven Luna, Thor y Kira al reiniciar) |
| `Invoke-Adopcion <id> <dueño>` | `POST /pets/{id}/adoption` y la respuesta completa |
| `Invoke-AdopcionSoap <sobre>` | Envía un sobre de `soap-requests` (por ejemplo, `01-adopt-pet`) |
| `Open-Charla` / `Open-Consolas` | Presentación, Mailpit y RabbitMQ en el navegador |
| `Test-Etiquetas` | `clean verify` de todas las etiquetas |
