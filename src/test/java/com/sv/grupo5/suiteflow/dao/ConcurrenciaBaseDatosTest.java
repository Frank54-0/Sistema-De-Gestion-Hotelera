package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.config.DataSourceConfig;
import com.sv.grupo5.suiteflow.config.InicializadorBaseDatos;
import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.Reserva;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comprueba que el candado de la base de datos (SELECT ... FOR UPDATE) evita la
 * doble reserva cuando varios hilos, cada uno con su propia conexión del pool,
 * piden la misma habitación al mismo tiempo.
 */
class ConcurrenciaBaseDatosTest {

    private DataSource dataSource;
    private HabitacionJdbcDAO habitacionDAO;
    private ReservaJdbcDAO reservaDAO;
    private ExecutorService ejecutor;

    @BeforeEach
    void prepararBaseDeDatos() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dataSource = DataSourceConfig.crear("org.h2.Driver", url, "sa", "");
        InicializadorBaseDatos.inicializar(dataSource);
        habitacionDAO = new HabitacionJdbcDAO(dataSource);
        reservaDAO = new ReservaJdbcDAO(dataSource);
        ejecutor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void cerrarTodo() {
        ejecutor.shutdownNow();
        dataSource.close();
    }

    private Reserva reservaDe(int numeroHabitacion) {
        Habitacion habitacion = habitacionDAO.buscarPorId(numeroHabitacion).orElseThrow();
        Cliente cliente = new Cliente("Cliente Prueba", "70000000", "prueba@correo.com");
        Date entrada = java.sql.Date.valueOf(LocalDate.of(2026, 12, 10));
        Date salida = java.sql.Date.valueOf(LocalDate.of(2026, 12, 13));
        return new Reserva(cliente, habitacion, entrada, salida);
    }

    // Lanza una tarea por reserva; todas esperan la señal para arrancar al mismo tiempo.
    // Devuelve cuántas lograron reservar.
    private int reservarEnParalelo(List<Reserva> reservas) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<Future<Boolean>>();
        for (Reserva reserva : reservas) {
            resultados.add(ejecutor.submit(() -> {
                salida.await();
                try {
                    reservaDAO.guardarSiHabitacionLibre(reserva);
                    return true;
                } catch (HabitacionNoDisponibleException e) {
                    return false;
                }
            }));
        }
        salida.countDown();

        int exitosas = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get(30, TimeUnit.SECONDS)) {
                exitosas++;
            }
        }
        return exitosas;
    }

    @Test
    void diezHilosPidenLaMismaHabitacionYSoloUnoLaConsigue() throws Exception {
        List<Reserva> reservas = new ArrayList<Reserva>();
        for (int i = 0; i < 10; i++) {
            reservas.add(reservaDe(101));
        }

        int exitosas = reservarEnParalelo(reservas);

        assertEquals(1, exitosas);
        assertEquals(1, reservaDAO.listarTodos().size());
    }

    @Test
    void habitacionesDistintasNoSeBloqueanEntreSi() throws Exception {
        List<Reserva> reservas = new ArrayList<Reserva>();
        for (int numero : new int[] {101, 102, 103, 104, 201, 202}) {
            reservas.add(reservaDe(numero));
        }

        int exitosas = reservarEnParalelo(reservas);

        assertEquals(6, exitosas);
        assertEquals(6, reservaDAO.listarTodos().size());
    }
}
