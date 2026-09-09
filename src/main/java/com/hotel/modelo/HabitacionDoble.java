package com.hotel.modelo;

/**
 * Representa una habitación doble del hotel.
 * Una habitación doble está diseñada para dos personas y ofrece más comodidades que la sencilla.
 * Su precio se calcula multiplicando el precio base por el factor de demanda y un factor adicional.
 */
public class HabitacionDoble extends Habitacion {

    /**
     * Constructor de HabitacionDoble.
     *
     * @param numero     número único de la habitación
     * @param precioBase precio base de la habitación doble
     */
    public HabitacionDoble(int numero, double precioBase) {
        super(numero, precioBase);
    }

    /**
     * Calcula el precio de la habitación doble multiplicando el precio base
     * por el factor de demanda y aplicando un incremento del 20% adicional
     * por las comodidades extra que ofrece (cama doble, más espacio).
     *
     * @param factorDemanda factor multiplicador según la demanda del mercado
     * @return el precio calculado: (precioBase * factorDemanda) * 1.20
     */
    @Override
    public double calcularPrecio(double factorDemanda) {
        return (precioBase * factorDemanda) * 1.20;
    }

    @Override
    public String toString() {
        return "HabitacionDoble{" +
                "numero=" + numero +
                ", estado='" + estado + '\'' +
                ", precioBase=" + precioBase +
                '}';
    }
}
