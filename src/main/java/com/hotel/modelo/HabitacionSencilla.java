package com.hotel.modelo;

/**
 * Esta clase representa una habitación sencilla del hotel.
 * Una habitación sencilla es la más básica y económica, diseñada para una
 * persona.
 * Su precio se calcula aplicando el factor de demanda al precio base.
 */
public class HabitacionSencilla extends Habitacion {

    /**
     * Constructor de HabitacionSencilla.
     *
     * @param numero     número único de la habitación
     * @param precioBase precio base de la habitación sencilla
     */
    public HabitacionSencilla(int numero, double precioBase) {
        super(numero, precioBase);
    }

    /**
     * Calcula el precio de la habitación sencilla multiplicando el precio base
     * por el factor de demanda.
     *
     * @param factorDemanda factor multiplicador según la demanda del mercado
     * @return el precio calculado: precioBase * factorDemanda
     */
    @Override
    public double calcularPrecio(double factorDemanda) {
        return precioBase * factorDemanda;
    }

    @Override
    public String toString() {
        return "HabitacionSencilla{" +
                "numero=" + numero +
                ", estado='" + estado + '\'' +
                ", precioBase=" + precioBase +
                '}';
    }
}
