package com.hotel.modelo;

import java.io.Serializable;

/**
 * Clase abstracta que representa una habitación genérica del hotel.
 * Esta clase define atributos y métodos comunes a todos los tipos de
 * habitación.
 * Implementa la interfaz Reservable para permitir el cálculo dinámico de
 * precios.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public abstract class Habitacion implements Reservable, Serializable {

    private static final long serialVersionUID = 1L;

    /** Estado: la habitación puede reservarse. */
    public static final String DISPONIBLE = "DISPONIBLE";
    /** Estado: la habitación tiene una reserva activa. */
    public static final String OCUPADA = "OCUPADA";
    /** Estado: la habitación está fuera de servicio temporalmente. */
    public static final String MANTENIMIENTO = "MANTENIMIENTO";

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
        this.estado = DISPONIBLE;
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
        if (!esEstadoValido(estado)) {
            throw new IllegalArgumentException(
                    "Estado de habitación inválido: '" + estado + "'. Valores permitidos: "
                            + DISPONIBLE + ", " + OCUPADA + ", " + MANTENIMIENTO);
        }
        this.estado = estado;
    }

    /**
     * Indica si el estado recibido es uno de los tres estados válidos.
     *
     * @param estado estado a validar
     * @return true si el estado es DISPONIBLE, OCUPADA o MANTENIMIENTO
     */
    public static boolean esEstadoValido(String estado) {
        return DISPONIBLE.equals(estado)
                || OCUPADA.equals(estado)
                || MANTENIMIENTO.equals(estado);
    }

    /**
     * Indica si la habitación se puede reservar en este momento.
     * Evita que cada capa tenga que comparar cadenas por su cuenta.
     *
     * @return true si el estado actual es DISPONIBLE
     */
    public boolean estaDisponible() {
        return DISPONIBLE.equals(estado);
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
