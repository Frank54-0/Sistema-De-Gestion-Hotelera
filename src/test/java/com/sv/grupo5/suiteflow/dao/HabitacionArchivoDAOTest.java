package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato CRUD y garantías de archivo aplicados a HabitacionArchivoDAO. */
class HabitacionArchivoDAOTest extends ArchivoDAOContrato<Habitacion, Integer> {

    @Override
    protected CrudDAO<Habitacion, Integer> crearDAO() {
        return new HabitacionArchivoDAO();
    }

    @Override
    protected Habitacion nueva(int n) {
        return new HabitacionSencilla(100 + n, 100.0);
    }

    @Override
    protected String nombreArchivo() {
        return "habitaciones.dat";
    }

    @Override
    protected Integer idDe(Habitacion h) {
        return h.getNumero();
    }

    @Override
    protected Habitacion modificar(Habitacion original) {
        return new Suite(original.getNumero(), 300.0);
    }

    @Override
    protected boolean estaModificada(Habitacion h) {
        return h instanceof Suite && h.getPrecioBase() == 300.0;
    }

    @Test
    @DisplayName("Las subclases se recuperan con su tipo real (polimorfismo)")
    void subclasesConservanSuTipo() {
        dao.guardar(new HabitacionSencilla(101, 100.0));
        dao.guardar(new HabitacionDoble(102, 150.0));
        dao.guardar(new Suite(201, 300.0));

        assertEquals(HabitacionSencilla.class, dao.buscarPorId(101).get().getClass());
        assertEquals(HabitacionDoble.class, dao.buscarPorId(102).get().getClass());
        assertEquals(Suite.class, dao.buscarPorId(201).get().getClass());
    }

    @Test
    @DisplayName("El estado de la habitación se conserva al actualizar")
    void actualizarConservaEstado() {
        Habitacion h = new HabitacionDoble(102, 150.0);
        dao.guardar(h);

        h.setEstado(Habitacion.OCUPADA);
        dao.actualizar(h);

        assertEquals(Habitacion.OCUPADA, dao.buscarPorId(102).get().getEstado());
    }
}
