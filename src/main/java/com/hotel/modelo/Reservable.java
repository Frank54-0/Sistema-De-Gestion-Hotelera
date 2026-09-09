package com.hotel.modelo;

/**
 * Interfaz que define el contrato para objetos que pueden ser reservados.
 * Cualquier clase que implemente esta interfaz debe proporcionar un método
 * para calcular su precio basado en un factor de demanda.
 */
public interface Reservable {

    /**
     * Calcula el precio de la habitación basado en un factor de demanda.
     *
     * @param factorDemanda factor multiplicador que ajusta el precio según la demanda del mercado
     * @return el precio calculado con el ajuste de demanda aplicado
     */
    double calcularPrecio(double factorDemanda);
}
