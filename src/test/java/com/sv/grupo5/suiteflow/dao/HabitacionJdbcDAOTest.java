package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.config.DataSourceConfig;
import com.sv.grupo5.suiteflow.config.InicializadorBaseDatos;
import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionDoble;
import com.sv.grupo5.suiteflow.model.Suite;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HabitacionJdbcDAOTest {

    private DataSource dataSource;
    private HabitacionJdbcDAO dao;

    @BeforeEach
    void prepararBaseDeDatos() {
        // Una base H2 en memoria distinta por prueba, con la sintaxis de MySQL.
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dataSource = DataSourceConfig.crear("org.h2.Driver", url, "sa", "");
        InicializadorBaseDatos.inicializar(dataSource);   // crea tablas y las 6 habitaciones iniciales
        dao = new HabitacionJdbcDAO(dataSource);
    }

    @AfterEach
    void cerrarPool() {
        dataSource.close();
    }

    @Test
    void listarTodosDevuelveLasSeisIniciales_ConSuSubclase() {
        List<Habitacion> habitaciones = dao.listarTodos();

        assertEquals(6, habitaciones.size());
        assertTrue(dao.buscarPorId(201).orElseThrow() instanceof Suite);
        assertEquals(300.0, dao.buscarPorId(201).orElseThrow().getPrecioBase(), 0.001);
    }

    @Test
    void guardarYBuscarReconstruyeLaSubclase() {
        dao.guardar(new HabitacionDoble(301, 180.0));

        Habitacion leida = dao.buscarPorId(301).orElseThrow();
        assertTrue(leida instanceof HabitacionDoble);
        assertEquals(180.0, leida.getPrecioBase(), 0.001);
        assertEquals(Habitacion.DISPONIBLE, leida.getEstado());
    }

    @Test
    void actualizarPersisteElNuevoEstado() {
        Habitacion h = dao.buscarPorId(101).orElseThrow();
        h.setEstado(Habitacion.OCUPADA);
        dao.actualizar(h);

        assertEquals(Habitacion.OCUPADA, dao.buscarPorId(101).orElseThrow().getEstado());
    }

    @Test
    void actualizarUnaHabitacionInexistenteLanzaPersistenciaException() {
        assertThrows(PersistenciaException.class, () -> dao.actualizar(new Suite(999, 10.0)));
    }

    @Test
    void eliminarDevuelveTrueSiExisteYFalseSiNo() {
        assertTrue(dao.eliminar(104));
        assertFalse(dao.eliminar(104));
        assertTrue(dao.buscarPorId(104).isEmpty());
    }
}
