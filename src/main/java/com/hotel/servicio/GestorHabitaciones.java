package com.hotel.servicio;

import com.hotel.dao.HabitacionDAO;
import com.hotel.modelo.Habitacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gestiona el inventario de habitaciones del hotel: alta, cambio de estado
 * y consultas de disponibilidad.
 *
 * Capa de Servicio. Nunca toca archivos directamente: delega la persistencia
 * en HabitacionDAO.
 *
 * Concurrencia: la lista interna se mantiene en memoria y es leída y escrita
 * por varios hilos, por lo que todos los métodos que la recorren o modifican
 * están sincronizados sobre esta instancia. Los métodos de consulta también
 * lo están: recorrer una lista mientras otro hilo la modifica produce
 * ConcurrentModificationException.
 */
public class GestorHabitaciones {

    private final List<Habitacion> habitaciones;
    private final HabitacionDAO habitacionDAO;

    /**
     * Construye el gestor usando el DAO por defecto y carga el inventario
     * previamente persistido en datos/habitaciones.dat.
     */
    public GestorHabitaciones() {
        this(new HabitacionDAO());
    }

    /**
     * Construye el gestor con un DAO indicado por el llamador.
     * El constructor permite inyectar un DAO de prueba en los tests
     * unitarios sin tocar los archivos del sistema.
     *
     * @param habitacionDAO DAO encargado de la persistencia
     */
    public GestorHabitaciones(HabitacionDAO habitacionDAO) {
        if (habitacionDAO == null) {
            throw new IllegalArgumentException("El DAO de habitaciones no puede ser null");
        }
        this.habitacionDAO = habitacionDAO;
        this.habitaciones = new ArrayList<Habitacion>(habitacionDAO.cargarHabitaciones());
    }

    /**
     * Agrega una habitación al inventario y persiste el cambio.
     * Rechaza números duplicados: el número de habitación identifica de forma
     * única a la habitación dentro del hotel.
     *
     * @param habitacion habitación a agregar
     * @return true si se agregó, false si era null o el número ya existía
     */
    public synchronized boolean agregarHabitacion(Habitacion habitacion) {
        if (habitacion == null || buscarPorNumero(habitacion.getNumero()) != null) {
            return false;
        }
        habitaciones.add(habitacion);
        habitacionDAO.guardarHabitaciones(habitaciones);
        return true;
    }

    /**
     * Cambia el estado de una habitación y persiste el cambio.
     *
     * @param numero número de la habitación
     * @param estado nuevo estado; debe ser uno de los definidos en Habitacion
     * @return true si la habitación existía y fue actualizada
     * @throws IllegalArgumentException si el estado no es válido
     */
    public synchronized boolean cambiarEstado(int numero, String estado) {
        Habitacion habitacion = buscarPorNumero(numero);
        if (habitacion == null) {
            return false;
        }
        habitacion.setEstado(estado);
        habitacionDAO.guardarHabitaciones(habitaciones);
        return true;
    }

    /**
     * Obtiene una habitación por su número.
     *
     * @param numero número de la habitación
     * @return la habitación si existe, null en caso contrario
     */
    public synchronized Habitacion obtenerHabitacion(int numero) {
        return buscarPorNumero(numero);
    }

    /**
     * Obtiene las habitaciones que se pueden reservar en este momento.
     *
     * @return lista de habitaciones en estado DISPONIBLE
     */
    public synchronized List<Habitacion> obtenerHabitacionesDisponibles() {
        List<Habitacion> disponibles = new ArrayList<Habitacion>();
        for (Habitacion habitacion : habitaciones) {
            if (habitacion.estaDisponible()) {
                disponibles.add(habitacion);
            }
        }
        return disponibles;
    }

    /**
     * Total de habitaciones registradas en el hotel.
     *
     * @return cantidad total de habitaciones
     */
    public synchronized int obtenerTotalHabitaciones() {
        return habitaciones.size();
    }

    /**
     * Cantidad de habitaciones actualmente ocupadas.
     *
     * @return número de habitaciones en estado OCUPADA
     */
    public synchronized int obtenerHabitacionesOcupadas() {
        int ocupadas = 0;
        for (Habitacion habitacion : habitaciones) {
            if (Habitacion.OCUPADA.equals(habitacion.getEstado())) {
                ocupadas++;
            }
        }
        return ocupadas;
    }

    /**
     * Vista de solo lectura del inventario completo.
     * Se devuelve inmutable para que ninguna otra capa pueda alterar el
     * inventario sin pasar por este gestor (y sin persistir el cambio).
     *
     * @return lista inmutable de todas las habitaciones
     */
    public synchronized List<Habitacion> obtenerTodasLasHabitaciones() {
        return Collections.unmodifiableList(new ArrayList<Habitacion>(habitaciones));
    }

    /**
     * Búsqueda interna por número. No sincroniza: siempre se invoca desde
     * un método que ya tiene el monitor de esta instancia.
     *
     * @param numero número de la habitación
     * @return la habitación o null
     */
    private Habitacion buscarPorNumero(int numero) {
        for (Habitacion habitacion : habitaciones) {
            if (habitacion.getNumero() == numero) {
                return habitacion;
            }
        }
        return null;
    }
}
