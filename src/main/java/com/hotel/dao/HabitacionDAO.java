package com.hotel.dao;

import com.hotel.modelo.Habitacion;

import java.util.List;

/**
 * Acceso a datos de la entidad Habitacion. Persiste en datos/habitaciones.dat.
 * Gracias al polimorfismo, las subclases (HabitacionSencilla, HabitacionDoble,
 * Suite) se serializan y recuperan con su tipo real sin lógica adicional.
 */
public class HabitacionDAO extends ArchivoDAO<Habitacion> {

    private static final String ARCHIVO = "habitaciones.dat";

    /** Construye el DAO apuntando a datos/habitaciones.dat. */
    public HabitacionDAO() {
        super(ARCHIVO);
    }

    /**
     * Guarda el inventario completo de habitaciones, sobrescribiendo el anterior.
     *
     * @param habitaciones lista de habitaciones a persistir
     */
    public void guardarHabitaciones(List<Habitacion> habitaciones) {
        guardarTodos(habitaciones);
    }

    /**
     * Carga todas las habitaciones persistidas.
     *
     * @return lista de habitaciones; vacía si aún no hay datos
     */
    public List<Habitacion> cargarHabitaciones() {
        return cargarTodos();
    }

    /**
     * Agrega una habitación al inventario persistido.
     *
     * @param habitacion habitación a agregar
     */
    public void guardarHabitacion(Habitacion habitacion) {
        agregar(habitacion);
    }

    /**
     * Busca una habitación por su número.
     *
     * @param numero número de la habitación
     * @return la habitación si existe, null en caso contrario
     */
    public Habitacion buscarPorNumero(int numero) {
        for (Habitacion habitacion : cargarTodos()) {
            if (habitacion.getNumero() == numero) {
                return habitacion;
            }
        }
        return null;
    }
}
