package com.sv.grupo5.suiteflow.service;

import com.sv.grupo5.suiteflow.dao.DAOEnMemoria;
import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.model.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos que complementan a GestorReservasTest: argumentos inválidos, estados
 * especiales de la habitación, ciclo cancelar-y-volver-a-reservar,
 * persistencia tras reiniciar y concurrencia adicional.
 */
class GestorReservasCasosBordeTest {

    private DAOEnMemoria.ReservaEnMemoria reservaDAO;
    private DAOEnMemoria.HabitacionEnMemoria habitacionDAO;
    private GestorHabitaciones gestorHabitaciones;
    private GestorReservas gestor;
    private Cliente cliente;
    private Date entrada;
    private Date salida;

    @BeforeEach
    void prepararEscenario() {
        reservaDAO = new DAOEnMemoria.ReservaEnMemoria();
        habitacionDAO = new DAOEnMemoria.HabitacionEnMemoria();
        gestorHabitaciones = new GestorHabitaciones(habitacionDAO);
        gestor = new GestorReservas(reservaDAO, gestorHabitaciones);
        cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        entrada = new Date();
        salida = new Date(entrada.getTime() + 86_400_000L);
    }

    private Habitacion registrarHabitacion(int numero) {
        Habitacion habitacion = new HabitacionSencilla(numero, 100.0);
        assertTrue(gestorHabitaciones.agregarHabitacion(habitacion));
        return habitacion;
    }

    // ---- Argumentos inválidos ------------------------------------------------

    @Test
    @DisplayName("crearReserva rechaza cliente, habitación o fechas null")
    void rechazaArgumentosNull() {
        Habitacion h = registrarHabitacion(101);

        assertThrows(IllegalArgumentException.class, () -> gestor.crearReserva(null, h, entrada, salida));
        assertThrows(IllegalArgumentException.class, () -> gestor.crearReserva(cliente, null, entrada, salida));
        assertThrows(IllegalArgumentException.class, () -> gestor.crearReserva(cliente, h, null, salida));
        assertThrows(IllegalArgumentException.class, () -> gestor.crearReserva(cliente, h, entrada, null));
        assertEquals(0, gestor.obtenerTotalReservas());
        assertTrue(h.estaDisponible(), "Una reserva rechazada no debe ocupar la habitación");
    }

    @Test
    @DisplayName("crearReserva rechaza fecha de salida igual a la de entrada")
    void rechazaFechasIguales() {
        Habitacion h = registrarHabitacion(101);

        assertThrows(IllegalArgumentException.class, () -> gestor.crearReserva(cliente, h, entrada, entrada));
        assertTrue(h.estaDisponible());
    }

    @Test
    @DisplayName("El constructor rechaza DAO o gestor de habitaciones null")
    void constructorRechazaNull() {
        assertThrows(IllegalArgumentException.class, () -> new GestorReservas(null, gestorHabitaciones));
        assertThrows(IllegalArgumentException.class, () -> new GestorReservas(reservaDAO, null));
    }

    // ---- Estados de la habitación -------------------------------------------

    @Test
    @DisplayName("No se puede reservar una habitación en MANTENIMIENTO")
    void rechazaHabitacionEnMantenimiento() {
        Habitacion h = registrarHabitacion(101);
        gestorHabitaciones.cambiarEstado(101, Habitacion.MANTENIMIENTO);

        assertThrows(HabitacionNoDisponibleException.class,
                () -> gestor.crearReserva(cliente, h, entrada, salida));

        assertEquals(0, gestor.obtenerTotalReservas());
        assertEquals(Habitacion.MANTENIMIENTO, h.getEstado(), "El estado no debe cambiar");
    }

    @Test
    @DisplayName("Una habitación cancelada puede volver a reservarse")
    void cancelarYVolverAReservar() throws Exception {
        Habitacion h = registrarHabitacion(101);
        Reserva primera = gestor.crearReserva(cliente, h, entrada, salida);
        assertTrue(gestor.cancelarReserva(primera.getId()));

        Reserva segunda = gestor.crearReserva(cliente, h, entrada, salida);

        assertNotEquals(primera.getId(), segunda.getId());
        assertEquals(2, gestor.obtenerTotalReservas());
        assertEquals(1, gestor.obtenerReservasActivas().size());
        assertEquals(Habitacion.OCUPADA, h.getEstado());
    }

    // ---- Consultas ------------------------------------------------------------

    @Test
    @DisplayName("cancelarReserva con id inexistente o null devuelve false")
    void cancelarInexistente() {
        assertFalse(gestor.cancelarReserva("no-existe"));
        assertFalse(gestor.cancelarReserva(null));
    }

    @Test
    @DisplayName("obtenerReserva con id inexistente o null devuelve null")
    void obtenerInexistente() {
        assertNull(gestor.obtenerReserva("no-existe"));
        assertNull(gestor.obtenerReserva(null));
    }

    @Test
    @DisplayName("obtenerReservasActivas excluye las canceladas pero el total las cuenta")
    void activasExcluyeCanceladas() throws Exception {
        Habitacion h1 = registrarHabitacion(101);
        Habitacion h2 = registrarHabitacion(102);
        Reserva r1 = gestor.crearReserva(cliente, h1, entrada, salida);
        gestor.crearReserva(cliente, h2, entrada, salida);

        gestor.cancelarReserva(r1.getId());

        assertEquals(1, gestor.obtenerReservasActivas().size());
        assertEquals(2, gestor.obtenerTotalReservas());
    }

    // ---- Persistencia ---------------------------------------------------------

    @Test
    @DisplayName("Tras reiniciar, una reserva cancelada sigue CANCELADA y su habitación DISPONIBLE")
    void cancelacionSobreviveAlReinicio() throws Exception {
        Habitacion h = registrarHabitacion(101);
        Reserva r = gestor.crearReserva(cliente, h, entrada, salida);
        gestor.cancelarReserva(r.getId());

        GestorHabitaciones inventario = new GestorHabitaciones(habitacionDAO);
        GestorReservas reiniciado = new GestorReservas(reservaDAO, inventario);

        assertEquals(Reserva.CANCELADA, reiniciado.obtenerReserva(r.getId()).getEstado());
        assertTrue(inventario.obtenerHabitacion(101).estaDisponible());
        assertEquals(0, reiniciado.obtenerReservasActivas().size());
    }

    @Test
    @DisplayName("Tras reiniciar, las reservas conservan cliente y fechas")
    void datosSobrevivenAlReinicio() throws Exception {
        Habitacion h = registrarHabitacion(101);
        Reserva r = gestor.crearReserva(cliente, h, entrada, salida);

        GestorReservas reiniciado = new GestorReservas(reservaDAO, new GestorHabitaciones(habitacionDAO));
        Reserva cargada = reiniciado.obtenerReserva(r.getId());

        assertEquals("Juan Pérez", cargada.getCliente().getNombre());
        assertEquals(entrada, cargada.getFechaEntrada());
        assertEquals(salida, cargada.getFechaSalida());
        assertEquals(100.0, cargada.calcularTotal(), 0.001);
    }

    // ---- Concurrencia adicional ----------------------------------------------

    @Test
    @DisplayName("CONCURRENCIA: 40 hilos sobre 10 habitaciones, exactamente 10 reservas exitosas")
    void variasHabitacionesEnCompetencia() throws Exception {
        final int habitaciones = 10;
        final int totalHilos = 40;
        for (int i = 0; i < habitaciones; i++) {
            registrarHabitacion(100 + i);
        }
        ExecutorService pool = Executors.newFixedThreadPool(totalHilos);
        final CountDownLatch salidaLatch = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < totalHilos; i++) {
            final int numero = 100 + (i % habitaciones);
            futuros.add(pool.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    salidaLatch.await();
                    try {
                        gestor.crearReserva(cliente, gestorHabitaciones.obtenerHabitacion(numero), entrada, salida);
                        return true;
                    } catch (HabitacionNoDisponibleException e) {
                        return false;
                    }
                }
            }));
        }
        salidaLatch.countDown();

        int exitos = 0;
        for (Future<Boolean> futuro : futuros) {
            if (futuro.get(30, TimeUnit.SECONDS)) {
                exitos++;
            }
        }
        pool.shutdown();

        assertEquals(habitaciones, exitos);
        assertEquals(habitaciones, gestor.obtenerTotalReservas());
        assertEquals(habitaciones, reservaDAO.listarTodos().size(), "Todas deben estar persistidas");
        assertEquals(habitaciones, gestorHabitaciones.obtenerHabitacionesOcupadas());
    }

    @Test
    @DisplayName("CONCURRENCIA: 20 hilos cancelando la misma reserva, solo uno lo logra")
    void soloUnHiloCancelaLaMismaReserva() throws Exception {
        Habitacion h = registrarHabitacion(101);
        final Reserva reserva = gestor.crearReserva(cliente, h, entrada, salida);
        final int totalHilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(totalHilos);
        final CountDownLatch salidaLatch = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < totalHilos; i++) {
            futuros.add(pool.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    salidaLatch.await();
                    return gestor.cancelarReserva(reserva.getId());
                }
            }));
        }
        salidaLatch.countDown();

        int exitos = 0;
        for (Future<Boolean> futuro : futuros) {
            if (futuro.get(30, TimeUnit.SECONDS)) {
                exitos++;
            }
        }
        pool.shutdown();

        assertEquals(1, exitos);
        assertTrue(h.estaDisponible());
    }

    // ---- Hallazgo conocido ----------------------------------------------------

    @Test
    @Disabled("Hallazgo #6 (pendiente de definición con el Architect): buscarDisponibilidad "
            + "solo recorre habitaciones que ya aparecen en alguna reserva y no evalúa fechas")
    @DisplayName("buscarDisponibilidad debería devolver las habitaciones libres del inventario")
    void buscarDisponibilidadDeberiaRecorrerElInventario() {
        registrarHabitacion(101);
        registrarHabitacion(102);

        List<Habitacion> disponibles = gestor.buscarDisponibilidad(entrada, salida);

        assertEquals(2, disponibles.size());
    }
}
