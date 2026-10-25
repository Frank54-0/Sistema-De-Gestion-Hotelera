package com.sv.grupo5.suiteflow.controller;

import com.sv.grupo5.suiteflow.dto.NuevaReservaDTO;
import com.sv.grupo5.suiteflow.dto.ReservaResumenDTO;
import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionDoble;
import com.sv.grupo5.suiteflow.model.Reserva;
import com.sv.grupo5.suiteflow.service.GestorHabitaciones;
import com.sv.grupo5.suiteflow.service.GestorReservas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de ReservaController con los gestores simulados (Mockito): aquí solo
 * se verifica que el controller traduzca el DTO, delegue en los servicios y
 * convierta el resultado. La lógica de negocio ya se prueba en los tests de
 * service.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaControllerMockitoTest {

    @Mock
    private GestorReservas gestorReservas;

    @Mock
    private GestorHabitaciones gestorHabitaciones;

    private ReservaController controller;
    private Date entrada;
    private Date salida;
    private Habitacion doble;

    @BeforeEach
    void prepararEscenario() {
        controller = new ReservaController(gestorReservas, gestorHabitaciones);
        entrada = new Date();
        salida = new Date(entrada.getTime() + 86_400_000L);
        doble = new HabitacionDoble(102, 150.0);
    }

    private NuevaReservaDTO datos(String nombre, int numeroHabitacion) {
        return new NuevaReservaDTO(nombre, "77771234", "ana@example.com", numeroHabitacion, entrada, salida);
    }

    @Test
    @DisplayName("crearReserva delega en el gestor con el cliente del DTO y devuelve el resumen")
    void crearReservaDelegaYDevuelveResumen() throws Exception {
        Reserva reservaDevuelta = new Reserva(
                new Cliente("Ana Lopez", "77771234", "ana@example.com"), doble, entrada, salida);
        when(gestorHabitaciones.obtenerHabitacion(102)).thenReturn(doble);
        when(gestorReservas.crearReserva(any(Cliente.class), eq(doble), eq(entrada), eq(salida)))
                .thenReturn(reservaDevuelta);

        ReservaResumenDTO resumen = controller.crearReserva(datos("Ana Lopez", 102));

        assertEquals("Ana Lopez", resumen.getNombreCliente());
        assertEquals(102, resumen.getNumeroHabitacion());
        assertEquals("HabitacionDoble", resumen.getTipoHabitacion());
        assertEquals(180.0, resumen.getTotal(), 0.001); // 150 * 1.20

        ArgumentCaptor<Cliente> clienteEnviado = ArgumentCaptor.forClass(Cliente.class);
        verify(gestorReservas).crearReserva(clienteEnviado.capture(), eq(doble), eq(entrada), eq(salida));
        assertEquals("Ana Lopez", clienteEnviado.getValue().getNombre());
        assertEquals("77771234", clienteEnviado.getValue().getTelefono());
        assertEquals("ana@example.com", clienteEnviado.getValue().getEmail());
    }

    @Test
    @DisplayName("Si la habitación no existe, lanza IllegalArgumentException y no llama al gestor de reservas")
    void habitacionInexistenteNoLlamaAlGestor() throws Exception {
        when(gestorHabitaciones.obtenerHabitacion(999)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.crearReserva(datos("Ana Lopez", 999)));

        assertTrue(ex.getMessage().contains("999"));
        verify(gestorReservas, never()).crearReserva(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si el gestor informa que la habitación no está disponible, el controller propaga la excepción")
    void habitacionNoDisponiblePropagaExcepcion() throws Exception {
        when(gestorHabitaciones.obtenerHabitacion(102)).thenReturn(doble);
        when(gestorReservas.crearReserva(any(Cliente.class), eq(doble), any(Date.class), any(Date.class)))
                .thenThrow(HabitacionNoDisponibleException.class);

        assertThrows(HabitacionNoDisponibleException.class,
                () -> controller.crearReserva(datos("Ana Lopez", 102)));
    }

    @Test
    @DisplayName("Un nombre inválido se rechaza antes de llamar al gestor de reservas")
    void clienteInvalidoNoLlamaAlGestor() throws Exception {
        when(gestorHabitaciones.obtenerHabitacion(102)).thenReturn(doble);

        assertThrows(IllegalArgumentException.class,
                () -> controller.crearReserva(datos("Juan123", 102)));

        verify(gestorReservas, never()).crearReserva(any(), any(), any(), any());
    }

    @Test
    @DisplayName("cancelarReserva devuelve lo que responde el gestor (true y false)")
    void cancelarReservaDelega() {
        when(gestorReservas.cancelarReserva("abc")).thenReturn(true);
        when(gestorReservas.cancelarReserva("no-existe")).thenReturn(false);

        assertTrue(controller.cancelarReserva("abc"));
        assertFalse(controller.cancelarReserva("no-existe"));

        verify(gestorReservas).cancelarReserva("abc");
        verify(gestorReservas).cancelarReserva("no-existe");
    }
}
