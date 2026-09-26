package com.hotel.servicio;

import com.hotel.dao.HabitacionDAO;
import com.hotel.dao.ReservaDAO;
import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.excepciones.PersistenciaException;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.HabitacionSencilla;
import com.hotel.modelo.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de GestorReservas: reglas de negocio, concurrencia y coherencia de
 * los datos tras reiniciar.
 *
 * <p>Los DAO de prueba no escriben en {@code datos/}, pero sí serializan de
 * verdad a un arreglo de bytes.
 * Un DAO que solo guardara la lista en memoria
 * devolvería los mismos objetos y no mostraria el error #1 que encontro QA; al
 * deserializar, cada carga reconstruye instancias nuevas, igual que un
 * reinicio.</p>
 */
class GestorReservasTest {

    private static byte[] serializar(Object objeto) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(objeto);
            salida.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> deserializar(byte[] datos) {
        if (datos == null) {
            return new ArrayList<T>();
        }
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(datos))) {
            return (List<T>) entrada.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class ReservaDAOEnMemoria extends ReservaDAO {
        private byte[] almacen;

        @Override
        public void guardarReservas(List<Reserva> reservas) {
            almacen = serializar(new ArrayList<Reserva>(reservas));
        }

        @Override
        public List<Reserva> cargarReservas() {
            return deserializar(almacen);
        }
    }

    private static class HabitacionDAOEnMemoria extends HabitacionDAO {
        private byte[] almacen;

        @Override
        public void guardarHabitaciones(List<Habitacion> habitaciones) {
            almacen = serializar(new ArrayList<Habitacion>(habitaciones));
        }

        @Override
        public List<Habitacion> cargarHabitaciones() {
            return deserializar(almacen);
        }
    }

    private ReservaDAOEnMemoria reservaDAO;
    private HabitacionDAOEnMemoria habitacionDAO;
    private GestorHabitaciones gestorHabitaciones;
    private GestorReservas gestor;
    private Cliente cliente;
    private Date entrada;
    private Date salida;

    @BeforeEach
    void prepararEscenario() {
        reservaDAO = new ReservaDAOEnMemoria();
        habitacionDAO = new HabitacionDAOEnMemoria();
        gestorHabitaciones = new GestorHabitaciones(habitacionDAO);
        gestor = new GestorReservas(reservaDAO, gestorHabitaciones);
        cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        entrada = new Date();
        salida = new Date(entrada.getTime() + 86_400_000L);
    }

    private Habitacion registrarHabitacion(int numero, double precio) {
        Habitacion habitacion = new HabitacionSencilla(numero, precio);
        assertTrue(gestorHabitaciones.agregarHabitacion(habitacion));
        return habitacion;
    }

    @Test
    @DisplayName("Crear una reserva marca la habitación como OCUPADA")
    void crearReservaOcupaLaHabitacion() throws Exception {
        Habitacion habitacion = registrarHabitacion(101, 100.0);

        Reserva reserva = gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertNotNull(reserva.getId());
        assertTrue(reserva.estaActiva());
        assertEquals(Habitacion.OCUPADA, habitacion.getEstado());
        assertEquals(101, reserva.getNumeroHabitacion());
        assertEquals(1, gestor.obtenerTotalReservas());
    }

    @Test
    @DisplayName("No se puede reservar una habitación ya ocupada")
    void rechazaHabitacionOcupada() throws Exception {
        Habitacion habitacion = registrarHabitacion(101, 100.0);
        gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertThrows(HabitacionNoDisponibleException.class,
                () -> gestor.crearReserva(cliente, habitacion, entrada, salida));
    }

    @Test
    @DisplayName("No se puede reservar una habitación que no está en el inventario")
    void rechazaHabitacionNoRegistrada() {
        Habitacion fueraDeInventario = new HabitacionSencilla(999, 100.0);

        assertThrows(IllegalArgumentException.class,
                () -> gestor.crearReserva(cliente, fueraDeInventario, entrada, salida));
    }

    @Test
    @DisplayName("La fecha de salida debe ser posterior a la de entrada")
    void rechazaFechasIncoherentes() {
        Habitacion habitacion = registrarHabitacion(101, 100.0);

        assertThrows(IllegalArgumentException.class,
                () -> gestor.crearReserva(cliente, habitacion, salida, entrada));
    }

    @Test
    @DisplayName("Cancelar libera la habitación y desactiva la reserva")
    void cancelarLiberaLaHabitacion() throws Exception {
        Habitacion habitacion = registrarHabitacion(101, 100.0);
        Reserva reserva = gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertTrue(gestor.cancelarReserva(reserva.getId()));
        assertFalse(reserva.estaActiva());
        assertTrue(habitacion.estaDisponible());
    }

    @Test
    @DisplayName("Cancelar dos veces la misma reserva no tiene efecto la segunda vez")
    void cancelarDosVecesNoRepiteEfecto() throws Exception {
        Habitacion habitacion = registrarHabitacion(101, 100.0);
        Reserva reserva = gestor.crearReserva(cliente, habitacion, entrada, salida);

        assertTrue(gestor.cancelarReserva(reserva.getId()));
        assertFalse(gestor.cancelarReserva(reserva.getId()));
    }

    @Test
    @DisplayName("QA #1: tras reiniciar, reserva e inventario comparten la misma habitación")
    void reinicioConservaUnaSolaHabitacion() throws Exception {
        Habitacion habitacion = registrarHabitacion(101, 100.0);
        Reserva reserva = gestor.crearReserva(cliente, habitacion, entrada, salida);

        // Gestores nuevos sobre los mismos DAO = cerrar y volver a abrir el programa.
        GestorHabitaciones inventarioReiniciado = new GestorHabitaciones(habitacionDAO);
        GestorReservas reservasReiniciado = new GestorReservas(reservaDAO, inventarioReiniciado);

        Habitacion delInventario = inventarioReiniciado.obtenerHabitacion(101);
        Reserva cargada = reservasReiniciado.obtenerReserva(reserva.getId());

        assertEquals(Habitacion.OCUPADA, delInventario.getEstado(),
                "El estado OCUPADA debe haberse persistido en el inventario");
        assertSame(delInventario, cargada.getHabitacion(),
                "La reserva debe apuntar al MISMO objeto que el inventario, no a una copia");

        assertTrue(reservasReiniciado.cancelarReserva(reserva.getId()));
        assertEquals(Habitacion.DISPONIBLE, delInventario.getEstado());

        // Segundo reinicio: comprueba que la liberación también llegó a disco.
        GestorHabitaciones inventarioFinal = new GestorHabitaciones(habitacionDAO);
        assertEquals(Habitacion.DISPONIBLE, inventarioFinal.obtenerHabitacion(101).getEstado());
    }

    @Test
    @DisplayName("QA #1: una reserva que apunta a una habitación inexistente se detecta al cargar")
    void reservaHuerfanaLanzaPersistenciaException() throws Exception {
        Habitacion habitacion = registrarHabitacion(101, 100.0);
        gestor.crearReserva(cliente, habitacion, entrada, salida);

        GestorHabitaciones inventarioVacio = new GestorHabitaciones(new HabitacionDAOEnMemoria());

        assertThrows(PersistenciaException.class,
                () -> new GestorReservas(reservaDAO, inventarioVacio));
    }

    @Test
    @DisplayName("CONCURRENCIA: 20 hilos compiten por una habitación, solo uno la obtiene")
    void soloUnHiloReservaLaMismaHabitacion() throws Exception {
        final Habitacion habitacion = registrarHabitacion(102, 150.0);
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
