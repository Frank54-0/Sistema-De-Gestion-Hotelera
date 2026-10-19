package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Reserva;

import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link ReservaDAO} basada en archivos, persistida en
 * {@code datos/reservas.dat}.
 *
 * <p>Las reservas se devuelven con la habitación en null (el atributo es
 * transient); el reenlace con el inventario lo hace GestorReservas.</p>
 */
public class ReservaArchivoDAO extends ArchivoDAO<Reserva> implements ReservaDAO {

    private static final String ARCHIVO = "reservas.dat";

    /** Crea el DAO apuntando a {@code datos/reservas.dat}. */
    public ReservaArchivoDAO() {
        super(ARCHIVO);
    }

    @Override
    public void guardar(Reserva reserva) {
        agregar(reserva);
    }

    @Override
    public void actualizar(Reserva reserva) {
        boolean existe = reemplazarPrimero(r -> r.getId().equals(reserva.getId()), reserva);
        if (!existe) {
            throw new PersistenciaException(
                    "No existe la reserva a actualizar: " + reserva.getId());
        }
    }

    @Override
    public Optional<Reserva> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return buscarPrimero(r -> id.equals(r.getId()));
    }

    @Override
    public List<Reserva> listarTodos() {
        return cargarTodos();
    }

    @Override
    public boolean eliminar(String id) {
        if (id == null) {
            return false;
        }
        return eliminarSi(r -> id.equals(r.getId()));
    }
}