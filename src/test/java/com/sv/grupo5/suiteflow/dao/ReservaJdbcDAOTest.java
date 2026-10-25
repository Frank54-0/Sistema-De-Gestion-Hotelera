package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.config.DataSourceConfig;
import com.sv.grupo5.suiteflow.config.InicializadorBaseDatos;
import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.Reserva;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReservaJdbcDAOTest {

    private DataSource dataSource;
    private HabitacionJdbcDAO habitacionDAO;
    private ReservaJdbcDAO reservaDAO;

    @BeforeEach
    void prepararBaseDeDatos() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dataSource = DataSourceConfig.crear("org.h2.Driver", url, "sa", "");
        InicializadorBaseDatos.inicializar(dataSource);   // crea tablas y las 6 habitaciones iniciales
        habitacionDAO = new HabitacionJdbcDAO(dataSource);
        reservaDAO = new ReservaJdbcDAO(dataSource);
    }

    @AfterEach
    void cerrarPool() {
        dataSource.close();
    }

    // Día de diciembre de 2026 (fecha fija para que las pruebas no dependan de "hoy").
    private static Date dia(int dia) {
        return java.sql.Date.valueOf(LocalDate.of(2026, 12, dia));
    }

    private Reserva reservaDe(int numeroHabitacion, int entrada, int salida) {
        Habitacion habitacion = habitacionDAO.buscarPorId(numeroHabitacion).orElseThrow();
        Cliente cliente = new Cliente("Cliente de prueba", "70000000", "prueba@correo.com");
        return new Reserva(cliente, habitacion, dia(entrada), dia(salida));
    }

    @Test
    void guardarSiHabitacionLibreGuardaLaReservaYOcupaLaHabitacion() throws Exception {
        Reserva reserva = reservaDe(101, 10, 12);

        reservaDAO.guardarSiHabitacionLibre(reserva);

        Reserva leida = reservaDAO.buscarPorId(reserva.getId()).orElseThrow();
        assertEquals(101, leida.getNumeroHabitacion());
        assertEquals(Reserva.ACTIVA, leida.getEstado());
        assertEquals(reserva.getCliente().getId(), leida.getCliente().getId());
        assertEquals("Cliente de prueba", leida.getCliente().getNombre());
        assertEquals(Habitacion.OCUPADA, habitacionDAO.buscarPorId(101).orElseThrow().getEstado());
    }

    @Test
    void rechazaUnaReservaCuyasFechasSeSolapan() throws Exception {
        reservaDAO.guardarSiHabitacionLibre(reservaDe(101, 10, 15));
        Reserva solapada = reservaDe(101, 12, 18);

        assertThrows(HabitacionNoDisponibleException.class,
                () -> reservaDAO.guardarSiHabitacionLibre(solapada));
        assertEquals(1, reservaDAO.listarTodos().size());
    }

    @Test
    void permiteElRelevoElMismoDia() throws Exception {
        reservaDAO.guardarSiHabitacionLibre(reservaDe(101, 10, 12));

        // Sale el 12 y otro huésped entra el 12: no se solapan.
        reservaDAO.guardarSiHabitacionLibre(reservaDe(101, 12, 15));

        assertEquals(2, reservaDAO.listarTodos().size());
    }

    @Test
    void rechazaUnaHabitacionEnMantenimiento() {
        Habitacion habitacion = habitacionDAO.buscarPorId(102).orElseThrow();
        habitacion.setEstado(Habitacion.MANTENIMIENTO);
        habitacionDAO.actualizar(habitacion);
        Reserva reserva = reservaDe(102, 10, 12);

        assertThrows(HabitacionNoDisponibleException.class,
                () -> reservaDAO.guardarSiHabitacionLibre(reserva));
        assertTrue(reservaDAO.listarTodos().isEmpty());
    }

    @Test
    void unaReservaCanceladaNoBloqueaLasFechas() throws Exception {
        Reserva primera = reservaDe(103, 10, 12);
        reservaDAO.guardarSiHabitacionLibre(primera);
        primera.cancelar();
        reservaDAO.actualizar(primera);

        assertEquals(Reserva.CANCELADA, reservaDAO.buscarPorId(primera.getId()).orElseThrow().getEstado());
        // Las mismas fechas ya se pueden reservar otra vez.
        reservaDAO.guardarSiHabitacionLibre(reservaDe(103, 10, 12));
        assertEquals(2, reservaDAO.listarTodos().size());
    }

    @Test
    void buscarPorIdDevuelveVacioSiNoExiste() {
        assertTrue(reservaDAO.buscarPorId("no-existe").isEmpty());
    }

    @Test
    void actualizarUnaReservaInexistenteLanzaPersistenciaException() {
        Reserva sinGuardar = reservaDe(104, 10, 12);

        assertThrows(PersistenciaException.class, () -> reservaDAO.actualizar(sinGuardar));
    }
}
