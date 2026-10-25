package com.sv.grupo5.suiteflow.model;

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
     * @throws IllegalArgumentException si algún dato tiene formato inválido
     */
    public Cliente(String nombre, String telefono, String email) {
        this.id = UUID.randomUUID().toString();
        // Llamamos a los setters para aprovechar las validaciones centralizadas
        setNombre(nombre);
        setTelefono(telefono);
        setEmail(email);
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        // Solo permite letras (incluyendo tildes y ñ) y espacios. Entre 3 y 50 caracteres.
        if (nombre == null || !nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{3,50}$")) {
            throw new IllegalArgumentException(
                    "Error: El nombre solo puede contener letras y espacios (mínimo 3 caracteres).");
        }
        this.nombre = nombre.trim();
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        // Valida que sean exactamente 8 números sin espacios ni letras.
        if (telefono == null || !telefono.matches("^\\d{8}$")) {
            throw new IllegalArgumentException(
                    "Error: El teléfono debe contener exactamente 8 números.");
        }
        this.telefono = telefono.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        // Patrón estándar para validar correos electrónicos
        if (email == null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            throw new IllegalArgumentException(
                    "Error: El correo no tiene un formato válido (ejemplo: usuario@correo.com).");
        }
        this.email = email.trim();
    }

    @Override
    public String toString() {
        return "Cliente{id='" + id + "', nombre='" + nombre + "', telefono='" + telefono + "', email='" + email + "'}";
    }
}