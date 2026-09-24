# Avance 2 – Secciones del Business Analyst

> Borrador para revisar con el Product Owner y el Arquitecto antes de consolidar en el documento final.
> La columna "Avance 2" indica qué historias se proponen dentro de este avance (según lo que ya existe en el código) y cuáles quedan para avances posteriores.

## 6. Requerimientos del sistema (Historias de usuario)

| Prioridad | Historia HU | Criterios de aceptación | Avance 2 |
|---|---|---|---|
| ALTA | **HU-001. Inicio de sesión.** Como empleado (administrador o recepcionista) quiero iniciar sesión con mis credenciales para acceder solo a las funciones de mi rol. | 1) Solo se accede con credenciales válidas. 2) Si son inválidas se muestra un mensaje de error. 3) Las opciones disponibles dependen del rol. | Posterior |
| ALTA | **HU-002. Registro de clientes.** Como recepcionista quiero registrar los datos de un cliente para asociarlo a sus reservas. | 1) El cliente queda guardado con nombre, teléfono y correo. 2) No se permite registrar sin nombre o sin teléfono. 3) Cada cliente recibe un identificador único. | Incluida |
| ALTA | **HU-003. Consulta de habitaciones.** Como recepcionista quiero consultar las habitaciones disponibles para ofrecer una opción al cliente. | 1) Se muestran solo las habitaciones con estado Disponible. 2) Cada habitación muestra número, tipo y precio. | Incluida |
| ALTA | **HU-004. Creación de reservas.** Como recepcionista quiero crear una reserva para un cliente en una habitación disponible. | 1) La reserva queda guardada con cliente, habitación y fechas. 2) La habitación cambia a Ocupada. 3) Se rechaza si la habitación no está disponible. 4) La fecha de salida debe ser posterior a la de entrada. 5) Dos usuarios no pueden reservar la misma habitación al mismo tiempo. | Incluida |
| MEDIA | **HU-005. Modificación de reservas.** Como recepcionista quiero modificar fechas, habitación o servicios de una reserva existente. | 1) Los cambios se guardan correctamente. 2) No se puede modificar una reserva cancelada. | Posterior |
| MEDIA | **HU-006. Cancelación de reservas.** Como recepcionista quiero cancelar una reserva para liberar la habitación. | 1) La reserva queda en estado Cancelada (no se elimina, para conservar el historial). 2) La habitación vuelve a Disponible. 3) No se puede cancelar una reserva que ya está cancelada. | Incluida |
| ALTA | **HU-007. Check-in / Check-out.** Como recepcionista quiero registrar la llegada y salida del huésped. | 1) El estado de la habitación cambia según la operación. 2) Queda registro del movimiento. | Posterior |
| ALTA | **HU-008. Gestión de habitaciones.** Como administrador quiero crear, editar y eliminar habitaciones para mantener actualizado el inventario. | 1) Se puede registrar una habitación sencilla, doble o suite. 2) No se permiten dos habitaciones con el mismo número. 3) El precio base no puede ser negativo. 4) Los cambios se guardan en el archivo de habitaciones. | Incluida |
| MEDIA | **HU-009. Gestión de servicios adicionales.** Como recepcionista quiero agregar servicios (desayuno, spa, etc.) a una reserva. | 1) El servicio se agrega a la reserva con su costo. 2) El total de la reserva incluye el costo de los servicios. | Incluida |
| BAJA | **HU-010. Generación de reportes.** Como administrador quiero generar reportes de ocupación y reservas. | 1) El reporte se genera sin errores. 2) Muestra la información del rango solicitado. | Posterior |
| ALTA | **HU-011. Gestión de usuarios.** Como administrador quiero crear, modificar, eliminar y asignar roles a los usuarios. | 1) Los cambios se guardan correctamente. 2) Cada usuario tiene un rol asignado. | Posterior |
| MEDIA | **HU-012. Búsqueda de clientes.** Como recepcionista quiero buscar clientes por nombre o documento. | 1) Se muestran los resultados coincidentes de forma correcta. | Posterior |
| MEDIA | **HU-013. Historial de reservas.** Como recepcionista quiero ver el historial de reservas de un cliente. | 1) Se muestran fechas, habitación y estado de cada reserva, incluidas las canceladas. | Posterior |
| ALTA | **HU-014. Estado de habitaciones.** Como administrador quiero cambiar el estado de una habitación entre Disponible, Ocupada y Mantenimiento. | 1) El cambio se refleja de inmediato. 2) Solo se aceptan esos tres estados. 3) Una habitación en Mantenimiento no puede reservarse. | Incluida |
| ALTA | **HU-015. Pago y comprobante *(nueva)*.** Como recepcionista quiero registrar el pago de una reserva y emitir su factura. | 1) La factura queda asociada a la reserva con el total calculado. 2) Se registra la forma de pago. 3) La factura se procesa en segundo plano y cambia de Pendiente a Procesada. 4) La factura queda guardada en archivo. | Incluida |
| MEDIA | **HU-016. Retención temporal de habitación *(nueva)*.** Como recepcionista quiero que una habitación quede retenida unos minutos mientras se completa la reserva, para que otro usuario no la tome. | 1) Mientras está retenida, otro usuario recibe el mensaje "temporalmente retenida". 2) Si no se confirma en el tiempo límite, la habitación se libera automáticamente. | Incluida (si el equipo la implementa) |

## 10. Entradas y salidas del sistema (historias incluidas en el Avance 2)

| Proceso (HU) | Entradas | Salidas |
|---|---|---|
| Registro de clientes (HU-002) | Nombre, teléfono, correo electrónico | Confirmación de registro con el identificador generado; error si faltan datos obligatorios |
| Consulta de habitaciones (HU-003) | Solicitud de consulta (opcionalmente tipo de habitación) | Listado de habitaciones disponibles con número, tipo y precio |
| Creación de reservas (HU-004) | Cliente, habitación seleccionada, fecha de entrada y fecha de salida | Reserva creada con su identificador y habitación en estado Ocupada; error si la habitación no está disponible o las fechas son inválidas |
| Cancelación de reservas (HU-006) | Identificador de la reserva | Confirmación de cancelación y habitación nuevamente Disponible |
| Gestión de habitaciones (HU-008) | Número, tipo (sencilla, doble o suite) y precio base | Habitación registrada y guardada; error si el número ya existe o el precio es inválido |
| Servicios adicionales (HU-009) | Reserva, nombre y costo del servicio | Servicio agregado y total de la reserva actualizado |
| Estado de habitaciones (HU-014) | Número de habitación y nuevo estado | Estado actualizado; error si el estado no es válido |
| Pago y comprobante (HU-015) | Reserva y forma de pago | Factura con el total, estado Pendiente y luego Procesada, guardada en archivo |
| Retención temporal (HU-016) | Habitación y usuario que inicia la reserva | Habitación retenida por el tiempo definido; liberación automática si no se confirma |

## 11. Declaración de entidades del sistema

Entidades implementadas actualmente:

| Entidad | Descripción | Atributos |
|---|---|---|
| Cliente | Persona que solicita el hospedaje | id, nombre, teléfono, email |
| Habitación *(abstracta)* | Espacio del hotel disponible para hospedaje; se especializa en Sencilla, Doble y Suite | número, estado (Disponible / Ocupada / Mantenimiento), precio base |
| Reserva | Solicitud de un cliente para hospedarse en una habitación durante un periodo | id, cliente, habitación, fecha de entrada, fecha de salida, estado (Activa / Cancelada / Finalizada), servicios adicionales |
| Servicio adicional | Prestación extra que se agrega a una reserva | nombre, costo |
| Factura | Comprobante del cobro generado por una reserva | id, reserva, total, forma de pago, estado (Pendiente / Procesada) |
| Empleado | Persona que opera el sistema en el hotel | id, nombre, rol |

Entidades del Avance 1 pendientes de definir con el Arquitecto (pasan a avances posteriores o se integran a las anteriores):

| Entidad | Descripción | Atributos propuestos |
|---|---|---|
| Rol | Función que determina los permisos de un empleado | id, nombre |
| Tipo de habitación | Categoría de habitaciones con precio de referencia | id, nombre, capacidad, precio base |
| Forma de pago | Medio con el que se cancela una factura | id, nombre |
| Cargo de reserva | Ajuste porcentual aplicado al valor de una reserva | id, concepto, porcentaje |

## 7. Alcances y limitaciones (ajustes para este avance)

Limitaciones a agregar o actualizar:

- El sistema se ejecuta por consola; no incluye interfaz web ni aplicación móvil en este avance.
- La información se guarda en archivos `.dat`; no se conecta a una base de datos en este avance.
- No incluye inicio de sesión ni gestión de usuarios y roles en este avance.
- El procesamiento de facturas es simulado y no integra pasarelas de pago reales.
- No incluye modificación de reservas, check-in / check-out ni reportes en este avance.
