package com.hotel;

import com.hotel.modelo.*;
import com.hotel.servicio.CalculadorTarifas;
import com.hotel.servicio.GestorHabitaciones;
import com.hotel.servicio.GestorReservas;
import com.hotel.servicio.ProcesadorFacturas;
import com.hotel.excepciones.HabitacionNoDisponibleException;

import java.util.Date;
import java.util.List;

/**
 * Clase principal para pruebas funcionales del Sistema de Gestión Hotelera — Avance 2.
 * Demuestra el funcionamiento de:
 * - Persistencia de datos en archivos .dat (carga automática al iniciar)
 * - Programación concurrente: ProcesadorFacturas como hilo en segundo plano
 * - Sincronización: dos hilos compitiendo por la misma habitación (crearReserva)
 * - Herencia, polimorfismo, encapsulamiento y colecciones (heredado de Avance 1)
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Sistema de Gestión Hotelera - Avance 2 ===\n");

        // 1. Gestores: cargan automáticamente datos previos desde datos/*.dat (si existen)
        GestorHabitaciones gestorHab = new GestorHabitaciones();
        GestorReservas gestorRes = new GestorReservas();

        System.out.println(">> Datos cargados desde persistencia (.dat):");
        System.out.println("   Habitaciones ya registradas: " + gestorHab.obtenerTotalHabitaciones());
        System.out.println("   Reservas ya registradas: " + gestorRes.obtenerTotalReservas() + "\n");

        // 2. Si es la primera ejecución (no hay habitaciones aún), se crean unas iniciales
        if (gestorHab.obtenerTotalHabitaciones() == 0) {
            System.out.println(">> Primera ejecución: creando habitaciones iniciales...");
            gestorHab.agregarHabitacion(new HabitacionSencilla(101, 100.0));
            gestorHab.agregarHabitacion(new HabitacionDoble(102, 150.0));
            gestorHab.agregarHabitacion(new Suite(201, 300.0));
            System.out.println("✓ " + gestorHab.obtenerTotalHabitaciones() + " habitaciones creadas y persistidas en datos/habitaciones.dat\n");
        }

        // 3. Iniciar el hilo ProcesadorFacturas (concurrencia en segundo plano)
        ProcesadorFacturas procesador = new ProcesadorFacturas();
        procesador.start();
        System.out.println(">> Hilo ProcesadorFacturas iniciado en segundo plano.\n");

        // 4. Crear clientes y una reserva "normal"
        Cliente cliente1 = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        Cliente cliente2 = new Cliente("María García", "7777-5678", "maria@example.com");

        Habitacion hab101 = gestorHab.obtenerHabitacion(101);

        try {
            Date hoy = new Date();
            Date manana = new Date(hoy.getTime() + (24 * 60 * 60 * 1000));

            if (hab101 != null && hab101.estaDisponible()) {
                Reserva res1 = gestorRes.crearReserva(cliente1, hab101, hoy, manana);
                System.out.println(">> Reserva creada: " + res1.getId() + " (Cliente: " + cliente1.getNombre() + ")\n");

                ServicioAdicional desayuno = new ServicioAdicional("Desayuno", 15.0);
                res1.agregarServicio(desayuno);

                Factura factura1 = new Factura(res1, "TARJETA");
                procesador.encolarFactura(factura1); // se procesa en paralelo, no bloquea aquí
            } else {
                System.out.println(">> Habitación 101 no disponible en esta ejecución (ya estaba reservada de una corrida anterior).\n");
            }
        } catch (HabitacionNoDisponibleException e) {
            System.out.println("✗ Error: " + e.getMessage() + "\n");
        }

        // 5. DEMOSTRACIÓN DE CONCURRENCIA REAL:
        //    Dos hilos intentan reservar SIMULTÁNEAMENTE la misma habitación (102).
        //    Gracias a que crearReserva() es synchronized, solo uno de los dos
        //    debe tener éxito; el otro debe recibir HabitacionNoDisponibleException.
        System.out.println(">> Prueba de concurrencia: 2 hilos compiten por la habitación 102...\n");
        Habitacion hab102 = gestorHab.obtenerHabitacion(102);

        if (hab102 != null && hab102.estaDisponible()) {
            Runnable intentoReserva = () -> {
                try {
                    Date hoy = new Date();
                    Date pasadoManana = new Date(hoy.getTime() + (48 * 60 * 60 * 1000));
                    Reserva r = gestorRes.crearReserva(cliente2, hab102, hoy, pasadoManana);
                    System.out.println("  ✓ [" + Thread.currentThread().getName() + "] Reserva EXITOSA: " + r.getId());
                } catch (HabitacionNoDisponibleException e) {
                    System.out.println("  ✗ [" + Thread.currentThread().getName() + "] Rechazado: " + e.getMessage());
                }
            };

            Thread hiloA = new Thread(intentoReserva, "Hilo-ClienteA");
            Thread hiloB = new Thread(intentoReserva, "Hilo-ClienteB");
            hiloA.start();
            hiloB.start();
            hiloA.join();
            hiloB.join();
            System.out.println("\n✓ Solo uno de los dos hilos debió reservar la habitación 102 (ver resultado arriba).\n");
        } else {
            System.out.println("  Habitación 102 no disponible para esta prueba en esta ejecución.\n");
        }

        // 6. Calcular factor de demanda con las habitaciones actuales
        int ocupadas = gestorHab.obtenerHabitacionesOcupadas();
        int total = gestorHab.obtenerTotalHabitaciones();
        double factorDemanda = CalculadorTarifas.calcularFactorDemanda(ocupadas, total);
        System.out.println(">> Factor de demanda actual: " + factorDemanda + " (" + ocupadas + "/" + total + " ocupadas)\n");

        // 7. Esperar a que el hilo ProcesadorFacturas termine de procesar la cola y detenerlo
        procesador.detener();
        procesador.join();

        // 8. Resumen final (releyendo desde los gestores, ya persistidos en .dat)
        List<Habitacion> disponibles = gestorHab.obtenerHabitacionesDisponibles();
        System.out.println("=== Resumen Final ===");
        System.out.println("Total de habitaciones: " + gestorHab.obtenerTotalHabitaciones());
        System.out.println("Habitaciones disponibles: " + disponibles.size());
        System.out.println("Habitaciones ocupadas: " + gestorHab.obtenerHabitacionesOcupadas());
        System.out.println("Total de reservas: " + gestorRes.obtenerTotalReservas());
        System.out.println("Reservas activas: " + gestorRes.obtenerReservasActivas().size());
        System.out.println("\n✓ Avance 2 (persistencia + concurrencia) ejecutado exitosamente!");
        System.out.println("  Vuelve a ejecutar el programa: los datos de datos/*.dat se recargarán automáticamente.");
    }
}
