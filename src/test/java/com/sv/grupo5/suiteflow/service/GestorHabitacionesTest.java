package com.sv.grupo5.suiteflow.service;

import com.sv.grupo5.suiteflow.dao.DAOEnMemoria;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionDoble;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.model.Suite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de GestorHabitaciones: inventario, estados, persistencia entre
 * reinicios y concurrencia. Usa un DAO en memoria que serializa de verdad.
 */
class GestorHabitacionesTest {

    private DAOEnMemoria.HabitacionEnMemoria dao;
    private GestorHabitaciones gestor;

    @BeforeEach
    void prepararEscenario() {
        dao = new DAOEnMemoria.HabitacionEnMemoria();
        gestor = new GestorHabitaciones(dao);
    }

    @Test
    @DisplayName("Un gestor nuevo con DAO vacío no tiene habitaciones")
    void inventarioInicialVacio() {
        assertEquals(0, gestor.obtenerTotalHabitaciones());
        assertTrue(gestor.obtenerHabitacionesDisponibles().isEmpty());
    }

    @Test
    @DisplayName("agregarHabitacion registra la habitación y se puede obtener por número")
    void agregarYObtener() {
        Habitacion h = new HabitacionSencilla(101, 100.0);

        assertTrue(gestor.agregarHabitacion(h));

        assertSame(h, gestor.obtenerHabitacion(101));
        assertEquals(1, gestor.obtenerTotalHabitaciones());
    }

    @Test
    @DisplayName("No se aceptan números de habitación duplicados")
    void rechazaDuplicados() {
        gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));

        assertFalse(gestor.agregarHabitacion(new Suite(101, 300.0)));

        assertEquals(1, gestor.obtenerTotalHabitaciones());
        assertEquals(1, dao.listarTodos().size(), "Tampoco debe persistirse el duplicado");
    }

    @Test
    @DisplayName("agregarHabitacion(null) devuelve false")
    void rechazaNull() {
        assertFalse(gestor.agregarHabitacion(null));
        assertEquals(0, gestor.obtenerTotalHabitaciones());
    }

    @Test
    @DisplayName("obtenerHabitacion de un número inexistente devuelve null")
    void obtenerInexistente() {
        assertNull(gestor.obtenerHabitacion(999));
    }

    @Test
    @DisplayName("cambiarEstado actualiza la habitación y devuelve true")
    void cambiarEstado() {
        gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));

        assertTrue(gestor.cambiarEstado(101, Habitacion.MANTENIMIENTO));

        assertEquals(Habitacion.MANTENIMIENTO, gestor.obtenerHabitacion(101).getEstado());
    }

    @Test
    @DisplayName("cambiarEstado de una habitación inexistente devuelve false")
    void cambiarEstadoInexistente() {
        assertFalse(gestor.cambiarEstado(999, Habitacion.OCUPADA));
    }

    @Test
    @DisplayName("cambiarEstado rechaza estados inválidos sin alterar la habitación ni lo persistido")
    void cambiarEstadoInvalido() {
        gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));

        assertThrows(IllegalArgumentException.class, () -> gestor.cambiarEstado(101, "LIBRE"));

        assertEquals(Habitacion.DISPONIBLE, gestor.obtenerHabitacion(101).getEstado());
        assertEquals(Habitacion.DISPONIBLE, dao.buscarPorId(101).get().getEstado());
    }

    @Test
    @DisplayName("Los cambios de estado sobreviven a un reinicio del gestor")
    void estadoSobreviveAlReinicio() {
        gestor.agregarHabitacion(new HabitacionDoble(102, 150.0));
        gestor.cambiarEstado(102, Habitacion.OCUPADA);

        GestorHabitaciones reiniciado = new GestorHabitaciones(dao);

        assertEquals(Habitacion.OCUPADA, reiniciado.obtenerHabitacion(102).getEstado());
    }

    @Test
    @DisplayName("Tras reiniciar, cada habitación conserva su subclase (polimorfismo)")
    void tipoSobreviveAlReinicio() {
        gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));
        gestor.agregarHabitacion(new HabitacionDoble(102, 150.0));
        gestor.agregarHabitacion(new Suite(201, 300.0));

        GestorHabitaciones reiniciado = new GestorHabitaciones(dao);

        assertTrue(reiniciado.obtenerHabitacion(101) instanceof HabitacionSencilla);
        assertTrue(reiniciado.obtenerHabitacion(102) instanceof HabitacionDoble);
        assertTrue(reiniciado.obtenerHabitacion(201) instanceof Suite);
    }

    @Test
    @DisplayName("Solo las habitaciones DISPONIBLES aparecen como disponibles")
    void soloDisponibles() {
        gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));
        gestor.agregarHabitacion(new HabitacionDoble(102, 150.0));
        gestor.agregarHabitacion(new Suite(201, 300.0));
        gestor.cambiarEstado(102, Habitacion.OCUPADA);
        gestor.cambiarEstado(201, Habitacion.MANTENIMIENTO);

        List<Habitacion> disponibles = gestor.obtenerHabitacionesDisponibles();

        assertEquals(1, disponibles.size());
        assertEquals(101, disponibles.get(0).getNumero());
    }

    @Test
    @DisplayName("obtenerHabitacionesOcupadas cuenta solo las OCUPADAS (no las de mantenimiento)")
    void cuentaOcupadas() {
        gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));
        gestor.agregarHabitacion(new HabitacionDoble(102, 150.0));
        gestor.agregarHabitacion(new Suite(201, 300.0));
        gestor.cambiarEstado(101, Habitacion.OCUPADA);
        gestor.cambiarEstado(201, Habitacion.MANTENIMIENTO);

        assertEquals(1, gestor.obtenerHabitacionesOcupadas());
        assertEquals(3, gestor.obtenerTotalHabitaciones());
    }

    @Test
    @DisplayName("La lista completa expuesta es de solo lectura")
    void listaInmutable() {
        gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));

        assertThrows(UnsupportedOperationException.class,
                () -> gestor.obtenerTodasLasHabitaciones().clear());
    }

    @Test
    @DisplayName("El constructor rechaza un DAO null")
    void constructorRechazaDaoNull() {
        assertThrows(IllegalArgumentException.class, () -> new GestorHabitaciones(null));
    }

    @Test
    @DisplayName("CONCURRENCIA: 20 hilos agregando el MISMO número, solo uno lo logra")
    void soloUnHiloAgregaElMismoNumero() throws Exception {
        final int totalHilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(totalHilos);
        final CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < totalHilos; i++) {
            futuros.add(pool.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    salida.await();
                    return gestor.agregarHabitacion(new HabitacionSencilla(101, 100.0));
                }
            }));
        }
        salida.countDown();

        int exitos = 0;
        for (Future<Boolean> futuro : futuros) {
            if (futuro.get(30, TimeUnit.SECONDS)) {
                exitos++;
            }
        }
        pool.shutdown();

        assertEquals(1, exitos);
        assertEquals(1, gestor.obtenerTotalHabitaciones());
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    @DisplayName("CONCURRENCIA: 30 habitaciones distintas agregadas a la vez quedan todas persistidas")
    void agregadoConcurrenteDeHabitacionesDistintas() throws Exception {
        final int total = 30;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        final CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<Future<Boolean>>();

        for (int i = 0; i < total; i++) {
            final int numero = 100 + i;
            futuros.add(pool.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    salida.await();
                    return gestor.agregarHabitacion(new HabitacionSencilla(numero, 100.0));
                }
            }));
        }
        salida.countDown();
        for (Future<Boolean> futuro : futuros) {
            assertTrue(futuro.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(total, gestor.obtenerTotalHabitaciones());
        assertEquals(total, dao.listarTodos().size());
    }
}
