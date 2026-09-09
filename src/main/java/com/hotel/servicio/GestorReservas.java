package com.hotel.servicio;

import com.hotel.dao.ReservaDAO;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.excepciones.HabitacionNoDisponibleException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Gestiona todas las reservas del hotel.
 * Proporciona operaciones para crear, cancelar y consultar reservas,
 * así como verificar disponibilidad de habitaciones.
 *
 * Capa de Servicio dentro de la arquitectura por capas del sistema.
 * Delega la persistencia a ReservaDAO (capa DAO).
 *
 * IMPORTANTE — Programación concurrente:
 * crearReserva() y cancelarReserva() son métodos synchronized porque, en un
 * hotel real, más de un recepcionista/cliente puede intentar reservar la
 * MISMA habitación al mismo tiempo (por ejemplo desde el mostrador y desde
 * la web a la vez). Sin sincronización, dos hilos podrían leer
 * "DISPONIBLE" antes de que ninguno alcance a marcarla como "OCUPADA",
 * y ambos terminarían creando una reserva para la misma habitación
 * (condición de carrera). Al sincronizar el método sobre esta instancia,
 * solo un hilo a la vez puede verificar y cambiar el estado de la
 * habitación, garantizando la integridad de las reservas.
 */
public class GestorReservas {

    private List<Reserva> reservas;
    private final ReservaDAO reservaDAO;

    /**
     * Constructor de GestorReservas.
     * Carga automáticamente las reservas previamente persistidas
     * en reservas.dat (si existen) mediante ReservaDAO.
     */
    public GestorReservas() {
        this.reservaDAO = new ReservaDAO();
        this.reservas = reservaDAO.cargarReservas();
    }

    /**
     * Crea una nueva reserva en el sistema.
     * Valida que la habitación esté disponible antes de crear la reserva
     * y persiste el resultado en reservas.dat.
     *
     * Sincronizado para evitar que dos hilos reserven la misma habitación
     * simultáneamente (ver nota de clase sobre concurrencia).
     *
     * @param cliente        cliente que realiza la reserva
     * @param habitacion     habitación a reservar
     * @param fechaEntrada   fecha de check-in
     * @param fechaSalida    fecha de check-out
     * @return la reserva creada
     * @throws HabitacionNoDisponibleException si la habitación no está disponible
     */
    public synchronized Reserva crearReserva(Cliente cliente, Habitacion habitacion, Date fechaEntrada, Date fechaSalida)
            throws HabitacionNoDisponibleException {

        if (!habitacion.getEstado().equals("DISPONIBLE")) {
            throw new HabitacionNoDisponibleException(
                    "La habitación " + habitacion.getNumero() + " no está disponible. Estado actual: " + habitacion.getEstado()
            );
        }

        Reserva reserva = new Reserva(cliente, habitacion, fechaEntrada, fechaSalida);
        habitacion.setEstado("OCUPADA");
        reservas.add(reserva);
        reservaDAO.guardarReservas(reservas);

        return reserva;
    }

    /**
     * Cancela una reserva existente y persiste el cambio en reservas.dat.
     * Sincronizado por la misma razón que crearReserva(): evita que una
     * cancelación y una nueva reserva sobre la misma habitación se crucen.
     *
     * @param idReserva id de la reserva a cancelar
     * @return true si la reserva fue encontrada y cancelada, false en caso contrario
     */
    public synchronized boolean cancelarReserva(String idReserva) {
        for (Reserva reserva : reservas) {
            if (reserva.getId().equals(idReserva)) {
                reserva.cancelar();
                reserva.getHabitacion().setEstado("DISPONIBLE");
                reservaDAO.guardarReservas(reservas);
                return true;
            }
        }
        return false;
    }

    /**
     * Busca disponibilidad de habitaciones para un rango de fechas.
     * Por ahora retorna todas las habitaciones disponibles,
     * en una versión futura se validarían las fechas específicas.
     *
     * @param fechaEntrada fecha de check-in deseada
     * @param fechaSalida  fecha de check-out deseada
     * @return lista de habitaciones disponibles en las fechas especificadas
     */
    public List<Habitacion> buscarDisponibilidad(Date fechaEntrada, Date fechaSalida) {
        List<Habitacion> disponibles = new ArrayList<>();
        
        // Este método retorna habitaciones disponibles
        // En una versión futura, se cruzaría con las fechas de las reservas existentes
        // Por ahora, solo busca habitaciones con estado DISPONIBLE
        for (Reserva reserva : reservas) {
            if (reserva.getHabitacion().getEstado().equals("DISPONIBLE")) {
                disponibles.add(reserva.getHabitacion());
            }
        }
        
        return disponibles;
    }

    /**
     * Obtiene una reserva por su id.
     *
     * @param idReserva id de la reserva a buscar
     * @return la reserva si existe, null en caso contrario
     */
    public Reserva obtenerReserva(String idReserva) {
        for (Reserva reserva : reservas) {
            if (reserva.getId().equals(idReserva)) {
                return reserva;
            }
        }
        return null;
    }

    /**
     * Obtiene todas las reservas activas del hotel.
     *
     * @return lista de reservas con estado ACTIVA
     */
    public List<Reserva> obtenerReservasActivas() {
        List<Reserva> activas = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if ("ACTIVA".equals(reserva.getEstado())) {
                activas.add(reserva);
            }
        }
        return activas;
    }

    /**
     * Obtiene el total de reservas registradas.
     *
     * @return cantidad total de reservas
     */
    public int obtenerTotalReservas() {
        return reservas.size();
    }

    /**
     * Obtiene todas las reservas del sistema.
     *
     * @return lista completa de reservas
     */
    public List<Reserva> obtenerTodasLasReservas() {
        return new ArrayList<>(reservas);
    }
}
