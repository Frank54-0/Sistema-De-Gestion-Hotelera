package com.hotel.excepciones;

/**
 * Excepción personalizada que se lanza cuando se intenta reservar una habitación
 * que no está disponible para las fechas solicitadas.
 * 
 * Puede deberse a que:
 * - La habitación ya está ocupada
 * - La habitación está en mantenimiento
 * - No hay habitaciones disponibles del tipo solicitado
 */
public class HabitacionNoDisponibleException extends Exception {

    /**
     * Constructor que acepta un mensaje de error.
     *
     * @param mensaje descripción detallada del motivo por el cual la habitación no está disponible
     */
    public HabitacionNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    /**
     * Constructor que acepta un mensaje de error y una causa.
     *
     * @param mensaje descripción del error
     * @param causa   excepción que causó este error
     */
    public HabitacionNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
