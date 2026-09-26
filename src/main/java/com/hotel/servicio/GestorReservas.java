package com.hotel.servicio;

import com.hotel.dao.ReservaDAO;
import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.excepciones.PersistenciaException;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Gestiona el ciclo de vida de las reservas: creación, cancelación y consulta.
 *
 * <p><b>Concurrencia.</b> Dos operadores pueden intentar reservar la misma
 * habitación al mismo tiempo. Sin sincronizar, ambos podrían ver la habitación
 * DISPONIBLE antes de que alguno la ocupe (condición de carrera
 * "verificar-luego-actuar"). Todos los métodos que tocan la lista son
 * {@code synchronized}; los de consulta también, para evitar
 * ConcurrentModificationException.</p>
 *
 * <p><b>Estado de las habitaciones.</b> Este gestor nunca llama a
 * {@code Habitacion.setEstado()}: todo cambio pasa por
 * {@link GestorHabitaciones#cambiarEstado}, que es el único punto de entrada y
 * además persiste en {@code habitaciones.dat}. Orden de candados: siempre
 * GestorReservas y luego GestorHabitaciones. GestorHabitaciones nunca llama a
 * este gestor, por lo que no puede haber deadlock (hallazgo #2 de QA).</p>
 */
public class GestorReservas {

    private final List<Reserva> reservas;
    private final ReservaDAO reservaDAO;
    private final GestorHabitaciones gestorHabitaciones;

    /**
     * Crea el gestor con el DAO por defecto ({@code datos/reservas.dat}).
     *
     * @param gestorHabitaciones inventario del hotel; debe ser la misma instancia
     *                           que usa el resto del sistema, o volverían a
     *                           existir dos copias de cada habitación
     */
    public GestorReservas(GestorHabitaciones gestorHabitaciones) {
        this(new ReservaDAO(), gestorHabitaciones);
    }

    /**
     * Crea el gestor con un DAO indicado, lo que permite inyectar uno de prueba.
     *
     * @param reservaDAO         DAO encargado de la persistencia
     * @param gestorHabitaciones inventario del hotel
     * @throws PersistenciaException si una reserva guardada apunta a una
     *                               habitación inexistente en el inventario
     */
    public GestorReservas(ReservaDAO reservaDAO, GestorHabitaciones gestorHabitaciones) {
        if (reservaDAO == null) {
            throw new IllegalArgumentException("El DAO de reservas no puede ser null");
        }
        if (gestorHabitaciones == null) {
            throw new IllegalArgumentException("El gestor de habitaciones no puede ser null");
        }
        this.reservaDAO = reservaDAO;
        this.gestorHabitaciones = gestorHabitaciones;
        this.reservas = new ArrayList<Reserva>(reservaDAO.cargarReservas());
        reenlazarHabitaciones();
    }

    // Las reservas llegan del .dat con la habitación en null (es transient).
    // Se enlazan al objeto del inventario para que ambos gestores compartan la misma instancia.
    private void reenlazarHabitaciones() {
        for (Reserva reserva : reservas) {
            Habitacion real = gestorHabitaciones.obtenerHabitacion(reserva.getNumeroHabitacion());
            if (real == null) {
                throw new PersistenciaException(
                        "La reserva " + reserva.getId() + " apunta a la habitación "
                                + reserva.getNumeroHabitacion() + ", que no existe en el inventario");
            }
            reserva.setHabitacion(real);
        }
    }

    /**
     * Crea una reserva si la habitación está disponible, la marca OCUPADA y
     * persiste ambos cambios.
     *
     * @param cliente      cliente que reserva
     * @param habitacion   habitación a reservar; debe estar en el inventario
     * @param fechaEntrada fecha de check-in
     * @param fechaSalida  fecha de check-out
     * @return la reserva creada
     * @throws IllegalArgumentException        si falta algún dato, las fechas son
     *                                         incoherentes o la habitación no está
     *                                         en el inventario
     * @throws HabitacionNoDisponibleException si la habitación no está DISPONIBLE
     */
    public synchronized Reserva crearReserva(Cliente cliente, Habitacion habitacion,
            Date fechaEntrada, Date fechaSalida)
            throws HabitacionNoDisponibleException {

        validarDatosReserva(cliente, habitacion, fechaEntrada, fechaSalida);

        // Mismo monitor que usan los métodos synchronized de GestorHabitaciones: así
        // una llamada directa a cambiarEstado() no puede colarse entre verificar y ocupar.
        synchronized (gestorHabitaciones) {
            // Se usa la instancia del inventario y no la recibida, que podría ser otra copia.
            Habitacion real = gestorHabitaciones.obtenerHabitacion(habitacion.getNumero());
            if (real == null) {
                throw new IllegalArgumentException(
                        "La habitación " + habitacion.getNumero() + " no está registrada en el inventario");
            }
            if (!real.estaDisponible()) {
                throw new HabitacionNoDisponibleException(
                        "La habitación " + real.getNumero()
                                + " no está disponible. Estado actual: " + real.getEstado());
            }

            Reserva reserva = new Reserva(cliente, real, fechaEntrada, fechaSalida);
            gestorHabitaciones.cambiarEstado(real.getNumero(), Habitacion.OCUPADA);
            reservas.add(reserva);
            reservaDAO.guardarReservas(reservas);

            return reserva;
        }
    }

    /**
     * Cancela una reserva activa, libera su habitación y persiste ambos cambios.
     *
     * @param idReserva identificador de la reserva
     * @return true si existía y estaba activa; false si no existe o ya estaba
     *         cancelada
     */
    public synchronized boolean cancelarReserva(String idReserva) {
        Reserva reserva = buscarPorId(idReserva);
        if (reserva == null || !reserva.estaActiva()) {
            return false;
        }
        reserva.cancelar();
        reservaDAO.guardarReservas(reservas);
        gestorHabitaciones.cambiarEstado(reserva.getNumeroHabitacion(), Habitacion.DISPONIBLE);
        return true;
    }

    /**
     * Devuelve las habitaciones DISPONIBLES que aparecen en alguna reserva.
     *
     * <p><b>Pendiente:</b> conserva el comportamiento del Avance 1. Todavía no
     * evalúa el rango de fechas ni recorre el inventario completo; la consulta
     * por fechas está pendiente de definición con el Architect (hallazgo #6 de
     * QA). No construir lógica nueva sobre este método hasta entonces.</p>
     *
     * @param fechaEntrada fecha de check-in deseada (aún no evaluada)
     * @param fechaSalida  fecha de check-out deseada (aún no evaluada)
     * @return habitaciones en estado DISPONIBLE halladas en las reservas
     */
    public synchronized List<Habitacion> buscarDisponibilidad(Date fechaEntrada, Date fechaSalida) {
        List<Habitacion> disponibles = new ArrayList<Habitacion>();
        for (Reserva reserva : reservas) {
            if (reserva.getHabitacion().estaDisponible()) {
                disponibles.add(reserva.getHabitacion());
            }
        }
        return disponibles;
    }

    /**
     * @param idReserva identificador de la reserva
     * @return la reserva, o null si no existe
     */
    public synchronized Reserva obtenerReserva(String idReserva) {
        return buscarPorId(idReserva);
    }

    /** @return reservas en estado ACTIVA */
    public synchronized List<Reserva> obtenerReservasActivas() {
        List<Reserva> activas = new ArrayList<Reserva>();
        for (Reserva reserva : reservas) {
            if (reserva.estaActiva()) {
                activas.add(reserva);
            }
        }
        return activas;
    }

    /** @return cantidad total de reservas, sin importar su estado */
    public synchronized int obtenerTotalReservas() {
        return reservas.size();
    }

    /**
     * Devuelve una copia inmutable para que ninguna otra capa modifique la lista
     * sin pasar por este gestor (y sin persistir el cambio).
     *
     * @return lista de solo lectura con todas las reservas
     */
    public synchronized List<Reserva> obtenerTodasLasReservas() {
        return Collections.unmodifiableList(new ArrayList<Reserva>(reservas));
    }

    // Sin synchronized: solo se llama desde métodos que ya tienen el monitor.
    private Reserva buscarPorId(String idReserva) {
        if (idReserva == null) {
            return null;
        }
        for (Reserva reserva : reservas) {
            if (idReserva.equals(reserva.getId())) {
                return reserva;
            }
        }
        return null;
    }

    private void validarDatosReserva(Cliente cliente, Habitacion habitacion,
            Date fechaEntrada, Date fechaSalida) {
        if (cliente == null) {
            throw new IllegalArgumentException("La reserva requiere un cliente");
        }
        if (habitacion == null) {
            throw new IllegalArgumentException("La reserva requiere una habitación");
        }
        if (fechaEntrada == null || fechaSalida == null) {
            throw new IllegalArgumentException("La reserva requiere fecha de entrada y de salida");
        }
        if (!fechaSalida.after(fechaEntrada)) {
            throw new IllegalArgumentException(
                    "La fecha de salida debe ser posterior a la fecha de entrada");
        }
    }
}
