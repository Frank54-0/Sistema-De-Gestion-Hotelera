package com.hotel.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la entidad Reserva: cálculo de total, cancelación y
 * encapsulamiento de la colección de servicios.
 */
class ReservaTest {

    private Reserva nuevaReserva() {
        Cliente cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 86_400_000L);
        return new Reserva(cliente, habitacion, entrada, salida);
    }

    @Test
    @DisplayName("Una reserva nueva nace ACTIVA")
    void reservaNuevaEstaActiva() {
        assertTrue(nuevaReserva().estaActiva());
    }

    @Test
    @DisplayName("El total suma el precio de la habitación más los servicios")
    void calculaTotalConServicios() {
        Reserva reserva = nuevaReserva();
        reserva.agregarServicio(new ServicioAdicional("Desayuno", 15.0));
        reserva.agregarServicio(new ServicioAdicional("Spa", 25.0));

        assertEquals(140.0, reserva.calcularTotal(), 0.001);
    }

    @Test
    @DisplayName("Cancelar cambia el estado a CANCELADA")
    void cancelarCambiaEstado() {
        Reserva reserva = nuevaReserva();
        reserva.cancelar();

        assertFalse(reserva.estaActiva());
        assertEquals(Reserva.CANCELADA, reserva.getEstado());
    }

    @Test
    @DisplayName("La lista de servicios expuesta es de solo lectura (encapsulamiento)")
    void listaDeServiciosEsInmutable() {
        Reserva reserva = nuevaReserva();
        assertThrows(UnsupportedOperationException.class,
                () -> reserva.getServicios().add(new ServicioAdicional("Pirata", 0.0)));
    }
}
