package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato CRUD aplicado al DAO en memoria de reservas. */
class ReservaEnMemoriaTest extends CrudDAOContrato<Reserva, String> {

    private final Cliente cliente = new Cliente("Juan Pérez", "77771234", "juan@example.com");

    @Override
    protected CrudDAO<Reserva, String> crearDAO() {
        return new DAOEnMemoria.ReservaEnMemoria();
    }

    @Override
    protected Reserva nueva(int n) {
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 86_400_000L);
        return new Reserva(cliente, new HabitacionSencilla(100 + n, 100.0), entrada, salida);
    }

    @Override
    protected String idDe(Reserva r) {
        return r.getId();
    }

    @Override
    protected Reserva modificar(Reserva original) {
        original.cancelar();
        return original;
    }

    @Override
    protected boolean estaModificada(Reserva r) {
        return Reserva.CANCELADA.equals(r.getEstado());
    }

    @Test
    @DisplayName("Una reserva cargada conserva el número de habitación pero no el objeto (transient)")
    void reservaCargadaNoTraeLaHabitacion() {
        Reserva r = nueva(1);
        r.agregarServicio(new ServicioAdicional("Desayuno", 15.0));
        dao.guardar(r);

        Reserva cargada = dao.buscarPorId(r.getId()).get();

        assertEquals(101, cargada.getNumeroHabitacion());
        assertNull(cargada.getHabitacion(), "GestorReservas es quien debe reenlazarla");
        assertEquals(1, cargada.getServicios().size());
        assertEquals("juan@example.com", cargada.getCliente().getEmail());
    }
}
