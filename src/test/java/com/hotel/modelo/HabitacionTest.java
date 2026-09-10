package com.hotel.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la jerarquía de habitaciones: herencia, polimorfismo,
 * encapsulamiento y validación de estados.
 */
class HabitacionTest {

    @Test
    @DisplayName("Una habitación nueva nace DISPONIBLE")
    void habitacionNuevaEstaDisponible() {
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        assertTrue(habitacion.estaDisponible());
        assertEquals(Habitacion.DISPONIBLE, habitacion.getEstado());
    }

    @Test
    @DisplayName("Cada subclase aplica su propia fórmula de precio (polimorfismo)")
    void cadaSubclaseCalculaSuPrecio() {
        Habitacion sencilla = new HabitacionSencilla(101, 100.0);
        Habitacion doble = new HabitacionDoble(102, 100.0);
        Habitacion suite = new Suite(201, 100.0);

        assertEquals(100.0, sencilla.calcularPrecio(1.0), 0.001);
        assertEquals(120.0, doble.calcularPrecio(1.0), 0.001);
        assertEquals(150.0, suite.calcularPrecio(1.0), 0.001);
    }

    @Test
    @DisplayName("El factor de demanda multiplica el precio de cualquier subclase")
    void factorDemandaAfectaTodasLasSubclases() {
        Habitacion doble = new HabitacionDoble(102, 100.0);
        assertEquals(240.0, doble.calcularPrecio(2.0), 0.001);
    }

    @Test
    @DisplayName("No se permite un precio base negativo")
    void rechazaPrecioBaseNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> new HabitacionSencilla(101, -50.0));
    }

    @Test
    @DisplayName("setEstado rechaza estados que no existen en el sistema")
    void rechazaEstadoInvalido() {
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        assertThrows(IllegalArgumentException.class,
                () -> habitacion.setEstado("LIBRE"));
        // El estado original no debe haberse alterado
        assertEquals(Habitacion.DISPONIBLE, habitacion.getEstado());
    }
}
