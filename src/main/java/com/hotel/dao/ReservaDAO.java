package com.hotel.dao;

import com.hotel.modelo.Reserva;

import java.util.List;

/**
 * Acceso a datos de Reserva, persistido en {@code datos/reservas.dat}.
 *
 * <p>Las reservas se devuelven con la habitación en null (el atributo es
 * transient); el reenlace con el inventario lo hace GestorReservas.</p>
 */
public class ReservaDAO extends ArchivoDAO<Reserva> {

    private static final String ARCHIVO = "reservas.dat";

    /** Crea el DAO apuntando a {@code datos/reservas.dat}. */
    public ReservaDAO() {
        super(ARCHIVO);
    }

    /**
     * Sobrescribe el archivo con la lista completa de reservas.
     *
     * @param reservas reservas a persistir
     */
    public void guardarReservas(List<Reserva> reservas) {
        guardarTodos(reservas);
    }

    /** @return reservas persistidas; vacía si aún no hay datos */
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
     * Busca directamente en disco. La reserva devuelve null en getHabitacion()
     * porque no pasa por GestorReservas; para uso normal preferir
     * GestorReservas.obtenerReserva().
     *
     * @param id identificador de la reserva
     * @return la reserva, o null si no existe
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
}
