package com.hotel.excepciones;

/**
 * Excepción no verificada (unchecked) que señala una falla en la capa DAO:
 * el archivo .dat no se pudo leer, no se pudo escribir, o su contenido no
 * corresponde al modelo actual.
 *
 * Es un RuntimeException hecho a propósito porque un fallo de persistencia no
 * es una situación que el código de negocio pueda "manejar y continuar", si no
 * se puede leer reservas.dat, el sistema no debe seguir operando como si el
 * hotel no tuviera reservas.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construye la excepción con un mensaje descriptivo.
     * 
     * @param mensaje descripción de la falla de persistencia
     */
    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    /**
     * Construye la excepción conservando la causa original.
     * 
     * @param mensaje descripción de la falla de persistencia
     * @param causa   excepción de E/S o de deserialización que la originó
     */
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
