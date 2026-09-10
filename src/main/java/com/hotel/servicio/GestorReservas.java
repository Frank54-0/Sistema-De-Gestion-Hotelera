package com.hotel.servicio;

import com.hotel.dao.ReservaDAO;
import com.hotel.excepciones.HabitacionNoDisponibleException;
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
 * Capa de Servicio. Delega la persistencia en ReservaDAO.
 *
 * <b>Concurrencia.</b> crearReserva() y cancelarReserva() son synchronized
 * porque en un hotel real dos operadores pueden intentar reservar la MISMA
 * habitación al mismo tiempo (mostrador y web, por ejemplo). Sin sincronizar,
 * ambos hilos podrían leer el estado DISPONIBLE antes de que cualquiera de
 * los dos alcance a marcarla OCUPADA — una condición de carrera clásica
 * "verificar-luego-actuar" — y el hotel terminaría con dos reservas sobre la
 * misma habitación. Al sincronizar sobre esta instancia, la verificación y el
 * cambio de estado ocurren como una sola operación indivisible.
 *
 * Los métodos de consulta también están sincronizados: recorrer la lista
 * mientras otro hilo la modifica lanzaría ConcurrentModificationException.
 */
public class GestorReservas {

    private final List<Reserva> reservas;
    private final ReservaDAO reservaDAO;

    /**
     * Construye el gestor usando el DAO por defecto y carga las reservas
     * previamente persistidas en datos/reservas.dat.
     */
    public GestorReservas() {
        this(new ReservaDAO());
    }

    /**
     * Construye el gestor con un DAO indicado por el llamador.
     * Permite inyectar un DAO de prueba en los tests unitarios sin tocar
     * los archivos reales del sistema.
     *
     * @param reservaDAO DAO encargado de la persistencia
     */
    public GestorReservas(ReservaDAO reservaDAO) {
        if (reservaDAO == null) {
            throw new IllegalArgumentException("El DAO de reservas no puede ser null");
        }
        this.reservaDAO = reservaDAO;
        this.reservas = new ArrayList<Reserva>(reservaDAO.cargarReservas());
    }

    /**
     * Crea una reserva si la habitación está disponible, la marca como OCUPADA
     * y persiste el resultado.
     *
     * @param cliente      cliente que reserva
     * @param habitacion   habitación a reservar
     * @param fechaEntrada fecha de check-in
     * @param fechaSalida  fecha de check-out
     * @return la reserva creada
     * @throws IllegalArgumentException        si algún argumento es null o las
     *                                         fechas son incoherentes
     * @throws HabitacionNoDisponibleException si la habitación no está en estado
     *                                         DISPONIBLE
     */
    public synchronized Reserva crearReserva(Cliente cliente, Habitacion habitacion,
            Date fechaEntrada, Date fechaSalida)
            throws HabitacionNoDisponibleException {

        validarDatosReserva(cliente, habitacion, fechaEntrada, fechaSalida);

        if (!habitacion.estaDisponible()) {
            throw new HabitacionNoDisponibleException(
                    "La habitación " + habitacion.getNumero()
                            + " no está disponible. Estado actual: " + habitacion.getEstado());
        }

        Reserva reserva = new Reserva(cliente, habitacion, fechaEntrada, fechaSalida);
        habitacion.setEstado(Habitacion.OCUPADA);
        reservas.add(reserva);
        reservaDAO.guardarReservas(reservas);

        return reserva;
    }

    /**
     * Cancela una reserva activa, libera su habitación y persiste el cambio.
     * Cancelar una reserva que ya estaba cancelada no tiene efecto.
     *
     * @param idReserva identificador de la reserva
     * @return true si la reserva existía y estaba activa; false en otro caso
     */
    public synchronized boolean cancelarReserva(String idReserva) {
        Reserva reserva = buscarPorId(idReserva);
        if (reserva == null || !reserva.estaActiva()) {
            return false;
        }
        reserva.cancelar();
        reserva.getHabitacion().setEstado(Habitacion.DISPONIBLE);
        reservaDAO.guardarReservas(reservas);
        return true;
    }

    /**
     * Devuelve las habitaciones disponibles asociadas a las reservas registradas.
     *
     * <b>Nota para el equipo:</b> este método conserva el comportamiento del
     * Avance 1 y todavía NO cruza el rango de fechas recibido; los parámetros
     * se aceptan pero no se usan, y solo se recorren habitaciones que ya
     * aparecen en alguna reserva. La consulta correcta de disponibilidad por
     * fechas está pendiente de definición con el Architect (ver notas del
     * Avance 2). No construir lógica de negocio nueva sobre este método hasta
     * que esa definición exista.
     *
     * @param fechaEntrada fecha de check-in deseada (aún no evaluada)
     * @param fechaSalida  fecha de check-out deseada (aún no evaluada)
     * @return lista de habitaciones en estado DISPONIBLE halladas en las reservas
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
     * Obtiene una reserva por su identificador.
     *
     * @param idReserva identificador de la reserva
     * @return la reserva si existe, null en caso contrario
     */
    public synchronized Reserva obtenerReserva(String idReserva) {
        return buscarPorId(idReserva);
    }

    /**
     * Obtiene las reservas vigentes.
     *
     * @return lista de reservas en estado ACTIVA
     */
    public synchronized List<Reserva> obtenerReservasActivas() {
        List<Reserva> activas = new ArrayList<Reserva>();
        for (Reserva reserva : reservas) {
            if (reserva.estaActiva()) {
                activas.add(reserva);
            }
        }
        return activas;
    }

    /**
     * Total de reservas registradas, sin importar su estado.
     *
     * @return cantidad total de reservas
     */
    public synchronized int obtenerTotalReservas() {
        return reservas.size();
    }

    /**
     * Vista de solo lectura de todas las reservas.
     *
     * @return lista inmutable de reservas
     */
    public synchronized List<Reserva> obtenerTodasLasReservas() {
        return Collections.unmodifiableList(new ArrayList<Reserva>(reservas));
    }

    /**
     * Búsqueda interna por id. No sincroniza: siempre se invoca desde un
     * método que ya tiene el monitor de esta instancia.
     *
     * @param idReserva identificador a buscar
     * @return la reserva o null
     */
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

    /**
     * Valida los datos mínimos de una reserva antes de crearla.
     *
     * @throws IllegalArgumentException si algún dato es null o las fechas son
     *                                  incoherentes
     */
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
