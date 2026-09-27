package com.hotel;

import com.hotel.controlador.ReservaController;
import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.modelo.*;
import com.hotel.servicio.CalculadorTarifas;
import com.hotel.servicio.GestorHabitaciones;
import com.hotel.servicio.GestorReservas;
import com.hotel.servicio.ProcesadorFacturas;
import com.hotel.vista.VistaReservas;

import java.util.Date;
import java.util.Scanner;
import java.util.List;

// Punto de arranque de la aplicación (Avance 2).
// Main SOLO crea las capas y las conecta entre sí:
// Servicio (Gestores) -> Control (ReservaController) -> Presentación (VistaReservas).
// No contiene lógica de negocio ni interacción con el usuario: eso vive en
// las clases de cada capa correspondiente.

/**
 * Demostración por consola del Avance 2: persistencia en .dat, procesamiento de
 * facturas en segundo plano y sincronización de reservas concurrentes.
 *
 * <p>Está pensada para ejecutarse dos veces seguidas: la segunda corrida carga
 * los datos de la primera y debe rechazar las habitaciones ya reservadas.</p>
 */
public class Main {

    /**
     * @param args no se usan
     * @throws InterruptedException si se interrumpe la espera de los hilos
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Sistema de Gestión Hotelera - Avance 2 ===\n");

        // 1. Capa de Servicio: cargan automáticamente datos previos desde datos/*.dat
        // GestorReservas debe recibir el MISMO GestorHabitaciones: con otra instancia
        // habría dos copias de cada habitación y sus estados se desincronizarían.
        GestorHabitaciones gestorHab = new GestorHabitaciones();
        GestorReservas gestorRes = new GestorReservas(gestorHab);

        // 2. Si es la primera ejecución, se crean habitaciones iniciales
        System.out.println(">> Datos cargados desde persistencia (.dat):");
        System.out.println("   Habitaciones ya registradas: " + gestorHab.obtenerTotalHabitaciones());
        System.out.println("   Reservas ya registradas: " + gestorRes.obtenerTotalReservas() + "\n");

        if (gestorHab.obtenerTotalHabitaciones() == 0) {
            System.out.println(">> Primera ejecución: creando habitaciones iniciales...");
            gestorHab.agregarHabitacion(new HabitacionSencilla(101, 100.0));
            gestorHab.agregarHabitacion(new HabitacionDoble(102, 150.0));
            gestorHab.agregarHabitacion(new Suite(201, 300.0));
            System.out.println("Habitaciones creadas y persistidas en datos/habitaciones.dat\n");
        }

        // 3. Hilo en segundo plano para procesar facturas
        ProcesadorFacturas procesador = new ProcesadorFacturas();
        procesador.start();

        // 4. Prueba de sincronización: dos hilos compiten por la habitación 102.
        //    Se conserva como evidencia de que crearReserva() es thread-safe.
        ejecutarPruebaConcurrencia(gestorHab, gestorRes);
        Cliente cliente1 = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        Cliente cliente2 = new Cliente("María García", "7777-5678", "maria@example.com");

        // 5. Capa de Control: el Controller conecta la Vista con el Servicio
        ReservaController reservaController = new ReservaController(gestorRes, gestorHab);

        // 6. Capa de Presentación: el usuario solo interactúa con la Vista
        Scanner scanner = new Scanner(System.in);
        VistaReservas vista = new VistaReservas(reservaController, scanner);
        vista.iniciar();

        // 7. Al salir del menú, se detiene el hilo en segundo plano
        // (Se unificó el cierre del hilo con el bloque del resumen final que tenías)

        // --- RESUMEN FINAL RESTAURADO EXACTAMENTE COMO LO TENÍAS ---
        int ocupadas = gestorHab.obtenerHabitacionesOcupadas();
        int total = gestorHab.obtenerTotalHabitaciones();
        double factorDemanda = CalculadorTarifas.calcularFactorDemanda(ocupadas, total);
        System.out.println(">> Factor de demanda actual: " + factorDemanda + " (" + ocupadas + "/" + total + " ocupadas)\n");

        // join() espera a que el hilo vacíe la cola; sin él, el resumen podría
        // imprimirse con facturas aún sin procesar.
        procesador.detener();
        procesador.join();

        List<Habitacion> disponibles = gestorHab.obtenerHabitacionesDisponibles();
        System.out.println("=== Resumen Final ===");
        System.out.println("Total de habitaciones: " + gestorHab.obtenerTotalHabitaciones());
        System.out.println("Habitaciones disponibles: " + disponibles.size());
        System.out.println("Habitaciones ocupadas: " + gestorHab.obtenerHabitacionesOcupadas());
        System.out.println("Total de reservas: " + gestorRes.obtenerTotalReservas());
        System.out.println("Reservas activas: " + gestorRes.obtenerReservasActivas().size());
        System.out.println("\n✓ Avance 2 (persistencia + concurrencia) ejecutado exitosamente!");
        System.out.println("  Vuelve a ejecutar el programa: los datos de datos/*.dat se recargarán automáticamente.");

        System.out.println("\nSesión finalizada. Los datos quedaron guardados en datos/*.dat");
    }

    private static void ejecutarPruebaConcurrencia(GestorHabitaciones gestorHab, GestorReservas gestorRes)
            throws InterruptedException {
        // Dos hilos compiten por la 102. Como crearReserva() es synchronized, solo uno
        // puede verificar y ocupar a la vez; el otro recibe HabitacionNoDisponibleException.
        System.out.println(">> Prueba de concurrencia: 2 hilos compiten por la habitación 102...\n");
        Habitacion hab102 = gestorHab.obtenerHabitacion(102);
        if (hab102 == null || !hab102.estaDisponible()) {
            return;
        }

        Cliente clientePrueba = new Cliente("Cliente de prueba", "0000-0000", "prueba@example.com");

        Runnable intentoReserva = () -> {
            try {
                Date hoy = new Date();
                Date pasadoManana = new Date(hoy.getTime() + (48 * 60 * 60 * 1000));
                Reserva r = gestorRes.crearReserva(clientePrueba, hab102, hoy, pasadoManana);
                System.out.println("  [" + Thread.currentThread().getName() + "] Reserva EXITOSA: " + r.getId());
            } catch (HabitacionNoDisponibleException e) {
                System.out.println("  [" + Thread.currentThread().getName() + "] Rechazado: " + e.getMessage());
            }
        };

        System.out.println(">> Prueba de concurrencia: 2 hilos compiten por la habitación 102...");
        Thread hiloA = new Thread(intentoReserva, "Hilo-ClienteA");
        Thread hiloB = new Thread(intentoReserva, "Hilo-ClienteB");
        hiloA.start();
        hiloB.start();
        hiloA.join();
        hiloB.join();
        System.out.println("Solo uno de los dos hilos debió reservar la habitación 102.\n");
    }
}