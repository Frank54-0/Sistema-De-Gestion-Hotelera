package com.hotel.modelo;

import java.io.Serializable;
import java.util.UUID;

/**
 * Representa un empleado del hotel.
 * Cada empleado tiene un identificador único, nombre y rol dentro de la organización.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public class Empleado implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String nombre;
    private String rol;

    /**
     * Constructor de Empleado.
     * El id se genera automáticamente mediante UUID.
     *
     * @param nombre nombre completo del empleado
     * @param rol    rol del empleado (RECEPCIONISTA, GERENTE, LIMPIEZA, etc.)
     */
    public Empleado(String nombre, String rol) {
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.rol = rol;
    }

    /**
     * Obtiene el id único del empleado.
     *
     * @return identificador único del empleado
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene el nombre del empleado.
     *
     * @return nombre completo del empleado
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del empleado.
     *
     * @param nombre nuevo nombre del empleado
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene el rol del empleado.
     *
     * @return rol del empleado en el hotel
     */
    public String getRol() {
        return rol;
    }

    /**
     * Establece el rol del empleado.
     *
     * @param rol nuevo rol del empleado
     */
    public void setRol(String rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "Empleado{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}
