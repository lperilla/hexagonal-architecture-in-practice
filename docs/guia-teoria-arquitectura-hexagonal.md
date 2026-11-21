# Arquitectura hexagonal explicada fácil

Guía del ponente para la charla del 4 de diciembre de 2026. Sirve para entender bien cada concepto y para contarlo sin leer definiciones.

---

## Cómo usar esta guía

Cada concepto sigue el mismo esquema:

- **Qué es**, en una o dos frases.
- **En el restaurante**, una analogía que se repite en toda la guía.
- **En el Petstore**, la clase concreta del ejemplo de la charla.
- **Cuéntalo en 30 segundos**, un guion para decirlo en voz alta.
- **Confusión típica**, lo que la gente suele entender mal.
- **Pregunta al público**, para que la charla no sea un monólogo.

No hace falta memorizar las definiciones. Si entiendes el restaurante, puedes reconstruir cualquier concepto en el momento.

---

## La idea en una frase

> El negocio va en el centro y no sabe nada de tecnología. La tecnología va en el borde y se adapta al negocio, nunca al revés.

La propuso Alistair Cockburn en 2005 con el nombre *Ports and Adapters*: puertos y adaptadores. El hexágono no significa nada especial. Es solo un dibujo con muchos lados para que quepan varias entradas y salidas, sin la idea de "arriba" y "abajo" de las capas clásicas.

---

## El hilo conductor: un restaurante

Usa esta analogía durante toda la charla. Funciona porque todo el mundo ha estado en un restaurante.

Un restaurante recibe pedidos por muchos canales: la mesa, el teléfono o una app de reparto. La cocina no debería cambiar su forma de trabajar según el canal. Tampoco debería cambiar sus recetas cada vez que cambia de proveedor de tomates.

| En el restaurante | En la arquitectura hexagonal | En el Petstore |
|---|---|---|
| Las recetas y normas de la casa ("no se sirve pescado que no sea fresco") | Dominio | `Pet`, con la regla de `adopt()` |
| El jefe de cocina, que organiza cada pedido | Servicio de aplicación | `AdoptPetService` |
| "Preparar un menú del día" | Caso de uso | Adoptar una mascota |
| La comanda, una hoja estándar con el pedido | Comando | `AdoptPetCommand` |
| La ventanilla donde se entregan las comandas | Puerto de entrada | `AdoptPetUseCase` |
| El camarero, la centralita o la tablet de la app de reparto | Adaptadores de entrada | `PetRestController` |
| "Necesito tomates", "necesito avisar al repartidor" | Puertos de salida | `LoadPetPort`, `SavePetPort`, `PublishPetAdoptedPort` |
| El proveedor concreto y la empresa de reparto concreta | Adaptadores de salida | `MongoPetAdapter`, `KafkaPetAdoptedAdapter` |
| El encargado que decide con qué proveedor se trabaja | Configuración | `AdoptionConfiguration` |

La frase clave: **la cocina dice "necesito tomates", no "necesito ir a tal supermercado"**. Si cambias de proveedor, las recetas siguen igual.

---

## 1. Dominio

**Qué es.** Las reglas y conceptos del negocio: qué cosas existen, qué se puede hacer con ellas y qué no está permitido. No usa ningún framework y debería entenderlo alguien de negocio si se lo lees en voz alta.

**En el restaurante.** Las recetas y las normas de la casa. Son las mismas aunque cambies de horno o de proveedor.

**En el Petstore.** La clase `Pet` y su método `adopt()`:

```java
public void adopt(Owner newOwner) {
    if (status != PetStatus.AVAILABLE) {
        throw new PetNotAvailableException(id);
    }
    this.status = PetStatus.ADOPTED;
    this.owner = newOwner;
}
```

También son dominio `PetId`, `Owner`, `PetStatus`, la excepción `PetNotAvailableException` y el evento `PetAdopted`.

**Cuéntalo en 30 segundos.**
> "El dominio es lo que seguiría siendo verdad aunque mañana tiráramos Spring, MongoDB y Kafka. Que una mascota adoptada no se puede volver a adoptar es una regla del negocio, no de la base de datos. Por eso vive en una clase Java pura, sin anotaciones, y se prueba en milisegundos."

**Confusión típica.** Pensar que el dominio son "las entidades de la base de datos". `PetDocument` con `@Document` no es dominio: es cómo se guarda una mascota en MongoDB. `Pet` es qué es una mascota para el negocio.

**Pregunta al público.** "Si cambiamos MongoDB por PostgreSQL, ¿cambia la regla de que no se puede adoptar dos veces?" La respuesta obvia, que no, demuestra que la regla no pertenece a la base de datos.

---

## 2. Infraestructura

**Qué es.** Toda la tecnología concreta que podría cambiar sin que cambie el negocio: el framework web, la base de datos, el broker de mensajes, los clientes HTTP, la configuración.

**En el restaurante.** El horno, la cámara frigorífica, el datáfono, la empresa de reparto. Son necesarios, pero no definen qué cocina el restaurante.

**En el Petstore.** Spring MVC, MongoDB, Kafka y las clases `@Configuration`. En hexagonal, todo eso vive en los adaptadores y en la configuración.

**Cuéntalo en 30 segundos.**
> "La infraestructura es todo aquello de lo que podríamos decir 'esto lo cambiamos el año que viene'. Es importante, pero es un detalle. La idea es que esos detalles estén en el borde, donde cambiarlos sea barato."

**Truco para clasificar cualquier clase.** Pregúntate: *si cambio la tecnología, ¿esta clase sigue teniendo sentido?* Si sigue teniéndolo, es dominio o aplicación. Si desaparece, es infraestructura.

**Pregunta al público.** Lee frases y que el público diga dónde van:
- "Una mascota solo se adopta si está disponible." → Dominio.
- "Se guarda en la colección `pets`." → Infraestructura.
- "Si no está disponible, devuelve un 409." → Adaptador REST, es decir, infraestructura.

Este juego funciona muy bien para despertar a la sala.

---

## 3. Caso de uso y servicio de aplicación

**Qué es.**
- Un **caso de uso** es una intención del usuario, dicha en lenguaje de negocio: "adoptar una mascota". Es lo que la aplicación sabe hacer.
- El **servicio de aplicación** es la clase que lo lleva a cabo. Organiza los pasos, pero no toma las decisiones de negocio: se las pregunta al dominio.

**En el restaurante.** El caso de uso es "preparar un menú del día". El jefe de cocina es el servicio de aplicación: pide los ingredientes, sigue la receta, emplata y avisa al camarero. No inventa la receta.

**En el Petstore.**

```java
public AdoptionResult adopt(AdoptPetCommand command) {
    Pet pet = loadPetPort.load(new PetId(command.petId()))
            .orElseThrow(() -> new PetNotFoundException(command.petId()));

    pet.adopt(new Owner(command.ownerName()));   // decide el dominio

    savePetPort.save(pet);
    publishPetAdoptedPort.publish(PetAdopted.from(pet));
    return AdoptionResult.from(pet);
}
```

**Cuéntalo en 30 segundos.**
> "El servicio de aplicación es un director de orquesta: carga la mascota, le dice 'adóptate', la guarda y avisa al resto del sistema. Fijaos en que no hay ningún if de negocio. Si se lo preguntas, te dice: eso decídelo tú, mascota."

**Cómo distinguir dominio y aplicación.**
- Si la frase suena a *"si pasa esto, no se puede"*, es dominio.
- Si suena a *"primero esto, luego aquello"*, es aplicación.

**Confusión típica.** Meter todas las reglas en el servicio y dejar las entidades como bolsas de getters y setters. Eso se llama *modelo anémico*. Funciona, pero las reglas acaban repartidas y duplicadas.

---

## 4. Comando, consulta y resultado

**Qué es.**
- Un **comando** es un objeto inmutable con los datos que necesita una acción que cambia algo. Se nombra en imperativo: `AdoptPetCommand`.
- Una **consulta** pide datos sin cambiar nada: `GetPetQuery`.
- Un **resultado** es lo que devuelve el caso de uso: `AdoptionResult`.

Los tres pertenecen al núcleo, no a la API.

**En el restaurante.** La comanda. El cliente puede pedir a voces, por teléfono o con la app, pero a la cocina siempre le llega la misma hoja con el mismo formato. Al cocinero le da igual cómo se hizo el pedido.

**En el Petstore.** El recorrido de los datos es este:

```text
AdoptPetRequest    (JSON que llega por REST)      → adaptador
AdoptPetCommand    (lo que necesita el caso de uso) → núcleo
AdoptionResult     (lo que devuelve el caso de uso) → núcleo
AdoptionResponse   (JSON que sale por REST)         → adaptador
```

**Cuéntalo en 30 segundos.**
> "¿Por qué no pasar directamente el request REST al servicio? Porque mañana la adopción puede llegar por Kafka o por un proceso nocturno. Si el núcleo recibe un comando, cada canal traduce lo suyo y el núcleo ni se entera de que el canal ha cambiado. Es la comanda del restaurante."

**Y la validación, ¿dónde va?**
- El formato (campo vacío, JSON mal formado) se valida en el adaptador, con `@Valid`.
- Las reglas de negocio (la mascota debe estar disponible) se validan en el dominio.

**Confusión típica.** Pensar que el comando es "otro DTO más, duplicado". Se parecen hoy porque el ejemplo es sencillo. Se separan en cuanto la API añade un campo de presentación, se versiona o entra un segundo canal.

---

## 5. Puertos

Un **puerto** es una interfaz que define una conversación con el exterior, en el idioma del núcleo. La regla de oro: **el puerto siempre lo define el núcleo**, nunca la tecnología.

**Analogía extra.** El puerto USB-C de un portátil. El portátil decide la forma del puerto, y los fabricantes de cables y adaptadores se ajustan a ella. El portátil no cambia su forma para cada cable.

### Puerto de entrada (driving, primario)

**Qué es.** Lo que la aplicación ofrece al mundo.

**En el restaurante.** La ventanilla donde se dejan las comandas.

**En el Petstore.**

```java
public interface AdoptPetUseCase {
    AdoptionResult adopt(AdoptPetCommand command);
}
```

Lo implementa el servicio de aplicación y lo llama un adaptador de entrada.

### Puerto de salida (driven, secundario)

**Qué es.** Lo que la aplicación necesita del mundo.

**En el restaurante.** "Necesito tomates" o "necesito avisar al repartidor". No dice qué proveedor ni qué empresa de reparto.

**En el Petstore.** `LoadPetPort`, `SavePetPort` y `PublishPetAdoptedPort`. Los declara la aplicación y los implementa un adaptador de salida.

### La tabla que lo aclara todo

| | Puerto de entrada | Puerto de salida |
|---|---|---|
| Qué expresa | Lo que la aplicación **ofrece** | Lo que la aplicación **necesita** |
| Quién lo define | El núcleo | El núcleo |
| Quién lo implementa | El servicio de aplicación | Un adaptador de salida |
| Quién lo llama | Un adaptador de entrada | El servicio de aplicación |

**Cuéntalo en 30 segundos.**
> "Un puerto es un contrato que escribe el núcleo. El de entrada dice 'esto sé hacer', y el de salida dice 'esto necesito'. Fijaos en el nombre: `SavePetPort`, no `MongoPetPort`. La cocina pide tomates, no un supermercado concreto."

**Confusión típica.** Pensar que los puertos de salida los implementa el núcleo. Es justo al revés: el núcleo los declara y la tecnología los implementa. Esta es la fila de la tabla en la que más se equivoca la gente.

**Pregunta al público.** "¿`MongoPetRepositoryPort` es un buen nombre?" No lo es: mete la tecnología dentro del núcleo.

---

## 6. Adaptadores

**Qué es.** Código que traduce entre una tecnología concreta y un puerto. **Solo traduce**: no contiene reglas de negocio.

**En el restaurante.**
- Los adaptadores de entrada son el camarero, la centralita y la tablet de la app de reparto. Cada uno recibe el pedido a su manera y lo convierte en una comanda estándar.
- Los adaptadores de salida son el proveedor concreto y la empresa de reparto concreta. Cambiar de proveedor no cambia las recetas.

**En el Petstore.**
- **De entrada:** `PetRestController`. Recibe JSON, crea el comando, llama al caso de uso y convierte el resultado en JSON. También traduce excepciones: `PetNotAvailableException` se convierte en un 409. Otros ejemplos serían un listener de Kafka, un job batch o un test.
- **De salida:** `MongoPetAdapter` convierte `Pet` en `PetDocument` y viceversa, y `KafkaPetAdoptedAdapter` publica el evento. El adaptador en memoria también es un adaptador de salida.

**Cuéntalo en 30 segundos.**
> "Los adaptadores son traductores. El controlador traduce de HTTP al idioma del negocio, y el adaptador de Mongo traduce del idioma del negocio a documentos. Si un adaptador tiene un if de negocio, ese if está en el sitio equivocado: el camarero no decide si el pescado está fresco."

**Confusión típica.** Pensar que los mappers son "código duplicado". Son el precio explícito de que el negocio no dependa de la tecnología. Ese precio merece la pena cuando el dominio tiene reglas, y no cuando es un CRUD trivial.

---

## 7. La regla de dependencias

Es la idea más importante de la charla. Si el público se lleva solo una cosa, que sea esta.

> Las dependencias de código apuntan hacia el núcleo. El dominio no importa nada de infraestructura.

### El truco: el flujo y la dependencia van en sentidos contrarios

```text
En ejecución (quién llama a quién):
AdoptPetService ──llama──▶ SavePetPort ──▶ MongoPetAdapter
El control va de dentro hacia fuera.

En el código (quién conoce a quién):
AdoptPetService ──▶ SavePetPort ◀──implementa── MongoPetAdapter
La dependencia va de fuera hacia dentro.
```

Esto se llama **inversión de dependencias**. En ejecución, el servicio acaba guardando en MongoDB, pero en el código solo conoce la interfaz `SavePetPort`. Por eso puedes cambiar MongoDB sin tocar el servicio.

**En el restaurante.** El jefe de cocina llama al proveedor para pedir tomates; el control va de la cocina hacia fuera. Pero quien se adapta es el proveedor: entrega en el formato y en el horario que fija la cocina.

**Cuéntalo en 30 segundos.**
> "Aquí está el truco de magia. En ejecución, el servicio llama a Mongo. Pero si abrís el código del servicio, no hay ni un import de Mongo: solo una interfaz que él mismo ha definido. Es Mongo quien se adapta al servicio. Por eso podemos cambiar Mongo por memoria y el servicio ni se entera."

**Confusión típica.** Creer que la arquitectura hexagonal consiste en tener carpetas llamadas `domain`, `application` y `adapter`. Puedes tener esas carpetas y que el dominio importe clases de Mongo. Lo que cuenta es la dirección de las dependencias, y eso se puede comprobar automáticamente con ArchUnit.

---

## 8. Hexagonal frente a capas clásicas

```text
Capas clásicas                    Hexagonal

Controller                        REST ──▶ NÚCLEO ◀── Mongo
    ▼                                        ▲
Service (negocio)                            │
    ▼                                      Kafka
Repository (datos)

El negocio depende de los datos.  Todo depende del núcleo.
```

**Cuéntalo en 30 segundos.**
> "En capas, el negocio está encima de la base de datos y depende de ella. En hexagonal, la base de datos es un enchufe más, como la API REST. El código de la primera parte de la charla, el `PetService` acoplado, era una arquitectura en capas."

Si te preguntan por *Clean Architecture* (Robert C. Martin) u *Onion Architecture* (Jeffrey Palermo): son variantes con la misma regla de fondo, que las dependencias apuntan hacia el negocio. Cambian los nombres y el número de anillos.

---

## 9. DDD: qué tiene que ver

**La aclaración que hay que hacer siempre:** hexagonal y DDD no son lo mismo.

| | Arquitectura hexagonal | DDD (Domain-Driven Design) |
|---|---|---|
| Qué decide | Dónde está la frontera y hacia dónde apuntan las dependencias | Cómo modelar el negocio dentro de esa frontera |
| Pregunta que responde | ¿Qué puede cambiar sin tocar el negocio? | ¿Qué conceptos y reglas tiene el negocio? |
| Origen | Alistair Cockburn, 2005 | Eric Evans, 2003 |

Son independientes y se complementan. En el restaurante, la hexagonal es la distribución del local: la cocina separada del comedor y de los proveedores. DDD es cómo se escriben las recetas.

### DDD estratégico, solo dos ideas

- **Lenguaje ubicuo.** El código usa las palabras del negocio. Escribimos `pet.adopt(owner)` y no `pet.setStatus("ADOPTED")`. Si negocio dice "adoptar", el código dice "adoptar".
- **Contexto delimitado (bounded context).** Una palabra puede significar cosas distintas en distintas partes del negocio. "Mascota" en Adopción tiene dueño y estado de adopción. En Veterinaria tiene vacunas e historial clínico. No hace falta forzar una única clase `Pet` para todo.

### DDD táctico en el Petstore

| Pieza | Qué es | En el restaurante | Petstore |
|---|---|---|---|
| Entidad | Tiene identidad que se mantiene aunque cambien sus datos | Una mesa concreta: la 7 sigue siendo la 7 aunque cambien los comensales | `Pet` |
| Objeto valor | Se define por sus atributos, es inmutable y no tiene identidad propia | Un billete de 10 €: da igual cuál sea | `PetId`, `Owner`, `PetStatus` |
| Agregado | Grupo que se modifica como una unidad, siempre a través de su raíz | Una comanda con sus platos: no se cambia un plato sin pasar por la comanda | `Pet` es la raíz |
| Evento de dominio | Algo relevante que ya ha ocurrido, dicho en pasado | "Mesa 7 servida" | `PetAdopted` |
| Repositorio | Colección de agregados. En hexagonal es un puerto de salida | La despensa | `LoadPetPort`, `SavePetPort` |
| Servicio de dominio | Regla que no pertenece de forma natural a ninguna entidad | Decidir qué mesa se asigna a un grupo | No lo necesitamos, así que no lo forzamos |

**Cuéntalo en 30 segundos.**
> "La hexagonal construye la caja fuerte; DDD decide qué guardamos dentro y cómo lo ordenamos. Podéis hacer una sin la otra, pero juntas funcionan muy bien. El punto donde se tocan es el repositorio: en DDD es una colección de mascotas, y en hexagonal es un puerto de salida."

---

## 10. SOLID: ya lo habéis visto sin daros cuenta

No expliques SOLID en abstracto. Señala dónde aparece cada principio en el código que ya se ha visto en la charla.

| Principio | Qué dice | En el restaurante | En el Petstore |
|---|---|---|---|
| **S**: responsabilidad única | Una clase tiene un solo motivo para cambiar | El camarero no cocina y el cocinero no cobra | El controlador cambia si cambia la API, el adaptador Mongo si cambia la persistencia y `Pet` si cambia la regla |
| **O**: abierto/cerrado | Se puede extender sin modificar lo que ya funciona | Añadir reparto a domicilio sin reescribir las recetas | Añadimos `InMemoryPetAdapter` sin tocar `AdoptPetService` |
| **L**: sustitución de Liskov | Cualquier implementación de un contrato debe poder sustituir a otra sin romper nada | Cualquier proveedor de tomates debe traer tomates, no pimientos | El adaptador en memoria cumple el mismo contrato que el de Mongo: lo que se guarda se puede cargar |
| **I**: segregación de interfaces | Interfaces pequeñas, pensadas para quien las usa | Al repartidor le das la dirección, no la carta entera | `LoadPetPort` y `SavePetPort` están separados |
| **D**: inversión de dependencias | Lo importante no depende de los detalles; ambos dependen de abstracciones | La cocina fija cómo quiere recibir el género y el proveedor se adapta | El núcleo define `SavePetPort` y Mongo lo implementa |

**La frase para cerrar este bloque:**
> "La D de SOLID es la arquitectura hexagonal aplicada a una clase. La arquitectura hexagonal es la D aplicada a toda la aplicación."

---

## 11. Cuándo sí y cuándo no

Decir esto da credibilidad. Una charla que vende una arquitectura como solución universal genera desconfianza.

**Merece la pena cuando:**
- Hay reglas de negocio de verdad que conviene proteger y probar.
- Hay varios canales de entrada (REST, mensajería, batch) o se esperan más.
- La tecnología puede cambiar: migraciones, nuevos proveedores, nueva nube.
- El sistema va a vivir años y lo van a tocar varios equipos.

**Probablemente sobra cuando:**
- Es un CRUD sin reglas: guardar y leer formularios.
- Es un prototipo o un script de vida corta.
- El equipo no lo conoce y el plazo es muy ajustado.

**Cuéntalo en 30 segundos.**
> "Si vuestro servicio solo guarda y lee formularios, no os hace falta todo esto: tendréis el triple de archivos para nada. La arquitectura tiene que reducir el coste del cambio, no aumentar el número de archivos. Lo que justifica el Petstore no son las altas y bajas de mascotas, sino la regla de adopción y el evento."

---

## 12. Cómo hacer que la charla no sea aburrida

1. **Empieza por el dolor, no por la definición.** Enseña primero el `PetService` acoplado y pregunta qué pasaría si mañana la adopción llega por Kafka. La teoría llega como respuesta a un problema que la sala ya ha visto.
2. **Usa una sola analogía y repítela.** El restaurante aparece en cada concepto. Cambiar de analogía en cada diapositiva confunde.
3. **Haz predicciones.** Antes de cada cambio, pregunta qué creen que va a tener que cambiar. Acertar o fallar hace que la gente preste atención.
4. **Juega a "¿dónde va esto?".** Lee cinco frases y que la sala diga si son dominio, aplicación o adaptador. Dura dos minutos y despierta a todo el mundo.
5. **Guarda un momento de magia.** El cambio de MongoDB por memoria en directo, sin tocar el servicio, es el clímax de la charla. Anúncialo al principio: "dentro de media hora vamos a quitar la base de datos sin tocar el negocio".
6. **No leas las diapositivas.** Las tablas son para la sala. Tú cuenta la versión de 30 segundos.
7. **Admite los costes.** Decir en voz alta "esto tiene más archivos y más mappers" hace que te crean cuando dices que merece la pena.
8. **Cierra con la pregunta del principio.** Recuperar "¿qué cambia si sustituimos MongoDB?" da sensación de historia cerrada.

---

## 13. Preguntas difíciles que te pueden hacer

**"¿No es demasiado código para tan poca cosa?"**
Para este ejemplo, casi. Por eso existe el punto de "cuándo no". El coste se paga una vez; el beneficio aparece cada vez que cambia la tecnología o entra un canal nuevo.

**"¿Por qué no uso directamente el `MongoRepository` de Spring Data como puerto?"**
Porque entonces el núcleo conoce MongoDB: sus tipos, sus métodos y su paginación. El puerto propio dice solo lo que el negocio necesita, y Spring Data se usa dentro del adaptador.

**"¿Puedo poner `@Document` o `@Entity` en la clase de dominio y ahorrarme el mapper?"**
Puedes, y es una decisión pragmática válida en sistemas pequeños. El precio es que el modelo de negocio queda condicionado por la base de datos (constructores vacíos, setters, tipos). Si el dominio es rico, compensa separarlos.

**"¿Dónde va `@Transactional`?"**
En el servicio de aplicación, porque la transacción envuelve el caso de uso completo. Si quieres un núcleo sin Spring, puedes aplicarla desde la configuración envolviendo el servicio. La mayoría de equipos opta por la vía pragmática.

**"¿Hace falta la interfaz del puerto de entrada si solo hay una implementación?"**
No es imprescindible. Muchos equipos llaman directamente al servicio. La interfaz aporta un contrato claro y facilita sustituirlo en los tests del controlador. Es una decisión de equipo, no un dogma.

**"¿Un puerto por cada método?"**
No. Los puertos se diseñan por capacidad. `LoadPetPort` y `SavePetPort` podrían ser un único `PetRepository`; aquí se separan para que los tests puedan dar solo lo que necesitan.

**"¿Hexagonal significa microservicios?"**
No. Es una forma de organizar el interior de una aplicación, sea un monolito o un microservicio. De hecho, facilita partir un monolito más adelante.

**"¿Y si Mongo guarda pero Kafka falla?"**
Es un problema real de consistencia que la hexagonal no resuelve por sí sola. La solución habitual es el patrón *Transactional Outbox*: guardar el evento en la misma transacción que la mascota y publicarlo después desde un proceso aparte.

---

## Chuleta de una página

| Concepto | En una frase | Restaurante | Petstore |
|---|---|---|---|
| Dominio | Las reglas del negocio, sin tecnología | Recetas y normas de la casa | `Pet.adopt()` |
| Infraestructura | La tecnología que podría cambiar | Horno, datáfono, reparto | Spring, MongoDB, Kafka |
| Caso de uso | Una intención en lenguaje de negocio | Preparar un menú del día | Adoptar una mascota |
| Servicio de aplicación | Organiza los pasos del caso de uso | Jefe de cocina | `AdoptPetService` |
| Comando | Datos de una acción que cambia algo | La comanda | `AdoptPetCommand` |
| Puerto de entrada | Lo que la aplicación ofrece | Ventanilla de comandas | `AdoptPetUseCase` |
| Puerto de salida | Lo que la aplicación necesita | "Necesito tomates" | `SavePetPort` |
| Adaptador de entrada | Traduce un canal a un caso de uso | Camarero, app de reparto | `PetRestController` |
| Adaptador de salida | Implementa un puerto con tecnología | Proveedor concreto | `MongoPetAdapter` |
| Regla de dependencias | Todo depende del núcleo, nunca al revés | El proveedor se adapta a la cocina | Ningún import de Mongo en el núcleo |
| DDD | Cómo modelar el negocio por dentro | Cómo se escriben las recetas | Entidad, objeto valor, evento |
| SOLID (D) | Lo importante no depende de los detalles | La cocina fija el formato del pedido | El núcleo define los puertos |

**La frase final de la charla:**
> Hexagonal no consiste en dibujar un hexágono. Consiste en conservar las decisiones de negocio cuando cambian las decisiones tecnológicas.
