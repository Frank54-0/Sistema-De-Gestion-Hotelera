# SuiteFlow – Sistema de Gestión Hotelera

Proyecto del curso Programación II (UEES), Grupo 5. Sistema para gestionar habitaciones, reservas y facturas de un hotel, con persistencia en MySQL/MariaDB mediante JDBC y reservas concurrentes seguras.

## Integrantes

Nombre - Rol 
Jireh Alexander Trigueros Pérez - Product Owner 
Carla Vanesa Franco Avelar - Scrum Master 
Jesús Roberto Sánchez Orellana - Architect 
Franklin Martin Fuentes Chávez - Backend 
Alessandro Imanol Ramírez Moran - Full stack 
Gabriela Michelle Pérez Portillo - Full stack 
Christian Alessandro Marín Sandoval - Frontend 
Salvador Ernesto Rodríguez Galdamez - Base de datos 
Gabriel Alejandro Fuentes Chavarría - QA 
Emerson Eduardo Candido Joya - QA 
Cindy Shaiel Canales Campos - Business Analyst 
Julio Cesar Pérez Salazar - Business Analyst

## Tecnologías

- Java 17 y Maven
- JDBC puro (sin ORM ni frameworks) con pool de conexiones Tomcat JDBC
- MySQL / MariaDB (probado con MariaDB 10.11.16 de XAMPP) y driver `mysql-connector-j`
- H2 en memoria (modo MySQL), solo para las pruebas
- JUnit 5 y Mockito

## Requisitos

- JDK 17 o superior
- Maven 3.8 o superior
- MySQL o MariaDB encendido (por ejemplo XAMPP) para ejecutar la aplicación. Las pruebas no lo necesitan

## Configuración de la base de datos

1. Encender MySQL o MariaDB (puerto 3306)
2. No hay que crear nada a mano: al arrancar, la aplicación crea la base `suiteflow` y ejecuta `schema.sql` y `data.sql`. Se puede arrancar varias veces sin duplicar datos. Los mismos scripts están en la carpeta `database/`
3. La conexión se configura en `src/main/resources/db.properties` (URL, usuario y contraseña). También se puede sobrescribir con las variables de entorno `SUITEFLOW_DB_URL`, `SUITEFLOW_DB_USUARIO` y `SUITEFLOW_DB_CONTRASENA`

## Cómo ejecutar

1. Tener MySQL o MariaDB encendido
2. Clonar el repositorio:

```bash
git clone https://github.com/Frank54-0/Sistema-De-Gestion-Hotelera.git
```

3. Abrir el proyecto en IntelliJ IDEA y ejecutar la clase `com.sv.grupo5.suiteflow.Main`
4. También se puede desde la terminal:

```bash
mvn exec:java
```

5. Demo de concurrencia (8 solicitudes simultáneas a la misma habitación):

```bash
mvn exec:java "-Dexec.args=--demo-concurrencia"
```

## Cómo ejecutar las pruebas

- Desde la terminal: `mvn test`
- En IntelliJ: clic derecho sobre la carpeta `src/test` y elegir Run Tests

Son 263 pruebas con JUnit 5 y Mockito. Los DAO JDBC se prueban contra H2 en memoria, así que no hace falta tener MySQL encendido. Están en `src/test/java/com/sv/grupo5/suiteflow` (paquetes `controller`, `dao`, `model`, `service` y `view`).

## Usuario y credenciales de prueba

- Base de datos: usuario `root` sin contraseña (configuración por defecto de XAMPP)
- La aplicación no tiene inicio de sesión
- Las habitaciones de prueba (101, 102, 103, 104, 201 y 202) se cargan desde `data.sql`

## Programación concurrente

- **Reservas simultáneas:** varios clientes pueden intentar reservar la misma habitación al mismo tiempo. Hay dos barreras. `GestorReservas.crearReserva()` está sincronizado, así que dentro del mismo programa los hilos pasan de uno en uno. Además, `ReservaJdbcDAO` hace la reserva en una transacción que bloquea la fila de la habitación con `SELECT ... FOR UPDATE`, revisa que no haya reservas con fechas que se crucen y recién entonces guarda, lo que protege incluso entre programas distintos. Las solicitudes en lote se atienden con un `ExecutorService` de 8 hilos (`ProcesadorSolicitudesReserva`) y cada hilo usa su propia conexión del pool. El que pierde recibe `HabitacionNoDisponibleException`.
- **Procesamiento de facturas:** `ProcesadorFacturas` es un hilo dedicado con una cola. Procesa las facturas en segundo plano para que el recepcionista no quede bloqueado en cada cobro, y las guarda mediante `FacturaDAO` (por ahora en archivo).

## Estructura del proyecto

```
src/main/java/com/sv/grupo5/suiteflow
├── config       Pool de conexiones (DataSource) e inicialización de la base
├── controller   Controladores (reciben y devuelven DTO)
├── service      Lógica de negocio (gestores y procesadores)
├── dao          Acceso a datos (interfaces e implementaciones)
├── dto          Objetos de transferencia de datos
├── model        Entidades del dominio
├── exception    Excepciones propias
├── view         Menú de consola
└── Main.java

src/main/resources   db.properties, schema.sql y data.sql

src/test/java/com/sv/grupo5/suiteflow
├── controller   Pruebas de los controladores
├── dao          Pruebas de la capa de datos
├── model        Pruebas de las entidades
├── service      Pruebas de la lógica de negocio
└── view         Pruebas de la vista

database/        schema.sql y data.sql
docs/            Documento final y diagramas
pom.xml          Dependencias y plugins de Maven
```

## Documentación

El documento final y los diagramas están en la carpeta `docs/`.

## Repositorio

https://github.com/Frank54-0/Sistema-De-Gestion-Hotelera
