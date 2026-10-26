package com.sv.grupo5.suiteflow.service;

import com.sv.grupo5.suiteflow.controller.ReservaController;
import com.sv.grupo5.suiteflow.dto.NuevaReservaDTO;
import com.sv.grupo5.suiteflow.dto.ReservaResumenDTO;
import com.sv.grupo5.suiteflow.dto.ResultadoSolicitudDTO;
import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.exception.PersistenciaException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Procesa varias solicitudes de reserva a la vez con un pool de hilos. Los errores
 * esperados no se lanzan: cada uno queda como un resultado rechazado.
 */
public class ProcesadorSolicitudesReserva implements AutoCloseable {

    private final ReservaController controlador;
    private final ExecutorService ejecutor;

    /**
     * @param controlador controlador que crea las reservas
     * @param hilos       cantidad de hilos (mínimo 1)
     * @throws IllegalArgumentException si el controlador es null o hilos &lt; 1
     */
    public ProcesadorSolicitudesReserva(ReservaController controlador, int hilos) {
        if (controlador == null) {
            throw new IllegalArgumentException("El controlador no puede ser null");
        }
        if (hilos < 1) {
            throw new IllegalArgumentException("Debe haber al menos un hilo");
        }
        this.controlador = controlador;
        this.ejecutor = Executors.newFixedThreadPool(hilos, tarea -> {
            Thread hilo = new Thread(tarea);
            hilo.setDaemon(true);
            return hilo;
        });
    }

    /**
     * Procesa las solicitudes en paralelo y espera a que terminen todas.
     *
     * @param solicitudes solicitudes a procesar
     * @return un resultado por solicitud, en el mismo orden
     * @throws InterruptedException si el hilo que espera es interrumpido
     */
    public List<ResultadoSolicitudDTO> procesarLote(List<NuevaReservaDTO> solicitudes)
            throws InterruptedException {
        List<Future<ResultadoSolicitudDTO>> pendientes = new ArrayList<Future<ResultadoSolicitudDTO>>();
        for (final NuevaReservaDTO solicitud : solicitudes) {
            pendientes.add(ejecutor.submit(() -> procesarUna(solicitud)));
        }
        List<ResultadoSolicitudDTO> resultados = new ArrayList<ResultadoSolicitudDTO>();
        for (Future<ResultadoSolicitudDTO> futuro : pendientes) {
            try {
                resultados.add(futuro.get());
            } catch (ExecutionException e) {
                throw new IllegalStateException("Error inesperado procesando una solicitud", e.getCause());
            }
        }
        return resultados;
    }

    // Lo que ejecuta cada hilo.
    private ResultadoSolicitudDTO procesarUna(NuevaReservaDTO solicitud) {
        try {
            ReservaResumenDTO resumen = controlador.crearReserva(solicitud);
            return ResultadoSolicitudDTO.exito(solicitud, resumen);
        } catch (HabitacionNoDisponibleException e) {
            return ResultadoSolicitudDTO.rechazo(solicitud, "Habitación no disponible: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResultadoSolicitudDTO.rechazo(solicitud, "Datos inválidos: " + e.getMessage());
        } catch (PersistenciaException e) {
            return ResultadoSolicitudDTO.rechazo(solicitud, "Error de persistencia: " + e.getMessage());
        }
    }

    /** Detiene el pool y espera hasta 5 segundos a que terminen las tareas. */
    @Override
    public void close() {
        ejecutor.shutdown();
        try {
            if (!ejecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ejecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ejecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
