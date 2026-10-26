# Avance 3 — Declaración de entidades y entradas/salidas

## 10. Entradas y salidas del sistema — actualización para el Avance 3

Las entradas y salidas se describen según las operaciones implementadas en las clases Java, los controladores, los servicios y los objetos de transferencia de datos (DTO). Se distingue entre operaciones disponibles en consola, operaciones implementadas parcialmente y funcionalidades pendientes.

En el arranque actual, `Main` configura persistencia JDBC para habitaciones y reservas e inyecta `FacturaJdbcDAO` en `ProcesadorFacturas`. Los clientes asociados a reservas continúan almacenándose mediante `ReservaJdbcDAO`. También existe `ClienteJdbcDAO` para operaciones independientes de persistencia, aunque no está integrado en una opción de registro del menú. Los DAO de archivos permanecen como alternativas; el constructor sin argumentos de `ProcesadorFacturas` conserva esa configuración, pero `Main` utiliza explícitamente JDBC.

### 10.1. Operaciones y estado de implementación

| Operación / historia de usuario | Datos de entrada | Datos de salida | Estado real de implementación | Limitaciones relevantes |
|---|---|---|---|---|
| Registro de clientes — HU-002 | Nombre, teléfono y correo electrónico opcional, recibidos durante la creación de una reserva. | Objeto `Cliente` con identificador UUID; sus datos se guardan junto con la reserva en el flujo JDBC. | Parcial: integrado en la creación de reservas. | No existe una opción independiente de registro en el menú. Cada solicitud crea un cliente nuevo; el DTO no permite seleccionar uno existente por su ID. El resumen no incluye el ID del cliente. Existe `ClienteJdbcDAO` con operaciones CRUD, pero su disponibilidad no constituye un flujo independiente accesible desde el menú. |
| Consulta de habitaciones — HU-003 | Solicitud de consulta, sin filtros en el controlador actual. | `List<Integer>` con los números de habitaciones cuyo estado es `DISPONIBLE`. | Disponible en consola. | No devuelve tipo ni precio. No recibe filtros por tipo o fechas. |
| Creación de reservas — HU-004 | Nombre, teléfono, correo opcional, número de habitación, fecha de entrada y fecha de salida, agrupados en `NuevaReservaDTO`. | `ReservaResumenDTO` con identificación de la reserva, cliente, habitación, fechas, total calculado y estado. Ante datos inválidos o falta de disponibilidad se generan excepciones. | Disponible en consola y mediante el controlador. | Exige habitación disponible en el inventario. El DAO JDBC comprueba solapamientos y guarda cliente y reserva dentro de una transacción. No recibe servicios adicionales ni un cliente existente. |
| Cancelación de reservas — HU-006 | Identificador de reserva: `String`. | `boolean`: `true` si se cancela una reserva activa; `false` si no existe o no está activa. La consola muestra la confirmación correspondiente. | Disponible en consola. | Conserva la reserva con estado `CANCELADA` y cambia la habitación a `DISPONIBLE`. Las actualizaciones de reserva y habitación se realizan en operaciones de persistencia separadas. |
| Gestión de habitaciones — HU-008 | Para el alta, un objeto `HabitacionSencilla`, `HabitacionDoble` o `Suite`, construido con número y precio base. | `boolean` de `agregarHabitacion`: indica si se incorporó al inventario; pueden producirse excepciones de validación o persistencia. | Parcial: alta implementada en `GestorHabitaciones`. | El menú actual no ofrece gestión de habitaciones. No existe un flujo completo de edición y eliminación en dicho servicio, aunque el DAO dispone de operaciones CRUD. |
| Servicios adicionales — HU-009 | Objeto `ServicioAdicional` con nombre y costo, agregado a una instancia de `Reserva`. | `agregarServicio` retorna `void`; el servicio queda en la colección y participa en el siguiente cálculo del total. | Parcial: implementado en el modelo Java. | No existe una opción de consola ni una operación específica del controlador. El esquema y el DAO JDBC no almacenan los servicios. |
| Estado de habitaciones — HU-014 | Número de habitación: `int`; nuevo estado: `String`. | `boolean` de `cambiarEstado`; un estado inválido produce `IllegalArgumentException`. | Implementado en el servicio y utilizado en creación y cancelación de reservas. | No existe una opción independiente en el menú. Los estados admitidos son `DISPONIBLE`, `OCUPADA` y `MANTENIMIENTO`. |
| Pago y comprobante — HU-015 | Objeto `Reserva` y forma de pago para construir una `Factura`; objeto `Factura` para encolarlo. | Factura inicialmente `PENDIENTE`. Al procesarla, se marca `PROCESADA` y se intenta guardar mediante el DAO configurado. `encolarFactura` retorna `void`. | Parcial: modelo, procesamiento en segundo plano y persistencia JDBC implementados; `Main` configura `FacturaJdbcDAO`. | El menú continúa sin crear ni encolar facturas. La reserva debe existir en SQL y solo admite una factura por la restricción `UNIQUE`. No se verifica un pago bancario ni se genera PDF. Ante un error de persistencia, el procesador informa el fallo; el objeto ya pudo quedar marcado como `PROCESADA` sin haberse guardado. |
| Retención temporal — HU-016 | La documentación anterior propone habitación y usuario solicitante. | Se propone una retención con vencimiento y liberación automática. | Pendiente. | No se identificó una implementación de retención temporal. Los bloqueos transaccionales JDBC no equivalen a una retención de varios minutos. |
| Listado de reservas activas — relacionado con HU-004 y HU-006 | Solicitud de listado, sin parámetros. | `List<ReservaResumenDTO>` de reservas en estado `ACTIVA`. | Disponible en consola. | No corresponde a un historial completo por cliente ni incluye reservas canceladas o finalizadas. |
| Procesamiento de solicitudes por lote — relacionado con HU-004 | `List<NuevaReservaDTO>`. | `List<ResultadoSolicitudDTO>`, conservando el orden de las solicitudes. | Implementado en `ProcesadorSolicitudesReserva` y utilizado en la demostración de concurrencia. | Cada resultado contiene éxito y resumen, o motivo de rechazo. No implementa la retención temporal de HU-016. |

### 10.2. Estructuras de transferencia de datos

| Estructura | Función | Atributos y tipos Java |
|---|---|---|
| `NuevaReservaDTO` | Agrupar los datos de entrada para crear una reserva y su cliente. | `nombreCliente: String`, `telefonoCliente: String`, `emailCliente: String`, `numeroHabitacion: int`, `fechaEntrada: java.util.Date`, `fechaSalida: java.util.Date`. |
| `ReservaResumenDTO` | Transportar los datos de salida de una reserva. | `idReserva: String`, `nombreCliente: String`, `numeroHabitacion: int`, `tipoHabitacion: String`, `fechaEntrada: java.util.Date`, `fechaSalida: java.util.Date`, `total: double`, `estado: String`. |
| `ResultadoSolicitudDTO` | Representar el resultado individual de una solicitud procesada por lote. | `solicitud: NuevaReservaDTO`, `exitosa: boolean`, `reserva: ReservaResumenDTO`, `motivoRechazo: String`. Las factorías actuales establecen resumen en caso de éxito y motivo en caso de rechazo. |

Los DTO no son entidades persistentes y no tienen tablas propias. En `ReservaResumenDTO`, `tipoHabitacion` contiene actualmente el nombre de la clase Java, por ejemplo `HabitacionDoble`; en SQL, su representación es `DOBLE`.

### 10.3. Validaciones y límites del flujo actual

La consola solicita las fechas con formato `dd/MM/yyyy`. El servicio comprueba que existan ambas fechas, que la salida sea posterior a la entrada, que la estadía comprenda al menos una noche, que la entrada no corresponda al pasado y que los años estén entre 2024 y 2100.

En la creación de clientes se exige un nombre de 3 a 50 caracteres compuesto por letras y espacios, y un teléfono de ocho dígitos. El correo admite ausencia de valor; cuando contiene texto, se valida su formato. Estas reglas pertenecen al código Java y no están reproducidas íntegramente como restricciones SQL.

El método `buscarDisponibilidad(fechaEntrada, fechaSalida)` existe en `GestorReservas`, pero actualmente no utiliza las fechas recibidas y recorre las habitaciones asociadas a reservas. Por ello, no debe presentarse como una consulta completa de disponibilidad por periodo.

Las operaciones pendientes mencionadas en la documentación anterior, como modificación de reservas, check-in/check-out, autenticación, gestión de usuarios, reportes e historial por cliente, no se consideran completadas por la existencia de clases o métodos DAO relacionados. Las operaciones descritas en esta sección corresponden al flujo Java de consola. La interfaz React incorporada en `FrontPrivada/suiteflow-app` utiliza datos de ejemplo y estado local; no acredita integración con los controladores Java ni persistencia de sus operaciones.

## 11. Declaración de entidades del sistema — actualización para el Avance 3

Las entidades implementadas se encuentran en el paquete `com.sv.grupo5.suiteflow.model`. El archivo `src/main/resources/schema.sql` define cuatro tablas: `cliente`, `habitacion`, `reserva` y `factura`.

Las cuatro tablas tienen implementaciones JDBC disponibles. `FacturaJdbcDAO` permite guardar, consultar, actualizar y eliminar facturas, y está configurado en el procesador creado por `Main`. La integración del proceso de facturación con la interfaz de usuario continúa pendiente.

En las tablas siguientes, PK significa clave primaria y FK, clave foránea. Salvo donde se indique que se admite `NULL`, las columnas SQL son obligatorias.

### 11.1. Entidades con correspondencia SQL

| Entidad | Atributo Java | Tipo Java | Correspondencia SQL | Identificación o relación |
|---|---|---|---|---|
| `Cliente` | `id` | `String` | `cliente.id VARCHAR(36)` | PK. La creación genera un UUID; el constructor de reconstrucción conserva un ID recibido. |
| `Cliente` | `nombre` | `String` | `cliente.nombre VARCHAR(100)` | Nombre del cliente. |
| `Cliente` | `telefono` | `String` | `cliente.telefono VARCHAR(20)` | Teléfono de contacto. |
| `Cliente` | `email` | `String` | `cliente.email VARCHAR(100)`, admite `NULL` | Correo opcional. |
| `Habitacion` | `numero` | `int` | `habitacion.numero INT` | PK e identificación de la habitación. |
| `Habitacion` | `estado` | `String` | `habitacion.estado VARCHAR(15)` | Valores SQL: `DISPONIBLE`, `OCUPADA`, `MANTENIMIENTO`. Predeterminado: `DISPONIBLE`. |
| `Habitacion` | `precioBase` | `double` | `habitacion.precio_base DECIMAL(10,2)` | SQL exige valor mayor o igual a cero. |
| `Habitacion` | Tipo representado por la subclase | Sin atributo independiente `tipo` | `habitacion.tipo VARCHAR(10)` | Discriminador: `SENCILLA`, `DOBLE` o `SUITE`. |
| `Reserva` | `id` | `String` | `reserva.id VARCHAR(36)` | PK. UUID generado al crear la reserva; se conserva al reconstruirla. |
| `Reserva` | `cliente` | `Cliente` | `reserva.cliente_id VARCHAR(36)` | FK hacia `cliente.id`. |
| `Reserva` | `numeroHabitacion` | `int` | `reserva.numero_habitacion INT` | FK hacia `habitacion.numero`. |
| `Reserva` | `habitacion` | `Habitacion`, declarado `transient` | Sin columna adicional; utiliza `numero_habitacion` | Referencia que el gestor reenlaza con el inventario después de cargar. |
| `Reserva` | `fechaEntrada` | `java.util.Date` | `reserva.fecha_entrada DATE` | Inicio de la estadía. |
| `Reserva` | `fechaSalida` | `java.util.Date` | `reserva.fecha_salida DATE` | SQL exige salida posterior a entrada. |
| `Reserva` | `estado` | `String` | `reserva.estado VARCHAR(12)` | Valores SQL: `ACTIVA`, `CANCELADA`, `FINALIZADA`. Predeterminado: `ACTIVA`. |
| `Reserva` | `servicios` | `List<ServicioAdicional>` | Sin columna ni tabla relacionada | Colección existente únicamente en el modelo Java. |
| `Factura` | `id` | `String` | `factura.id VARCHAR(36)` | PK. UUID generado al crearla; se conserva al reconstruirla. |
| `Factura` | `reserva` | `Reserva` | `factura.reserva_id VARCHAR(36)` | FK hacia `reserva.id`; restricción `UNIQUE`. |
| `Factura` | `total` | `double` | `factura.total DECIMAL(10,2)` | El constructor de creación toma el resultado de `reserva.calcularTotal()`; la reconstrucción conserva el valor almacenado. |
| `Factura` | `formaPago` | `String` | `factura.forma_pago VARCHAR(20)`, admite `NULL` | Texto que identifica el medio de pago. |
| `Factura` | `estado` | `String` | `factura.estado VARCHAR(12)` | Valores SQL: `PENDIENTE`, `PROCESADA`. Predeterminado: `PENDIENTE`. |

Las constantes de estado y `serialVersionUID` son elementos de implementación, no atributos adicionales del modelo relacional.

`Factura` dispone de un constructor adicional para reconstruir registros desde SQL, conservando el identificador, la reserva, el total almacenado, la forma de pago y el estado. Este constructor comprueba que el ID no esté vacío, que exista una referencia de reserva y que el estado sea `PENDIENTE` o `PROCESADA`. A diferencia del constructor de creación, no recalcula el total.

### 11.2. Especialización de habitaciones

`Habitacion` es una clase abstracta que implementa `Reservable` y `Serializable`. Sus especializaciones comparten los atributos número, estado y precio base.

| Clase Java | Valor de `habitacion.tipo` | Cálculo de precio implementado |
|---|---|---|
| `HabitacionSencilla` | `SENCILLA` | `precioBase × factorDemanda`. |
| `HabitacionDoble` | `DOBLE` | `precioBase × factorDemanda × 1.20`. |
| `Suite` | `SUITE` | `precioBase × factorDemanda × 1.50`. |

`HabitacionJdbcDAO` convierte cada subclase al valor SQL correspondiente y reconstruye la subclase al leer una fila. No existen tablas independientes para estas especializaciones.

`Reservable` define el contrato `calcularPrecio(double factorDemanda)` y no constituye una entidad persistente. Aunque las habitaciones reciben un factor de demanda, `Reserva.calcularTotal()` utiliza actualmente el valor fijo `1.0`.

### 11.3. Entidades sin tabla SQL

| Entidad | Descripción | Atributos y tipos Java | Relación y alcance actual |
|---|---|---|---|
| `ServicioAdicional` | Prestación agregada a una reserva. | `nombre: String`, `costo: double`. | Una reserva contiene una lista de servicios. Su costo se suma al total. No tiene identificador propio, tabla ni DAO específico. |
| `Empleado` | Persona que opera el sistema. | `id: String`, `nombre: String`, `rol: String`. | El ID se genera mediante UUID. El rol es texto; no existe relación SQL ni integración de autenticación y permisos. |

Ambas clases implementan `Serializable`. Esto permite su serialización, pero no demuestra que exista un flujo completo de almacenamiento: `ServicioAdicional` puede viajar dentro de una reserva guardada en archivos; para `Empleado` no se identificó un DAO específico.

### 11.4. Relaciones y persistencia efectiva

| Relación o entidad | Representación actual |
|---|---|
| Cliente–Reserva | Un cliente puede estar relacionado con varias reservas mediante `reserva.cliente_id`. Cada reserva referencia un cliente. El controlador actual crea un cliente nuevo por solicitud. |
| Habitación–Reserva | Una habitación puede aparecer en varias reservas mediante `numero_habitacion`. Cada reserva referencia una habitación. La comprobación de solapamientos se implementa en el DAO; la FK por sí sola no la garantiza. |
| Reserva–Factura | SQL permite como máximo una factura por reserva mediante `UNIQUE(reserva_id)`. La existencia de una reserva no obliga a crear una factura. |
| Reserva–ServicioAdicional | Relación mediante una colección Java; no está representada en SQL. |
| Habitaciones | `Main` utiliza `HabitacionJdbcDAO`. Existe también una alternativa basada en archivos. |
| Reservas y clientes asociados | `Main` utiliza `ReservaJdbcDAO`, que inserta cliente y reserva. Los servicios adicionales no se guardan ni se recuperan por este DAO. |
| Facturas | `Main` configura `ProcesadorFacturas` con `FacturaJdbcDAO`. Al consultar, el DAO reconstruye factura, reserva y cliente mediante uniones SQL. La habitación de la reserva queda sin enlazar y sus servicios no se recuperan; el total de la factura conserva el valor almacenado. `FacturaArchivoDAO` permanece como alternativa. |
| Clientes independientes | Existen `ClienteJdbcDAO` y `ClienteArchivoDAO`. El primero implementa CRUD sobre `cliente`, pero no está conectado a una opción independiente del menú. La creación de reservas sigue guardando al cliente mediante `ReservaJdbcDAO`. |
| Empleados | Clase Java sin tabla ni DAO específico identificado. |

### 11.5. Conceptos propuestos pendientes

La documentación anterior incluye los siguientes conceptos. Sus atributos son propuestas documentales, no campos de entidades independientes implementadas.

| Concepto | Atributos propuestos anteriormente | Situación actual |
|---|---|---|
| Rol | ID, nombre. | No existe clase ni tabla de roles. `Empleado.rol` es `String`. |
| Tipo de habitación | ID, nombre, capacidad, precio base. | No existe catálogo independiente. El tipo se representa mediante subclases y la columna `habitacion.tipo`. La capacidad no aparece como atributo del modelo actual. |
| Forma de pago | ID, nombre. | No existe clase ni tabla de catálogo. Se representa mediante `Factura.formaPago` y `factura.forma_pago`. |
| Cargo de reserva | ID, concepto, porcentaje. | No existe clase, tabla ni estructura específica de cargos. Los multiplicadores de precio de las habitaciones no constituyen esta entidad. |

### 11.6. Discrepancias que requieren decisión del equipo

| Discrepancia comprobada | Decisión pendiente |
|---|---|
| Los importes Java utilizan `double`, mientras SQL utiliza `DECIMAL(10,2)`. | Definir una política común de precisión, redondeo y límites monetarios. |
| Las fechas Java utilizan `java.util.Date`, mientras SQL utiliza `DATE`. | Unificar el tratamiento de días de estadía, horas y zona horaria. |
| Las validaciones de clientes son más restrictivas al crear que al reconstruir; tampoco reproducen todas las longitudes SQL. | Acordar reglas comunes de creación, recuperación y almacenamiento. |
| El constructor de reconstrucción de `Factura` valida el estado, pero `setEstado()` sigue aceptando valores arbitrarios y no se valida íntegramente la longitud de la forma de pago. | Unificar las validaciones del modelo con las restricciones SQL. |
| Los servicios adicionales no persisten mediante JDBC. | Definir su almacenamiento y recuperación sin presentar esta funcionalidad como completada. |
| Facturación ya dispone de JDBC y está configurada en `Main`, pero el menú no crea ni encola facturas. | Completar la integración del flujo de facturación en la tarea correspondiente. La unicidad por reserva se aplica en SQL; la alternativa de archivos continúa sin garantizarla. |
| El procesador cambia el estado a `PROCESADA` antes de guardar; si falla la persistencia, informa el error sin revertir el estado del objeto. | Definir cómo representar y recuperar un procesamiento fallido. |
| La consulta de habitaciones muestra solo números y la disponibilidad por fechas está incompleta. | Acordar el contrato de consulta y la relación entre estado global y disponibilidad por periodo. |
| La documentación anterior afirma que no existe conexión a una base de datos. | Reconocer que esa afirmación corresponde al Avance 2. Para el Avance 3, describir JDBC y archivos según la integración actual. |

### 11.7. Procedencia y alcance de la información

| Fuente | Información utilizada |
|---|---|
| Clases de `model/` | Entidades existentes, atributos, tipos Java, herencia, estados y cálculos. |
| Clases de `dto/` | Estructuras reales de entrada y salida. |
| `ReservaController`, gestores, procesadores y `VistaReservas` | Parámetros, retornos, operaciones accesibles y limitaciones de integración. |
| `Main`, DAO JDBC y DAO de archivos | Persistencia configurada y alternativas disponibles. |
| `ClienteJdbcDAO`, `FacturaJdbcDAO`, `Main`, `Factura` y `ProcesadorFacturas` | Nuevas operaciones JDBC, configuración de facturación, reconstrucción de facturas y manejo de errores de almacenamiento incorporados al estado revisado del Avance 3. |
| `src/main/resources/schema.sql` | Tablas, columnas, tipos SQL, nulabilidad, PK, FK y restricciones. |
| `database/schema.sql` | Copia incorporada del esquema, actualmente idéntica a `src/main/resources/schema.sql`. El inicializador continúa leyendo el recurso del classpath. |
| `FrontPrivada/suiteflow-app` | Interfaz React incorporada con datos de ejemplo y estado local. No acredita conexión con los controladores Java ni persistencia de sus operaciones; las operaciones de la sección 10 describen el flujo Java de consola. |
| Secciones anteriores del Avance 2 | Identificadores de historias de usuario, alcance previsto y conceptos propuestos; no se utilizan como prueba de implementación. |

La descripción corresponde a una revisión estática de archivos locales al commit `59fd1d9`. No acredita resultados de ejecución ni pruebas contra una base de datos.

