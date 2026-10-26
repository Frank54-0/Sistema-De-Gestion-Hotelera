package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.config.DataSourceConfig;
import com.sv.grupo5.suiteflow.config.InicializadorBaseDatos;
import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.Reserva;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClienteJdbcDAOTest {

    private DataSource dataSource;
    private ClienteJdbcDAO dao;

    @BeforeEach
    void prepararBaseDeDatos() {
        // Una base H2 en memoria distinta por prueba, con la sintaxis de MySQL.
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dataSource = DataSourceConfig.crear("org.h2.Driver", url, "sa", "");
        InicializadorBaseDatos.inicializar(dataSource);
        dao = new ClienteJdbcDAO(dataSource);
    }

    @AfterEach
    void cerrarPool() {
        dataSource.close();
    }

    @Test
    void guardarYBuscarConservaElIdYLosDatos() {
        Cliente guardado = new Cliente("Juan Pérez", "77771234", "juan@example.com");
        dao.guardar(guardado);

        Cliente leido = dao.buscarPorId(guardado.getId()).orElseThrow();
        assertEquals(guardado.getId(), leido.getId());
        assertEquals("Juan Pérez", leido.getNombre());
        assertEquals("77771234", leido.getTelefono());
        assertEquals("juan@example.com", leido.getEmail());
    }

    @Test
    void buscarUnIdInexistenteODeNullDevuelveVacio() {
        assertTrue(dao.buscarPorId("no-existe").isEmpty());
        assertTrue(dao.buscarPorId(null).isEmpty());
    }

    @Test
    void guardarElMismoIdDosVecesLanzaPersistenciaException() {
        Cliente cliente = new Cliente("Ana López", "70001111", "ana@example.com");
        dao.guardar(cliente);

        assertThrows(PersistenciaException.class, () -> dao.guardar(cliente));
    }

    @Test
    void actualizarPersisteLosCambios() {
        Cliente cliente = new Cliente("Ana López", "70001111", "ana@example.com");
        dao.guardar(cliente);

        cliente.setTelefono("70002222");
        dao.actualizar(cliente);

        assertEquals("70002222", dao.buscarPorId(cliente.getId()).orElseThrow().getTelefono());
    }

    @Test
    void actualizarUnClienteInexistenteLanzaPersistenciaException() {
        Cliente fantasma = new Cliente("Luis Gómez", "70003333", null);

        assertThrows(PersistenciaException.class, () -> dao.actualizar(fantasma));
    }

    @Test
    void listarTodosDevuelveLosGuardados() {
        dao.guardar(new Cliente("Carlos Ruiz", "70004444", null));
        dao.guardar(new Cliente("Beatriz Mena", "70005555", "bea@example.com"));

        List<Cliente> clientes = dao.listarTodos();

        assertEquals(2, clientes.size());
        assertEquals("Beatriz Mena", clientes.get(0).getNombre());   
    }

    @Test
    void eliminarDevuelveTrueSiExisteYFalseSiNo() {
        Cliente cliente = new Cliente("Diana Cruz", "70006666", null);
        dao.guardar(cliente);

        assertTrue(dao.eliminar(cliente.getId()));
        assertFalse(dao.eliminar(cliente.getId()));
        assertFalse(dao.eliminar(null));
    }

    @Test
    void eliminarUnClienteConReservasLanzaPersistenciaException() {
        Cliente cliente = new Cliente("Elena Paz", "70007777", null);
        Habitacion habitacion = new HabitacionJdbcDAO(dataSource).buscarPorId(101).orElseThrow();
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 2 * 86_400_000L);
        new ReservaJdbcDAO(dataSource).guardar(new Reserva(cliente, habitacion, entrada, salida));

        assertThrows(PersistenciaException.class, () -> dao.eliminar(cliente.getId()));
    }
}
