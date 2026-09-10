package com.hotel.servicio;

import com.hotel.dao.ReservaDAO;
import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.HabitacionSencilla;
import com.hotel.modelo.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de GestorReservas, incluyendo la garantía de que dos hilos no
 * pueden reservar la misma habitación.
 *
 * Se usa un ReservaDAO en memoria para no escribir en datos/reservas.dat
 * durante las pruebas.
 */
class GestorReservasTest {

    /** DAO de prueba: guarda en memoria en vez de en disco. */
    private static class ReservaDAOEnMemoria extends ReservaDAO {
        private List<Reserva> almacen = new ArrayList<Reserva>();

        @Override
        public void guardarReservas(List<Reserva> reservas) {
            this.almacen = new ArrayList<Reserva>(reservas);
        }

        @Override
        public List<Reserva> cargarReservas() {
            return new ArrayList<Reserva>(almacen);
        }
    }

    private GestorReservas gestor;
    private Cliente cliente;
    private Date entrada;
    private Date salida;

    @BeforeEach
    void prepararEscenario() {
        gestor = new GestorReservas(new ReservaDAOEnMemoria());
        cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        entrada = new Date();
        salida = new Date(entrada.getTime() + 86_400_000L);
    }

    @Test
    @DisplayName("Crear una reserva marca la habitación como OCUPADA")
    void crearReservaOcupaLaHabitacion() throws Exception {
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);

        Reserva reserva = gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertNotNull(reserva.getId());
        assertTrue(reserva.estaActiva());
        assertEquals(Habitacion.OCUPADA, habitacion.getEstado());
        assertEquals(1, gestor.obtenerTotalReservas());
    }

    @Test
    @DisplayName("No se puede reservar una habitación ya ocupada")
    void rechazaHabitacionOcupada() throws Exception {
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertThrows(HabitacionNoDisponibleException.class,
                () -> gestor.crearReserva(cliente, habitacion, entrada, salida));
    }

    @Test
    @DisplayName("La fecha de salida debe ser posterior a la de entrada")
    void rechazaFechasIncoherentes() {
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);

        assertThrows(IllegalArgumentException.class,
                () -> gestor.crearReserva(cliente, habitacion, salida, entrada));
    }

    @Test
    @DisplayName("Cancelar libera la habitación y desactiva la reserva")
    void cancelarLiberaLaHabitacion() throws Exception {
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        Reserva reserva = gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertTrue(gestor.cancelarReserva(reserva.getId()));
        assertFalse(reserva.estaActiva());
        assertTrue(habitacion.estaDisponible());
    }

    @Test
    @DisplayName("Cancelar dos veces la misma reserva no tiene efecto la segunda vez")
    void cancelarDosVecesNoRepiteEfecto() throws Exception {
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        Reserva reserva = gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertTrue(gestor.cancelarReserva(reserva.getId()));
        assertFalse(gestor.cancelarReserva(reserva.getId()));
    }

    @Test
    @DisplayName("CONCURRENCIA: 20 hilos compiten por una habitación, solo uno la obtiene")
    void soloUnHiloReservaLaMismaHabitacion() throws Exception {
        final Habitacion habitacion = new HabitacionSencilla(102, 150.0);
        final int totalHilos = 20;
        final CountDownLatch listos = new CountDownLatch(1);
        final CountDownLatch terminados = new CountDownLatch(totalHilos);
        final AtomicInteger exitos = new AtomicInteger(0);
        final AtomicInteger rechazos = new AtomicInteger(0);

        for (int i = 0; i < totalHilos; i++) {
            new Thread(() -> {
                try {
                    listos.await(); // todos arrancan a la vez: maximiza la competencia
                    gestor.crearReserva(cliente, habitacion, entrada, salida);
                    exitos.incrementAndGet();
                } catch (HabitacionNoDisponibleException e) {
                    rechazos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    terminados.countDown();
                }
            }).start();
        }

        listos.countDown();
        terminados.await();

        assertEquals(1, exitos.get(), "Exactamente un hilo debe lograr la reserva");
        assertEquals(totalHilos - 1, rechazos.get());
        assertEquals(1, gestor.obtenerTotalReservas());
    }

    @Test
    @DisplayName("La lista de reservas expuesta es de solo lectura")
    void listaDeReservasEsInmutable() {
        assertThrows(UnsupportedOperationException.class,
                () -> gestor.obtenerTodasLasReservas().clear());
    }
}
