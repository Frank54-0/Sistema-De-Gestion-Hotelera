package com.hotel.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del cálculo del factor de demanda, incluyendo los valores
 * exactos en los bordes de cada rango de ocupación.
 */
class CalculadorTarifasTest {

    @Test
    @DisplayName("Ocupación baja mantiene el precio base")
    void ocupacionBajaNoAumentaPrecio() {
        assertEquals(1.0, CalculadorTarifas.calcularFactorDemanda(0, 10), 0.001);
        assertEquals(1.0, CalculadorTarifas.calcularFactorDemanda(3, 10), 0.001);
    }

    @Test
    @DisplayName("Los bordes de cada rango caen en el factor esperado")
    void bordesDeRango() {
        assertEquals(1.2, CalculadorTarifas.calcularFactorDemanda(6, 10), 0.001);  // 60%
        assertEquals(1.5, CalculadorTarifas.calcularFactorDemanda(9, 10), 0.001);  // 90%
        assertEquals(2.0, CalculadorTarifas.calcularFactorDemanda(10, 10), 0.001); // 100%
    }

    @Test
    @DisplayName("Un hotel sin habitaciones es un dato inválido")
    void rechazaTotalCero() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadorTarifas.calcularFactorDemanda(0, 0));
    }

    @Test
    @DisplayName("No puede haber más ocupadas que el total")
    void rechazaOcupadasMayorQueTotal() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadorTarifas.calcularFactorDemanda(11, 10));
    }
}
