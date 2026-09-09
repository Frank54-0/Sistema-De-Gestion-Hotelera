package com.hotel.servicio;

import com.hotel.dao.HabitacionDAO;
import com.hotel.modelo.Habitacion;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona todas las habitaciones del hotel.
 * Proporciona operaciones para agregar habitaciones, cambiar su estado,
 * y consultar disponibilidad.
 *
 * Capa de Servicio dentro de la arquitectura por capas del sistema.
 * Delega la persistencia de los datos a HabitacionDAO (capa DAO),
 * de modo que esta clase nunca toca archivos directamente.
 */
public class GestorHabitaciones {

    private List<Habitacion> habitaciones;
    private final HabitacionDAO habitacionDAO;

    /**
     * Constructor de GestorHabitaciones.
     * Carga automáticamente las habitaciones previamente persistidas
     * en habitaciones.dat (si existen) mediante HabitacionDAO.
     */
    public GestorHabitaciones() {
        this.habitacionDAO = new HabitacionDAO();
        this.habitaciones = habitacionDAO.cargarHabitaciones();
    }

    /**
     * Agrega una nueva habitación al inventario del hotel y persiste
     * el cambio inmediatamente en habitaciones.dat.
     *
     * @param habitacion habitación a agregar
     */
    public synchronized void agregarHabitacion(Habitacion habitacion) {
        if (habitacion != null) {
            habitaciones.add(habitacion);
            habitacionDAO.guardarHabitaciones(habitaciones);
        }
    }

    /**
     * Cambia el estado de una habitación identificada por su número
     * y persiste el cambio en habitaciones.dat.
     * Es un método sincronizado porque puede ser invocado desde varios
     * hilos a la vez (por ejemplo, GestorReservas al confirmar o cancelar reservas).
     *
     * @param numero número de la habitación
     * @param estado nuevo estado (DISPONIBLE, OCUPADA, MANTENIMIENTO)
     * @return true si la habitación fue encontrada y actualizada, false en caso contrario
     */
    public synchronized boolean cambiarEstado(int numero, String estado) {
        for (Habitacion habitacion : habitaciones) {
            if (habitacion.getNumero() == numero) {
                habitacion.setEstado(estado);
                habitacionDAO.guardarHabitaciones(habitaciones);
                return true;
            }
        }
        return false;
    }

    /**
     * Obtiene una habitación por su número.
     *
     * @param numero número de la habitación a buscar
     * @return la habitación si existe, null en caso contrario
     */
    public Habitacion obtenerHabitacion(int numero) {
        for (Habitacion habitacion : habitaciones) {
            if (habitacion.getNumero() == numero) {
                return habitacion;
            }
        }
        return null;
    }

    /**
     * Obtiene todas las habitaciones disponibles en el hotel.
     *
     * @return lista de habitaciones con estado DISPONIBLE
     */
    public List<Habitacion> obtenerHabitacionesDisponibles() {
        List<Habitacion> disponibles = new ArrayList<>();
        for (Habitacion habitacion : habitaciones) {
            if ("DISPONIBLE".equals(habitacion.getEstado())) {
                disponibles.add(habitacion);
            }
        }
        return disponibles;
    }

    /**
     * Obtiene el total de habitaciones en el hotel.
     *
     * @return cantidad total de habitaciones
     */
    public int obtenerTotalHabitaciones() {
        return habitaciones.size();
    }

    /**
     * Obtiene el número de habitaciones ocupadas.
     *
     * @return cantidad de habitaciones con estado OCUPADA
     */
    public int obtenerHabitacionesOcupadas() {
        int ocupadas = 0;
        for (Habitacion habitacion : habitaciones) {
            if ("OCUPADA".equals(habitacion.getEstado())) {
                ocupadas++;
            }
        }
        return ocupadas;
    }

    /**
     * Obtiene todas las habitaciones registradas.
     *
     * @return lista completa de habitaciones
     */
    public List<Habitacion> obtenerTodasLasHabitaciones() {
        return new ArrayList<>(habitaciones);
    }
}
