package com.sv.grupo5.suiteflow.controller;

import com.sv.grupo5.suiteflow.dao.DAOEnMemoria;
import com.sv.grupo5.suiteflow.dto.NuevaReservaDTO;
import com.sv.grupo5.suiteflow.dto.ReservaResumenDTO;
import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionDoble;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.model.Reserva;
import com.sv.grupo5.suiteflow.model.Suite;
import com.sv.grupo5.suiteflow.service.GestorHabitaciones;
import com.sv.grupo5.suiteflow.service.GestorReservas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de ReservaController. Se usan los gestores reales con DAO en memoria:
 * así se valida el flujo completo Controller -> Servicio -> DAO sin tocar disco.
 */
class ReservaControllerTest {

    private GestorHabitaciones gestorHabitaciones;
    private GestorReservas gestorReservas;
    private ReservaController controller;
    private Date entrada;
    private Date salida;

    @BeforeEach
    void prepararEscenario() {
        gestorHabitaciones = new GestorHabitaciones(new DAOEnMemoria.HabitacionEnMemoria());
        gestorHabitaciones.agregarHabitacion(new HabitacionSencilla(101, 100.0));
        gestorHabitaciones.agregarHabitacion(new HabitacionDoble(102, 150.0));
        gestorHabitaciones.agregarHabitacion(new Suite(201, 300.0));
        gestorReservas = new GestorReservas(new DAOEnMemoria.ReservaEnMemoria(), gestorHabitaciones);
        controller = new ReservaController(gestorReservas, gestorHabitaciones);
        entrada = new Date();
        salida = new Date(entrada.getTime() + 86_400_000L);
    }

    private NuevaReservaDTO datos(String nombre, int habitacion) {
        return new NuevaReservaDTO(nombre, "77771234", "cliente@example.com", habitacion, entrada, salida);
    }

    @Test
    @DisplayName("El constructor rechaza gestores null")
    void constructorRechazaNull() {
        assertThrows(IllegalArgumentException.class, () -> new ReservaController(null, gestorHabitaciones));
        assertThrows(IllegalArgumentException.class, () -> new ReservaController(gestorReservas, null));
    }

    @Test
    @DisplayName("crearReserva devuelve un resumen con los datos y el total de la habitación")
    void crearReservaDevuelveResumen() throws Exception {
        ReservaResumenDTO resumen = controller.crearReserva(datos("Ana López", 102));

        assertNotNull(resumen.getIdReserva());
        assertEquals("Ana López", resumen.getNombreCliente());
        assertEquals(102, resumen.getNumeroHabitacion());
        assertEquals("HabitacionDoble", resumen.getTipoHabitacion());
        assertEquals(180.0, resumen.getTotal(), 0.001); // 150 * 1.20
        assertEquals(Reserva.ACTIVA, resumen.getEstado());
        assertEquals(entrada, resumen.getFechaEntrada());
        assertEquals(salida, resumen.getFechaSalida());
    }

    @Test
    @DisplayName("El total de una suite aplica su recargo del 50%")
    void totalDeSuite() throws Exception {
        ReservaResumenDTO resumen = controller.crearReserva(datos("Ana López", 201));

        assertEquals("Suite", resumen.getTipoHabitacion());
        assertEquals(450.0, resumen.getTotal(), 0.001); // 300 * 1.50
    }

    @Test
    @DisplayName("Crear la reserva deja la habitación ocupada en el inventario")
    void crearReservaOcupaLaHabitacion() throws Exception {
        controller.crearReserva(datos("Ana López", 101));

        assertEquals(Habitacion.OCUPADA, gestorHabitaciones.obtenerHabitacion(101).getEstado());
    }

    @Test
    @DisplayName("Reservar una habitación que no existe lanza IllegalArgumentException con el número")
    void habitacionInexistente() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.crearReserva(datos("Ana López", 999)));

        assertTrue(ex.getMessage().contains("999"));
        assertEquals(0, gestorReservas.obtenerTotalReservas());
    }

    @Test
    @DisplayName("Reservar una habitación ocupada lanza HabitacionNoDisponibleException")
    void habitacionOcupada() throws Exception {
        controller.crearReserva(datos("Ana López", 101));

        assertThrows(HabitacionNoDisponibleException.class,
                () -> controller.crearReserva(datos("Luis Mena", 101)));
        assertEquals(1, gestorReservas.obtenerTotalReservas());
    }

    @Test
    @DisplayName("Un nombre de cliente vacío se rechaza y no ocupa la habitación")
    void clienteInvalido() {
        assertThrows(IllegalArgumentException.class, () -> controller.crearReserva(datos("", 101)));

        assertTrue(gestorHabitaciones.obtenerHabitacion(101).estaDisponible());
        assertEquals(0, gestorReservas.obtenerTotalReservas());
    }

    @Test
    @DisplayName("Fechas incoherentes se rechazan y no ocupan la habitación")
    void fechasIncoherentes() {
        NuevaReservaDTO alReves = new NuevaReservaDTO(
                "Ana López", "77771234", "a@x.com", 101, salida, entrada);

        assertThrows(IllegalArgumentException.class, () -> controller.crearReserva(alReves));

        assertTrue(gestorHabitaciones.obtenerHabitacion(101).estaDisponible());
    }

    @Test
    @DisplayName("cancelarReserva libera la habitación; con un id inexistente devuelve false")
    void cancelarReserva() throws Exception {
        ReservaResumenDTO resumen = controller.crearReserva(datos("Ana López", 101));

        assertTrue(controller.cancelarReserva(resumen.getIdReserva()));
        assertTrue(gestorHabitaciones.obtenerHabitacion(101).estaDisponible());
        assertFalse(controller.cancelarReserva("no-existe"));
    }

    @Test
    @DisplayName("listarNumerosHabitacionesDisponibles refleja las reservas hechas")
    void listarDisponibles() throws Exception {
        assertEquals(Arrays.asList(101, 102, 201), controller.listarNumerosHabitacionesDisponibles());

        controller.crearReserva(datos("Ana López", 101));

        assertEquals(Arrays.asList(102, 201), controller.listarNumerosHabitacionesDisponibles());
    }

    @Test
    @DisplayName("listarReservasActivas excluye las canceladas")
    void listarActivas() throws Exception {
        ReservaResumenDTO r1 = controller.crearReserva(datos("Ana López", 101));
        controller.crearReserva(datos("Luis Mena", 102));
        controller.cancelarReserva(r1.getIdReserva());

        List<ReservaResumenDTO> activas = controller.listarReservasActivas();

        assertEquals(1, activas.size());
        assertEquals("Luis Mena", activas.get(0).getNombreCliente());
    }
}
