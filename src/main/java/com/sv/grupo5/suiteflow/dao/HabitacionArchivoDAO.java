package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Habitacion;

import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link HabitacionDAO} basada en archivos, persistida en
 * {@code datos/habitaciones.dat}. Gracias al polimorfismo, las subclases
 * (HabitacionSencilla, HabitacionDoble, Suite) se serializan y recuperan con su
 * tipo real sin lógica adicional.
 */
public class HabitacionArchivoDAO extends ArchivoDAO<Habitacion> implements HabitacionDAO {

    private static final String ARCHIVO = "habitaciones.dat";

    /** Construye el DAO apuntando a datos/habitaciones.dat. */
    public HabitacionArchivoDAO() {
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

    @Override
    public void guardar(Habitacion habitacion) {
        agregar(habitacion);
    }

    @Override
    public void actualizar(Habitacion habitacion) {
        boolean existe = reemplazarPrimero(h -> h.getNumero() == habitacion.getNumero(), habitacion);
        if (!existe) {
            throw new PersistenciaException(
                    "No existe la habitación a actualizar: " + habitacion.getNumero());
        }
    }

    @Override
    public Optional<Habitacion> buscarPorId(Integer numero) {
        if (numero == null) {
            return Optional.empty();
        }
        return buscarPrimero(h -> h.getNumero() == numero);
    }

    @Override
    public List<Habitacion> listarTodos() {
        return cargarTodos();
    }

    @Override
    public boolean eliminar(Integer numero) {
        if (numero == null) {
            return false;
        }
        return eliminarSi(h -> h.getNumero() == numero);
    }
}