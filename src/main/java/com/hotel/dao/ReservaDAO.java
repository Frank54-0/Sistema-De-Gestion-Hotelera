package com.hotel.dao;

import com.hotel.modelo.Reserva;

import java.util.List;

/**
 * Acceso a datos de la entidad Reserva. Persiste en datos/reservas.dat.
 */
public class ReservaDAO extends ArchivoDAO<Reserva> {

    private static final String ARCHIVO = "reservas.dat";

    /** Construye el DAO apuntando a datos/reservas.dat. */
    public ReservaDAO() {
        super(ARCHIVO);
    }

    /**
     * Guarda la lista completa de reservas, sobrescribiendo la anterior.
     *
     * @param reservas lista de reservas a persistir
     */
    public void guardarReservas(List<Reserva> reservas) {
        guardarTodos(reservas);
    }

    /**
     * Carga todas las reservas persistidas.
     *
     * @return lista de reservas; vacía si aún no hay datos
     */
    public List<Reserva> cargarReservas() {
        return cargarTodos();
    }

    /**
     * Agrega una reserva a los datos persistidos.
     *
     * @param reserva reserva a agregar
     */
    public void guardarReserva(Reserva reserva) {
        agregar(reserva);
    }

    /**
     * Busca una reserva por su identificador único.
     *
     * @param id identificador de la reserva
     * @return la reserva si existe, null en caso contrario
     */
    public Reserva buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        for (Reserva reserva : cargarTodos()) {
            if (id.equals(reserva.getId())) {
                return reserva;
            }
        }
        return null;
    }

    /**
     * Reemplaza en disco la reserva que tenga el mismo id que la recibida.
     * Se usa, por ejemplo, tras cancelar una reserva.
     *
     * @param reservaActualizada reserva con los datos ya modificados
     * @return true si se encontró y actualizó, false si no existía ese id
     */
    public synchronized boolean actualizarReserva(Reserva reservaActualizada) {
        if (reservaActualizada == null) {
            return false;
        }
        List<Reserva> reservas = cargarReservas();
        for (int i = 0; i < reservas.size(); i++) {
            if (reservas.get(i).getId().equals(reservaActualizada.getId())) {
                reservas.set(i, reservaActualizada);
                guardarReservas(reservas);
                return true;
            }
        }
        return false;
    }
}
