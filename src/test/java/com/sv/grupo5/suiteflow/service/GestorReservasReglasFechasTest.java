package com.sv.grupo5.suiteflow.service;

import com.sv.grupo5.suiteflow.dao.DAOEnMemoria;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.model.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de fechas de GestorReservas.validarDatosReserva: año de 4 dígitos
 * (2024-2100), sin reservas en el pasado, mínimo una noche y salida posterior
 * a la entrada. Las fechas se calculan con Calendar a partir de hoy para que
 * los tests no caduquen.
 */
class GestorReservasReglasFechasTest {

    private GestorReservas gestor;
    private Habitacion habitacion;
    private Cliente cliente;

    @BeforeEach
    void prepararEscenario() {
        GestorHabitaciones gestorHabitaciones =
                new GestorHabitaciones(new DAOEnMemoria.HabitacionEnMemoria());
        habitacion = new HabitacionSencilla(101, 100.0);
        gestorHabitaciones.agregarHabitacion(habitacion);
        gestor = new GestorReservas(new DAOEnMemoria.ReservaEnMemoria(), gestorHabitaciones);
        cliente = new Cliente("Juan Perez", "77771234", "juan@example.com");
    }

    /** Fecha a las 12:00, desplazada {@code dias} días respecto a hoy (puede ser negativo). */
    private static Date enDias(int dias) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dias);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date fecha(int anio, int mes, int dia, int hora) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes, dia, hora, 0, 0);
        return cal.getTime();
    }

    private void assertRechazada(Date entrada, Date salida, String motivo) {
        assertThrows(IllegalArgumentException.class,
                () -> gestor.crearReserva(cliente, habitacion, entrada, salida), motivo);
        assertEquals(0, gestor.obtenerTotalReservas(), "Una reserva rechazada no se guarda");
        assertTrue(habitacion.estaDisponible(), "Una reserva rechazada no ocupa la habitación");
    }

    // ---- Casos exitosos -------------------------------------------------------

    @Test
    @DisplayName("Una estadía de una noche (mañana -> pasado mañana) se acepta")
    void aceptaUnaNoche() throws Exception {
        Reserva reserva = gestor.crearReserva(cliente, habitacion, enDias(1), enDias(2));

        assertNotNull(reserva);
        assertEquals(Habitacion.OCUPADA, habitacion.getEstado());
        assertEquals(1, gestor.obtenerTotalReservas());
    }

    @Test
    @DisplayName("Entrar hoy (ahora mismo) y salir mañana se acepta: no es una reserva en el pasado")
    void aceptaEntradaHoy() throws Exception {
        Date ahora = new Date();

        Reserva reserva = gestor.crearReserva(cliente, habitacion, ahora, enDias(1));

        assertNotNull(reserva);
        assertEquals(1, gestor.obtenerTotalReservas());
    }

    // ---- Casos de error -------------------------------------------------------

    @Test
    @DisplayName("Rechaza una entrada anterior al día de hoy")
    void rechazaEntradaEnElPasado() {
        assertRechazada(enDias(-1), enDias(2), "La entrada de ayer es una reserva en el pasado");
    }

    @Test
    @DisplayName("Rechaza entrada y salida el mismo día (day use), aunque la hora sea distinta")
    void rechazaMismoDia() {
        Date entrada = enDias(1);
        Calendar tarde = Calendar.getInstance();
        tarde.setTime(entrada);
        tarde.set(Calendar.HOUR_OF_DAY, 18);

        assertRechazada(entrada, tarde.getTime(), "Debe haber al menos una noche");
    }

    @Test
    @DisplayName("Rechaza una salida anterior a la entrada")
    void rechazaSalidaAntesDeEntrada() {
        assertRechazada(enDias(3), enDias(1), "La salida debe ser posterior a la entrada");
    }

    @Test
    @DisplayName("Rechaza años menores a 2024 (el caso '26' interpretado como año 26 d.C.)")
    void rechazaAnioMenorA2024() {
        assertRechazada(fecha(26, Calendar.MARCH, 10, 12), fecha(26, Calendar.MARCH, 12, 12),
                "Año 26 d.C.");
        assertRechazada(fecha(2023, Calendar.DECEMBER, 30, 12), fecha(2024, Calendar.JANUARY, 2, 12),
                "Año de entrada 2023");
    }

    @Test
    @DisplayName("Rechaza años mayores a 2100")
    void rechazaAnioMayorA2100() {
        assertRechazada(fecha(2101, Calendar.JANUARY, 10, 12), fecha(2101, Calendar.JANUARY, 12, 12),
                "Año 2101");
        assertRechazada(enDias(1), fecha(2101, Calendar.JANUARY, 12, 12),
                "Año de salida 2101");
    }
}
