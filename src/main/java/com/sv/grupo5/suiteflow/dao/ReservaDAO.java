package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.model.Reserva;

/**
 * Contrato de acceso a datos de {@link Reserva}.
 *
 * <p>Hereda las operaciones CRUD de {@link CrudDAO}; el identificador de una
 * reserva es su id de tipo String.</p>
 */
public interface ReservaDAO extends CrudDAO<Reserva, String> {

    /**
     * Guarda la reserva solo si la habitación está libre en esas fechas, sin que
     * otro hilo se cuele entre la verificación y el guardado. Esta versión sirve
     * para los DAO de archivo y memoria; {@code ReservaJdbcDAO} usa una transacción.
     *
     * @param reserva reserva a guardar
     * @throws HabitacionNoDisponibleException si ya hay una reserva activa que se cruza
     */
    default void guardarSiHabitacionLibre(Reserva reserva) throws HabitacionNoDisponibleException {
        synchronized (this) {
            for (Reserva existente : listarTodos()) {
                if (existente.estaActiva()
                        && existente.getNumeroHabitacion() == reserva.getNumeroHabitacion()
                        && existente.seSolapaCon(reserva.getFechaEntrada(), reserva.getFechaSalida())) {
                    throw new HabitacionNoDisponibleException(
                            "La habitación " + reserva.getNumeroHabitacion()
                                    + " ya está reservada en esas fechas");
                }
            }
            guardar(reserva);
        }
    }
}
