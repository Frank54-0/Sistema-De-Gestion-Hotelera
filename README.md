# Sistema de Gestión Hotelera

## Tecnologías

- Java 17+
- Maven
- JUnit 5

## Estructura del proyecto
src/main/java/com/hotel/
├── modelo/ ← Cliente, Habitacion, Reserva, Factura, ServicioAdicional, Empleado
├── servicio/ ← GestorReservas, GestorHabitaciones, CalculadorTarifas, ProcesadorFacturas
├── dao/ ← Persistencia en archivos .dat
├── excepciones/ ← Excepciones personalizadas
└── Main.java ← Punto de entrada

## Cómo compilar y ejecutar

```bash
mvn clean compile
mvn exec:java
```

## Cómo correr las pruebas

```bash
mvn test
```

## Notas

- Los datos se guardan automáticamente en la carpeta `datos/` (archivos .dat), y se cargan de nuevo cada vez que se inicia el programa.
- `crearReserva()` y `cancelarReserva()` están sincronizados para evitar que dos usuarios reserven la misma habitación al mismo tiempo.
- `ProcesadorFacturas` procesa las facturas en un hilo separado, en segundo plano.

## Equipo

Backend: Franklin Martin Fuentes Chavez