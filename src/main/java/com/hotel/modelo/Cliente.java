package com.hotel.modelo;

import java.io.Serializable;
import java.util.UUID;

/**
 * Representa a un cliente del hotel.
 * Contiene los datos básicos de contacto necesarios para asociarlo a una
 * Reserva.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public class Cliente implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String nombre;
    private String telefono;
    private String email;

    /**
     * Crea un nuevo Cliente. El id se genera automáticamente.
     *
     * @param nombre   nombre completo del cliente
     * @param telefono número de teléfono de contacto
     * @param email    correo electrónico de contacto
     * @throws IllegalArgumentException si el nombre o teléfono son inválidos
     */
    public Cliente(String nombre, String telefono, String email) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío");
        }
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono del cliente no puede estar vacío");
        }
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Cliente{id='" + id + "', nombre='" + nombre + "', telefono='" + telefono + "', email='" + email + "'}";
    }
}
