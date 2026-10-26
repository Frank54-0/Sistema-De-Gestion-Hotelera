package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.config.DataSourceConfig;
import com.sv.grupo5.suiteflow.config.InicializadorBaseDatos;
import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Factura;
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

class FacturaJdbcDAOTest {

    private DataSource dataSource;
    private FacturaJdbcDAO dao;

    @BeforeEach
    void prepararBaseDeDatos() {
        // Una base H2 en memoria distinta por prueba, con la sintaxis de MySQL.
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dataSource = DataSourceConfig.crear("org.h2.Driver", url, "sa", "");
        InicializadorBaseDatos.inicializar(dataSource);   // crea tablas y las 6 habitaciones iniciales
        dao = new FacturaJdbcDAO(dataSource);
    }

    @AfterEach
    void cerrarPool() {
        dataSource.close();
    }

    // La factura necesita una reserva ya guardada (llave foránea), y la reserva una habitación.
    private Reserva reservaGuardada(int numeroHabitacion, String nombreCliente) {
        Habitacion habitacion = new HabitacionJdbcDAO(dataSource)
                .buscarPorId(numeroHabitacion).orElseThrow();
        Cliente cliente = new Cliente(nombreCliente, "77771234", "cliente@example.com");
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 2 * 86_400_000L);
        Reserva reserva = new Reserva(cliente, habitacion, entrada, salida);
        new ReservaJdbcDAO(dataSource).guardar(reserva);
        return reserva;
    }

    @Test
    void guardarYBuscarReconstruyeLaFacturaConSuReservaYCliente() {
        Reserva reserva = reservaGuardada(101, "Juan Pérez");
        Factura guardada = new Factura(reserva, "TARJETA");
        dao.guardar(guardada);

        Factura leida = dao.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals(guardada.getId(), leida.getId());
        assertEquals(guardada.getTotal(), leida.getTotal(), 0.001);
        assertEquals("TARJETA", leida.getFormaPago());
        assertEquals(Factura.PENDIENTE, leida.getEstado());
        assertEquals(reserva.getId(), leida.getReserva().getId());
        assertEquals("Juan Pérez", leida.getReserva().getCliente().getNombre());
    }

    @Test
    void buscarUnIdInexistenteODeNullDevuelveVacio() {
        assertTrue(dao.buscarPorId("no-existe").isEmpty());
        assertTrue(dao.buscarPorId(null).isEmpty());
    }

    @Test
    void facturarDosVecesLaMismaReservaLanzaPersistenciaException() {
        Reserva reserva = reservaGuardada(101, "Ana López");
        dao.guardar(new Factura(reserva, "EFECTIVO"));

        assertThrows(PersistenciaException.class,
                () -> dao.guardar(new Factura(reserva, "TARJETA")));
    }

    @Test
    void guardarUnaFacturaDeUnaReservaNoGuardadaLanzaPersistenciaException() {
        Habitacion habitacion = new HabitacionJdbcDAO(dataSource).buscarPorId(102).orElseThrow();
        Cliente cliente = new Cliente("Luis Gómez", "70003333", null);
        Date entrada = new Date();
        Reserva sinGuardar = new Reserva(cliente, habitacion, entrada,
                new Date(entrada.getTime() + 86_400_000L));

        assertThrows(PersistenciaException.class,
                () -> dao.guardar(new Factura(sinGuardar, "EFECTIVO")));
    }

    @Test
    void actualizarPersisteElNuevoEstado() {
        Factura factura = new Factura(reservaGuardada(101, "Carlos Ruiz"), "EFECTIVO");
        dao.guardar(factura);

        factura.generarFactura();
        dao.actualizar(factura);

        assertEquals(Factura.PROCESADA, dao.buscarPorId(factura.getId()).orElseThrow().getEstado());
    }

    @Test
    void actualizarUnaFacturaInexistenteLanzaPersistenciaException() {
        Factura fantasma = new Factura(reservaGuardada(101, "Beatriz Mena"), "EFECTIVO");

        assertThrows(PersistenciaException.class, () -> dao.actualizar(fantasma));
    }

    @Test
    void listarTodosDevuelveLasGuardadas() {
        dao.guardar(new Factura(reservaGuardada(101, "Diana Cruz"), "TARJETA"));
        dao.guardar(new Factura(reservaGuardada(102, "Elena Paz"), "EFECTIVO"));

        List<Factura> facturas = dao.listarTodos();

        assertEquals(2, facturas.size());
    }

    @Test
    void eliminarDevuelveTrueSiExisteYFalseSiNo() {
        Factura factura = new Factura(reservaGuardada(101, "Fabio Cruz"), "TARJETA");
        dao.guardar(factura);

        assertTrue(dao.eliminar(factura.getId()));
        assertFalse(dao.eliminar(factura.getId()));
        assertFalse(dao.eliminar(null));
    }
}
