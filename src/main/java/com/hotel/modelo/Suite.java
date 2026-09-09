package com.hotel.modelo;

/**
 * Representa una suite del hotel.
 * Una suite es la habitación de lujo más premium, con todas las comodidades y servicios.
 * Su precio se calcula multiplicando el precio base por el factor de demanda y un factor adicional mayor.
 */
public class Suite extends Habitacion {

    /**
     * Constructor de Suite.
     *
     * @param numero     número único de la habitación
     * @param precioBase precio base de la suite
     */
    public Suite(int numero, double precioBase) {
        super(numero, precioBase);
    }

    /**
     * Calcula el precio de la suite multiplicando el precio base
     * por el factor de demanda y aplicando un incremento del 50% adicional
     * por las comodidades premium que ofrece (sala de estar, jacuzzi, servicio concierge).
     *
     * @param factorDemanda factor multiplicador según la demanda del mercado
     * @return el precio calculado: (precioBase * factorDemanda) * 1.50
     */
    @Override
    public double calcularPrecio(double factorDemanda) {
        return (precioBase * factorDemanda) * 1.50;
    }

    @Override
    public String toString() {
        return "Suite{" +
                "numero=" + numero +
                ", estado='" + estado + '\'' +
                ", precioBase=" + precioBase +
                '}';
    }
}
