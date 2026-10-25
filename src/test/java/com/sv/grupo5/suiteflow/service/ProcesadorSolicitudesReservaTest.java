package com.sv.grupo5.suiteflow.service;

import com.sv.grupo5.suiteflow.config.DataSourceConfig;
import com.sv.grupo5.suiteflow.config.InicializadorBaseDatos;
import com.sv.grupo5.suiteflow.controller.ReservaController;
import com.sv.grupo5.suiteflow.dao.HabitacionJdbcDAO;
import com.sv.grupo5.suiteflow.dao.ReservaJdbcDAO;
import com.sv.grupo5.suiteflow.dto.NuevaReservaDTO;
import com.sv.grupo5.suiteflow.dto.ResultadoSolicitudDTO;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del procesador de lotes con gestores y DAO JDBC reales sobre H2 (modo MySQL). */
class ProcesadorSolicitudesReservaTest {

    private DataSource dataSource;
    private ReservaJdbcDAO reservaDAO;
    private ReservaController controlador;
    private Date entrada;
    private Date salida;

    @BeforeEach
    void preparar() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dataSource = DataSourceConfig.crear("org.h2.Driver", url, "sa", "");
        InicializadorBaseDatos.inicializar(dataSource);
        GestorHabitaciones gestorHabitaciones = new GestorHabitaciones(new HabitacionJdbcDAO(dataSource));
        reservaDAO = new ReservaJdbcDAO(dataSource);
        GestorReservas gestorReservas = new GestorReservas(reservaDAO, gestorHabitaciones);
        controlador = new ReservaController(gestorReservas, gestorHabitaciones);
        entrada = new Date();
        salida = new Date(entrada.getTime() + 86_400_000L);
    }

    @AfterEach
    void cerrar() {
        dataSource.close();
    }

    private NuevaReservaDTO solicitud(int i, int habitacion) {
        return new NuevaReservaDTO("Cliente Prueba", "7777000" + i, "c" + i + "@mail.com",
                habitacion, entrada, salida);
    }

    private long exitosas(List<ResultadoSolicitudDTO> resultados) {
        return resultados.stream().filter(ResultadoSolicitudDTO::isExitosa).count();
    }

    @Test
    void ochoSolicitudesALaMismaHabitacionDejanUnaSolaReserva() throws Exception {
        List<NuevaReservaDTO> lote = new ArrayList<NuevaReservaDTO>();
        for (int i = 0; i < 8; i++) {
            lote.add(solicitud(i, 101));
        }
        try (ProcesadorSolicitudesReserva procesador = new ProcesadorSolicitudesReserva(controlador, 8)) {
            List<ResultadoSolicitudDTO> resultados = procesador.procesarLote(lote);
            assertEquals(8, resultados.size());
            assertEquals(1, exitosas(resultados));
        }
        assertEquals(1, reservaDAO.listarTodos().size());
    }

    @Test
    void habitacionesDistintasTienenExitoYLaInexistenteSeRechaza() throws Exception {
        List<NuevaReservaDTO> lote = new ArrayList<NuevaReservaDTO>();
        lote.add(solicitud(1, 101));
        lote.add(solicitud(2, 102));
        lote.add(solicitud(3, 201));
        lote.add(solicitud(4, 999));
        try (ProcesadorSolicitudesReserva procesador = new ProcesadorSolicitudesReserva(controlador, 4)) {
            List<ResultadoSolicitudDTO> resultados = procesador.procesarLote(lote);
            assertEquals(3, exitosas(resultados));
            assertFalse(resultados.get(3).isExitosa());
            assertNotNull(resultados.get(3).getMotivoRechazo());
        }
        assertEquals(3, reservaDAO.listarTodos().size());
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ProcesadorSolicitudesReserva(null, 2));
        assertThrows(IllegalArgumentException.class, () -> new ProcesadorSolicitudesReserva(controlador, 0));
    }
}
