package com.sv.grupo5.suiteflow.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del cálculo de noches y del total por estadía. */
class ReservaNochesTest {

    private static final long DIA = 86_400_000L;

    private Reserva reserva(Habitacion habitacion, int noches) {
        Cliente cliente = new Cliente("Juan Pérez", "77771234", "juan@example.com");
        Date entrada = new Date();
        return new Reserva(cliente, habitacion, entrada, new Date(entrada.getTime() + noches * DIA));
    }

    @Test
    void cuentaLasNochesEntreEntradaYSalida() {
        assertEquals(3, reserva(new HabitacionSencilla(101, 100.0), 3).calcularNoches());
    }

    @Test
    void elTotalSeMultiplicaPorLasNoches() {
        // Doble: 150 * 1.20 = 180 por noche, 2 noches
        assertEquals(360.0, reserva(new HabitacionDoble(102, 150.0), 2).calcularTotal(), 0.001);
    }

    @Test
    void losServiciosSeSumanUnaSolaVez() {
        Reserva reserva = reserva(new HabitacionSencilla(101, 100.0), 3);
        reserva.agregarServicio(new ServicioAdicional("Desayuno", 15.0));
        assertEquals(315.0, reserva.calcularTotal(), 0.001);
    }

    @Test
    void siSalidaEsElMismoDiaSeCobraUnaNoche() {
        Cliente cliente = new Cliente("Juan Pérez", "77771234", "juan@example.com");
        Date entrada = new Date();
        Reserva reserva = new Reserva(cliente, new Suite(201, 300.0), entrada, new Date(entrada.getTime() + 1000L));
        assertEquals(1, reserva.calcularNoches());
        assertEquals(450.0, reserva.calcularTotal(), 0.001);
    }
}
