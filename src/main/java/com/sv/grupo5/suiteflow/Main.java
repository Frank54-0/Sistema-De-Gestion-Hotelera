package com.sv.grupo5.suiteflow;

import com.sv.grupo5.suiteflow.config.DataSourceConfig;
import com.sv.grupo5.suiteflow.config.InicializadorBaseDatos;
import com.sv.grupo5.suiteflow.controller.ReservaController;
import com.sv.grupo5.suiteflow.dao.FacturaJdbcDAO;
import com.sv.grupo5.suiteflow.dao.HabitacionJdbcDAO;
import com.sv.grupo5.suiteflow.dao.ReservaJdbcDAO;
import com.sv.grupo5.suiteflow.dto.NuevaReservaDTO;
import com.sv.grupo5.suiteflow.dto.ResultadoSolicitudDTO;
import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.service.CalculadorTarifas;
import com.sv.grupo5.suiteflow.service.GestorHabitaciones;
import com.sv.grupo5.suiteflow.service.GestorReservas;
import com.sv.grupo5.suiteflow.service.ProcesadorFacturas;
import com.sv.grupo5.suiteflow.service.ProcesadorSolicitudesReserva;
import com.sv.grupo5.suiteflow.view.VistaReservas;
import org.apache.tomcat.jdbc.pool.DataSource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Punto de arranque de SuiteFlow. Solo crea las capas y las conecta:
 * base de datos (DataSource y DAO JDBC) -> servicio (gestores) -> controlador -> vista.
 *
 * <p>Sin argumentos abre el menú. Con {@code --demo-concurrencia} lanza un lote de
 * reservas en paralelo y muestra el resultado, sin menú.</p>
 */
public class Main {

    private static final String DEMO = "--demo-concurrencia";

    /**
     * @param args {@code --demo-concurrencia} para la demostración; vacío para el menú
     * @throws InterruptedException si se interrumpe la espera de los hilos
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SuiteFlow - Sistema de Gestión Hotelera ===\n");

        DataSource dataSource = null;
        try {
            dataSource = DataSourceConfig.crearDesdeConfiguracion();
            InicializadorBaseDatos.inicializar(dataSource);
            ejecutar(dataSource, args.length > 0 && DEMO.equals(args[0]));
        } catch (PersistenciaException e) {
            System.out.println("No se pudo usar la base de datos: " + e.getMessage());
            System.out.println("Revise que MySQL/MariaDB esté encendido y que db.properties sea correcto.");
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }

    private static void ejecutar(DataSource dataSource, boolean demo) throws InterruptedException {
        // Los dos gestores comparten el mismo inventario de habitaciones.
        GestorHabitaciones gestorHab = new GestorHabitaciones(new HabitacionJdbcDAO(dataSource));
        GestorReservas gestorRes = new GestorReservas(new ReservaJdbcDAO(dataSource), gestorHab);
        ReservaController controlador = new ReservaController(gestorRes, gestorHab);

        System.out.println(">> Datos cargados desde la base de datos:");
        System.out.println("   Habitaciones: " + gestorHab.obtenerTotalHabitaciones());
        System.out.println("   Reservas: " + gestorRes.obtenerTotalReservas() + "\n");

        // Hilo en segundo plano que procesa facturas.
        ProcesadorFacturas procesadorFacturas = new ProcesadorFacturas(new FacturaJdbcDAO(dataSource));
        procesadorFacturas.start();

        if (demo) {
            ejecutarDemoConcurrencia(controlador);
        } else {
            new VistaReservas(controlador, new Scanner(System.in)).iniciar();
        }

        mostrarResumen(gestorHab, gestorRes);

        // join() espera a que el hilo termine de vaciar su cola.
        procesadorFacturas.detener();
        procesadorFacturas.join();
        System.out.println("\nSesión finalizada. Los datos quedaron guardados en la base de datos.");
    }

    // 8 solicitudes a la misma habitación y 1 a otra, en paralelo con ExecutorService.
    private static void ejecutarDemoConcurrencia(ReservaController controlador) throws InterruptedException {
        System.out.println(">> Demo de concurrencia: 8 solicitudes para la habitación 103 y 1 para la 104...\n");
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 48L * 60 * 60 * 1000);

        List<NuevaReservaDTO> lote = new ArrayList<NuevaReservaDTO>();
        String[] nombres = {"Ana Lopez", "Bruno Diaz", "Carla Mena", "Diego Rios",
                "Elena Paz", "Fabio Cruz", "Gina Sosa", "Hugo Vega"};
        for (int i = 0; i < nombres.length; i++) {
            lote.add(new NuevaReservaDTO(nombres[i], "7000000" + (i + 1),
                    "cliente" + (i + 1) + "@mail.com", 103, entrada, salida));
        }
        lote.add(new NuevaReservaDTO("Ivan Mora", "70000009", "cliente9@mail.com", 104, entrada, salida));

        try (ProcesadorSolicitudesReserva procesador =
                     new ProcesadorSolicitudesReserva(controlador, DataSourceConfig.HILOS_RESERVA)) {
            int exitosas = 0;
            for (ResultadoSolicitudDTO r : procesador.procesarLote(lote)) {
                String quien = r.getSolicitud().getNombreCliente();
                if (r.isExitosa()) {
                    exitosas++;
                    System.out.println("  " + quien + ": reserva creada " + r.getReserva().getIdReserva());
                } else {
                    System.out.println("  " + quien + ": rechazada. " + r.getMotivoRechazo());
                }
            }
            System.out.println("\nReservas creadas: " + exitosas + " de " + lote.size()
                    + " (solo una por habitación).\n");
        }
    }

    private static void mostrarResumen(GestorHabitaciones gestorHab, GestorReservas gestorRes) {
        int ocupadas = gestorHab.obtenerHabitacionesOcupadas();
        int total = gestorHab.obtenerTotalHabitaciones();
        double factorDemanda = CalculadorTarifas.calcularFactorDemanda(ocupadas, total);
        List<Habitacion> disponibles = gestorHab.obtenerHabitacionesDisponibles();

        System.out.println(">> Factor de demanda actual: " + factorDemanda
                + " (" + ocupadas + "/" + total + " ocupadas)\n");
        System.out.println("=== Resumen Final ===");
        System.out.println("Total de habitaciones: " + total);
        System.out.println("Habitaciones disponibles: " + disponibles.size());
        System.out.println("Habitaciones ocupadas: " + ocupadas);
        System.out.println("Total de reservas: " + gestorRes.obtenerTotalReservas());
        System.out.println("Reservas activas: " + gestorRes.obtenerReservasActivas().size());
    }
}
