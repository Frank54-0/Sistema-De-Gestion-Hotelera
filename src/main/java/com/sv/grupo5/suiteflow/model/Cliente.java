package com.sv.grupo5.suiteflow.model;

import java.io.Serializable;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Representa a un cliente del hotel.
 * Contiene los datos básicos de contacto necesarios para asociarlo a una
 * Reserva.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public class Cliente implements Serializable {

    private static final long serialVersionUID = 1L;

    // Letras (con tildes y ñ) y espacios, de 3 a 50 caracteres.
    private static final Pattern PATRON_NOMBRE = Pattern.compile("^[\\p{L} ]{3,50}$");
    // Exactamente 8 dígitos.
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\+?\\d{8,15}$");
    // usuario@dominio.ext, con extensión de al menos 2 letras.
    private static final Pattern PATRON_EMAIL =
            Pattern.compile("^[\\w.+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

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
     * @throws IllegalArgumentException si el nombre, teléfono o correo no cumplen el formato
     */
    public Cliente(String nombre, String telefono, String email) {
        this(UUID.randomUUID().toString(), validarNombre(nombre), validarTelefono(telefono), validarEmail(email));
    }

    /**
     * Reconstruye un cliente que ya existe (por ejemplo, leído de la base de
     * datos), conservando su id en lugar de generar uno nuevo.
     *
     * @param id       identificador existente del cliente
     * @param nombre   nombre completo del cliente
     * @param telefono número de teléfono de contacto
     * @param email    correo electrónico de contacto
     * @throws IllegalArgumentException si el id, el nombre o el teléfono son inválidos
     */
    public Cliente(String id, String nombre, String telefono, String email) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El id del cliente no puede estar vacío");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío");
        }
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono del cliente no puede estar vacío");
        }
        this.id = id;
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

    /**
     * @param nombre nuevo nombre (letras y espacios, 3 a 50 caracteres)
     * @throws IllegalArgumentException si no cumple el formato
     */
    public void setNombre(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    public String getTelefono() {
        return telefono;
    }

    /**
     * @param telefono nuevo teléfono (8 dígitos)
     * @throws IllegalArgumentException si no cumple el formato
     */
    public void setTelefono(String telefono) {
        this.telefono = validarTelefono(telefono);
    }

    public String getEmail() {
        return email;
    }

    /**
     * @param email nuevo correo; puede ser null o vacío porque es opcional
     * @throws IllegalArgumentException si tiene texto y no cumple el formato
     */
    public void setEmail(String email) {
        this.email = validarEmail(email);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío");
        }
        if (!PATRON_NOMBRE.matcher(nombre).matches()) {
            throw new IllegalArgumentException(
                    "El nombre debe tener de 3 a 50 letras y espacios, sin números ni símbolos");
        }
        return nombre;
    }

    private static String validarTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono del cliente no puede estar vacío");
        }
        if (!PATRON_TELEFONO.matcher(telefono).matches()) {
            throw new IllegalArgumentException("El teléfono debe tener 8 dígitos, con prefijo 503 opcional (ej. 75375027 o +50375375027)");
        }
        return telefono;
    }

    // El correo es opcional: solo se valida si trae texto.
    private static String validarEmail(String email) {
        if (email != null && !email.isEmpty() && !PATRON_EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("El correo no tiene un formato válido (usuario@dominio.ext)");
        }
        return email;
    }

    @Override
    public String toString() {
        return "Cliente{id='" + id + "', nombre='" + nombre + "', telefono='" + telefono + "', email='" + email + "'}";
    }
}
