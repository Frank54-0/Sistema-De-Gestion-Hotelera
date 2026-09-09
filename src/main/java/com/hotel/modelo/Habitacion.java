package com.hotel.modelo;

import java.io.Serializable;

/**
 * Clase abstracta que representa una habitación genérica del hotel.
 * Define atributos y métodos comunes a todos los tipos de habitación.
 * Implementa la interfaz Reservable para permitir el cálculo dinámico de precios.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public abstract class Habitacion implements Reservable, Serializable {

    private static final long serialVersionUID = 1L;

    protected int numero;
    protected String estado;
    protected double precioBase;

    /**
     * Constructor de Habitacion.
     *
     * @param numero     número único de la habitación
     * @param precioBase precio base de la habitación sin ajustes de demanda
     * @throws IllegalArgumentException si el precioBase es negativo
     */
    public Habitacion(int numero, double precioBase) {
        if (precioBase < 0) {
            throw new IllegalArgumentException("El precio base no puede ser negativo");
        }
        this.numero = numero;
        this.precioBase = precioBase;
        this.estado = "DISPONIBLE";
    }

    /**
     * Método abstracto que define el contrato para calcular el precio.
     * Cada subclase implementará su propia fórmula de cálculo.
     *
     * @param factorDemanda factor multiplicador según la demanda del mercado
     * @return el precio calculado de la habitación
     */
    @Override
    public abstract double calcularPrecio(double factorDemanda);

    /**
     * Obtiene el estado actual de la habitación.
     *
     * @return estado de la habitación (DISPONIBLE, OCUPADA, MANTENIMIENTO)
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Establece el estado de la habitación.
     *
     * @param estado nuevo estado de la habitación
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Obtiene el número de la habitación.
     *
     * @return número único de la habitación
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Obtiene el precio base de la habitación.
     *
     * @return precio base sin ajustes
     */
    public double getPrecioBase() {
        return precioBase;
    }

    @Override
    public String toString() {
        return "Habitacion{" +
                "numero=" + numero +
                ", estado='" + estado + '\'' +
                ", precioBase=" + precioBase +
                '}';
    }
}
