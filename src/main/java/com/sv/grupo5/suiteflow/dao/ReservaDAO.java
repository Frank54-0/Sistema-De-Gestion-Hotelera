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
     * Guarda la reserva solo si su habitación sigue libre para esas fechas, de
     * forma atómica: verificar y guardar ocurren sin que otro hilo (u otro
     * programa conectado a la misma base de datos) pueda colarse en medio.
     *
     * <p>La implementación por defecto simplemente guarda, con candado de la JVM.
     * Sirve para los DAO de archivo y de memoria, que no tienen candado propio.
     * {@code ReservaJdbcDAO} la reemplaza por una transacción con
     * {@code SELECT ... FOR UPDATE}, cuyo candado vive en la base de datos y por
     * eso protege también entre procesos distintos.</p>
     *
     * @param reserva reserva a guardar
     * @throws HabitacionNoDisponibleException si la habitación está en
     *         mantenimiento, no existe o ya tiene una reserva activa que se solapa
     */
    default void guardarSiHabitacionLibre(Reserva reserva) throws HabitacionNoDisponibleException {
        synchronized (this) {
            guardar(reserva);
        }
    }
}
