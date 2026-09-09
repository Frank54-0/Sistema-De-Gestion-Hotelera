package com.hotel.servicio;

/**
 * Clase de utilidad para calcular el factor de demanda basado en la ocupación del hotel.
 * Proporciona un método estático que determina el multiplicador de precio según cuántas
 * habitaciones están ocupadas en relación al total disponible.
 */
public class CalculadorTarifas {

    /**
     * Calcula el factor de demanda basado en el porcentaje de ocupación del hotel.
     * 
     * Factor de demanda:
     * - Ocupación 0-30%: factor = 1.0 (precio base)
     * - Ocupación 31-60%: factor = 1.2 (aumento del 20%)
     * - Ocupación 61-90%: factor = 1.5 (aumento del 50%)
     * - Ocupación 91-100%: factor = 2.0 (aumento del 100%)
     *
     * @param habitacionesOcupadas número de habitaciones actualmente ocupadas
     * @param totalHabitaciones    número total de habitaciones del hotel
     * @return factor de demanda aplicable a los precios
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    public static double calcularFactorDemanda(int habitacionesOcupadas, int totalHabitaciones) {
        if (totalHabitaciones <= 0) {
            throw new IllegalArgumentException("El total de habitaciones debe ser mayor a 0");
        }
        
        if (habitacionesOcupadas < 0 || habitacionesOcupadas > totalHabitaciones) {
            throw new IllegalArgumentException("Las habitaciones ocupadas deben estar entre 0 y " + totalHabitaciones);
        }
        
        double porcentajeOcupacion = (double) habitacionesOcupadas / totalHabitaciones;
        
        if (porcentajeOcupacion <= 0.30) {
            return 1.0;
        } else if (porcentajeOcupacion <= 0.60) {
            return 1.2;
        } else if (porcentajeOcupacion <= 0.90) {
            return 1.5;
        } else {
            return 2.0;
        }
    }
}
