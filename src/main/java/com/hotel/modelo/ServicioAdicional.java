package com.hotel.modelo;

import java.io.Serializable;

/**
 * Esta clase representa un servicio adicional que puede ser incluido en una
 * reserva.
 * Ej: desayuno, spa, lavandería, transporte, etc.
 * Cada servicio tiene un nombre y un costo asociado.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public class ServicioAdicional implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private double costo;

    /**
     * Constructor de ServicioAdicional.
     *
     * @param nombre nombre del servicio adicional
     * @param costo  costo del servicio en unidades monetarias
     */
    public ServicioAdicional(String nombre, double costo) {
        this.nombre = nombre;
        this.costo = costo;
    }

    /**
     * Obtiene el nombre del servicio.
     *
     * @return nombre del servicio adicional
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Obtiene el costo del servicio.
     *
     * @return costo del servicio en unidades monetarias
     */
    public double getCosto() {
        return costo;
    }

    @Override
    public String toString() {
        return "ServicioAdicional{" +
                "nombre='" + nombre + '\'' +
                ", costo=" + costo +
                '}';
    }
}
