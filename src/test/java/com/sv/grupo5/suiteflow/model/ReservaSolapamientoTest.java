package com.sv.grupo5.suiteflow.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservaSolapamientoTest {

    private static Date dia(int dia) {
        return java.sql.Date.valueOf(LocalDate.of(2026, 12, dia));
    }

    // Reserva existente del día 10 al 15.
    private Reserva reservaDel10Al15() {
        Cliente cliente = new Cliente("Ana", "70000000", "ana@correo.com");
        return new Reserva(cliente, new HabitacionSencilla(101, 100.0), dia(10), dia(15));
    }

    @Test
    void sinSolapeSiElRangoEstaAntesOEstaDespues() {
        Reserva reserva = reservaDel10Al15();

        assertFalse(reserva.seSolapaCon(dia(1), dia(5)));
        assertFalse(reserva.seSolapaCon(dia(20), dia(25)));
    }

    @Test
    void relevoElMismoDiaNoSeSolapa() {
        Reserva reserva = reservaDel10Al15();

        assertFalse(reserva.seSolapaCon(dia(15), dia(18)));   // entra el día que otro sale
        assertFalse(reserva.seSolapaCon(dia(7), dia(10)));    // sale el día que otro entra
    }

    @Test
    void haySolapeSiCompartenAlMenosUnaNoche() {
        Reserva reserva = reservaDel10Al15();

        assertTrue(reserva.seSolapaCon(dia(12), dia(18)));    // empieza dentro
        assertTrue(reserva.seSolapaCon(dia(8), dia(11)));     // termina dentro
        assertTrue(reserva.seSolapaCon(dia(11), dia(13)));    // queda contenido
        assertTrue(reserva.seSolapaCon(dia(5), dia(20)));     // la contiene
    }

    @Test
    void rechazaFechasNull() {
        Reserva reserva = reservaDel10Al15();

        assertThrows(IllegalArgumentException.class, () -> reserva.seSolapaCon(null, dia(5)));
    }
}
