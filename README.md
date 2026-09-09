# Sistema de Gestión Hotelera

**Backend de gestión integral para hoteles**, implementado en **Java 17+** con arquitectura por capas, persistencia en archivos, y programación concurrente para operaciones paralelas seguras.

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=java)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-blue?logo=apache-maven)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 📋 Descripción

Sistema backend completo para la administración de un hotel, incluyendo:

- ✅ **Gestión de habitaciones** — registrar, actualizar estado, consultar disponibilidad
- ✅ **Gestión de reservas** — crear, cancelar, búsqueda por rango de fechas
- ✅ **Gestión de clientes** — datos de contacto, historial de reservas
- ✅ **Generación de facturas** — cálculo automático, procesamiento en segundo plano
- ✅ **Cálculo dinámico de tarifas** — factor de demanda basado en ocupación
- ✅ **Persistencia de datos** — archivos .dat con serialización de objetos
- ✅ **Concurrencia sincronizada** — evita conflictos de doble reserva, procesamiento paralelo de facturas

---

## 🏗️ Arquitectura

Implementación de **arquitectura por capas** separada en responsabilidades:

```
┌─────────────────────────────────────┐
│       Presentación (UI)             │  ← Main.java, Frontend (futuro)
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Capa de Servicio (Lógica)         │  ← GestorReservas, GestorHabitaciones
│                                     │     ProcesadorFacturas, CalculadorTarifas
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Capa de Acceso a Datos (DAO)      │  ← ClienteDAO, HabitacionDAO
│                                     │     ReservaDAO, FacturaDAO
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Capa de Modelo               │  ← Cliente, Habitacion, Reserva
│                                     │     Factura, ServicioAdicional
└─────────────────────────────────────┘
               │
        ┌──────▼──────┐
        │  datos/*.dat │  ← Persistencia
        └─────────────┘
```

### Estructura de paquetes

```
src/main/java/com/hotel/
├── modelo/
│   ├── Cliente.java                 [Serializable]
│   ├── Habitacion.java              [Abstract, implements Reservable]
│   ├── HabitacionSencilla.java      [extends Habitacion]
│   ├── HabitacionDoble.java         [extends Habitacion]
│   ├── Suite.java                   [extends Habitacion]
│   ├── Reserva.java                 [Serializable]
│   ├── Empleado.java                [Serializable]
│   ├── Factura.java                 [Serializable, con estado PENDIENTE/PROCESADA]
│   ├── ServicioAdicional.java       [Serializable]
│   └── Reservable.java              [Interface]
│
├── servicio/
│   ├── GestorReservas.java          [synchronized: crearReserva(), cancelarReserva()]
│   ├── GestorHabitaciones.java      [synchronized: cambiarEstado()]
│   ├── CalculadorTarifas.java       [factor de demanda dinámico]
│   └── ProcesadorFacturas.java      [extends Thread, cola sincronizada]
│
├── dao/
│   ├── ClienteDAO.java              [persistencia en clientes.dat]
│   ├── HabitacionDAO.java           [persistencia en habitaciones.dat]
│   ├── ReservaDAO.java              [persistencia en reservas.dat]
│   └── FacturaDAO.java              [persistencia en facturas.dat]
│
├── excepciones/
│   └── HabitacionNoDisponibleException.java
│
└── Main.java                        [Punto de entrada, demos funcionales]
```

---

## 🚀 Requisitos Previos

- **Java Development Kit (JDK) 17+**
- **Apache Maven 3.8+**
- **Git** (para control de versiones)

**Verificar instalación:**

```bash
java -version      # debe mostrar openjdk 17 o superior
mvn -version       # debe mostrar Maven 3.8+
```

---

## 📦 Instalación y Compilación

### 1. Clonar el repositorio

```bash
git clone https://github.com/Franklin-Martin-Fuentes/Sistema-De-Gestion-Hotelera.git
cd Sistema-De-Gestion-Hotelera
```

### 2. Compilar el proyecto

```bash
mvn clean compile
```

Esto descargará todas las dependencias y compilará el código fuente.

### 3. Ejecutar las pruebas (opcional)

```bash
mvn test
```

---

## ▶️ Ejecución

### Opción A: Ejecutar con Maven

```bash
mvn exec:java -Dexec.mainClass="com.hotel.Main"
```

### Opción B: Compilar a JAR y ejecutar

```bash
mvn clean package -DskipTests
java -jar target/sistema-gestion-hotelera-1.0-SNAPSHOT.jar
```

---

## 🎯 Características Principales

### 1. Persistencia en Archivos .dat

Los datos se guardan automáticamente en la carpeta `datos/`:

- `clientes.dat` — lista de clientes registrados
- `habitaciones.dat` — inventario de habitaciones
- `reservas.dat` — historial de reservas
- `facturas.dat` — facturas procesadas

**Ventaja:** Los datos persisten entre ejecuciones. Ejecuta el programa dos veces y la segunda vez cargará los datos de la primera.

### 2. Programación Concurrente

#### Sincronización en Operaciones Críticas

Los métodos `crearReserva()` y `cancelarReserva()` son **synchronized** para evitar condiciones de carrera:

```java
public synchronized Reserva crearReserva(Cliente cliente, Habitacion habitacion, ...) 
    throws HabitacionNoDisponibleException { ... }
```

**Caso de uso real:** Si dos clientes intentan reservar la MISMA habitación al mismo tiempo (uno desde el mostrador, otro por web), solo uno logrará reservarla. El otro recibirá `HabitacionNoDisponibleException`.

#### Hilo: ProcesadorFacturas

Un Thread dedicado procesa facturas de forma asíncrona:

```java
ProcesadorFacturas procesador = new ProcesadorFacturas();
procesador.start();
procesador.encolarFactura(nuevaFactura);  // No bloquea
```

**Justificación:** En un hotel real, generar una factura implica armar documento PDF, enviar correo, actualizar registros de impuestos, etc. Esto no debe detener el flujo principal. Por eso se procesa en un hilo separado con una cola sincronizada.

### 3. Cálculo Dinámico de Tarifas

Las habitaciones tienen precios que varían según demanda:

```java
// Base: $100
// Factor demanda: 1.0x (0-30% ocupación) → $100
//               1.2x (31-60% ocupación) → $120
//               1.5x (61-90% ocupación) → $150
//               2.0x (91-100% ocupación) → $200
```

Cada tipo de habitación tiene su multiplicador:
- **Sencilla:** `precioBase × factorDemanda`
- **Doble:** `(precioBase × 1.20) × factorDemanda`
- **Suite:** `(precioBase × 1.50) × factorDemanda`

---

## 📝 Ejemplo de Uso

```java
// 1. Crear gestores (cargan datos previos automáticamente)
GestorHabitaciones gestorHab = new GestorHabitaciones();
GestorReservas gestorRes = new GestorReservas();

// 2. Crear modelos
Cliente cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
Habitacion hab = new HabitacionDoble(102, 150.0);
gestorHab.agregarHabitacion(hab);  // Se persiste en habitaciones.dat

// 3. Crear reserva (método sincronizado)
try {
    Reserva reserva = gestorRes.crearReserva(cliente, hab, new Date(), new Date());
    
    // 4. Agregar servicios adicionales
    ServicioAdicional desayuno = new ServicioAdicional("Desayuno", 15.0);
    reserva.agregarServicio(desayuno);
    
    // 5. Generar factura y encolarla para procesamiento asíncrono
    Factura factura = new Factura(reserva, "TARJETA");
    procesador.encolarFactura(factura);  // Se procesa en background
    
} catch (HabitacionNoDisponibleException e) {
    System.out.println("Habitación no disponible: " + e.getMessage());
}
```

---

## 🧪 Testing

Ejecuta el programa principal para ver demostraciones en vivo:

```bash
mvn exec:java -Dexec.mainClass="com.hotel.Main"
```

**Lo que demuestra:**

1. **Carga de datos persistidos** desde `datos/*.dat`
2. **Primera ejecución:** crea 3 habitaciones iniciales y las guarda
3. **Creación de reserva normal** con cliente real
4. **Encola una factura** para procesamiento paralelo (sin bloquear)
5. **Prueba de concurrencia:** 2 hilos (`Hilo-ClienteA`, `Hilo-ClienteB`) intentan reservar la misma habitación
   - Solo 1 debe lograr la reserva
   - El otro debe recibir `HabitacionNoDisponibleException`
6. **Factor de demanda calculado** en tiempo real
7. **Resumen final** con estadísticas del sistema

---

## 🔄 Flujo de Trabajo Git

El proyecto usa **Git Flow** simplificado para el trabajo en equipo:

```
main (entrega final)
  └─ develop (integración)
      ├─ feature/persistencia-dao (Backend)
      ├─ feature/interfaz-usuario (Frontend)
      ├─ feature/base-datos (DBA)
      └─ feature/qa-tests (QA)
```

**Comandos básicos:**

```bash
# Crear una rama de trabajo
git checkout -b feature/tu-funcionalidad

# Hacer cambios y commitear
git add .
git commit -m "Descripción clara del cambio"

# Subir tu rama
git push origin feature/tu-funcionalidad

# Crear un Pull Request en GitHub para revisión
```

---

## 👥 Equipo

| Nombre | Rol | GitHub |
|--------|-----|--------|
| Franklin Martin Fuentes Chavez | Desarrollador Backend | [@Frank54-0](https://github.com/Frank54-0) |
| Christian Alessandro Marin | Desarrollador Frontend | TBD |
| Salvador Ernesto Rodriguez | Base de Datos | TBD |
| Gabriel Alejandro Fuentes | QA / Testing | TBD |
| ... | ... | ... |

---

## 📚 Documentación Adicional

- **Diagrama UML:** Ver `Diccionario_de_Clases.docx` en la raíz
- **Guía Backend:** Ver `backend_guia.pdf`
- **Especificación de Avance 2:** Ver `AVANCE2_ENTREGA.pdf`

---

## 🛠️ Troubleshooting

### Error: "No se encuentra com.hotel.Main"
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.hotel.Main"
```

### Error: "Java version mismatch"
Verifica que tu `JAVA_HOME` apunte a JDK 17+:
```bash
echo $JAVA_HOME
java -version
```

### Error: "datos/ no existe"
Los archivos DAO crean la carpeta automáticamente. Si persiste, créala manualmente:
```bash
mkdir datos
```

---

## 📄 Licencia

Proyecto de código abierto bajo licencia **MIT**. Libre para usar, modificar y distribuir.

---

## 📬 Contacto

- **Franklin Martin Fuentes:** franklin.martinfuenteschavez@gmail.com
- **Repositorio:** [GitHub](https://github.com/Frank54-0/Sistema-De-Gestion-Hotelera)
- **Universidad:** Evangélica de El Salvador - Facultad de Ingeniería

---

**Última actualización:** 8 de septiembre de 2026  
**Estado:** Avance 2 en desarrollo (persistencia + concurrencia ✅)
