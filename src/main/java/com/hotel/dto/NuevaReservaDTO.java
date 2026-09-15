package com.hotel.dto;

import java.util.Date;

public class NuevaReservaDTO {

    private final String nombreCliente;
    private final String telefonoCliente;
    private final String emailCliente;
    private final int numeroHabitacion;
    private final Date fechaEntrada;
    private final Date fechaSalida;

    public NuevaReservaDTO(String nombreCliente, String telefonoCliente, String emailCliente,
                           int numeroHabitacion, Date fechaEntrada, Date fechaSalida) {
        this.nombreCliente = nombreCliente;
        this.telefonoCliente = telefonoCliente;
        this.emailCliente = emailCliente;
        this.numeroHabitacion = numeroHabitacion;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }
    public String getTelefonoCliente() {
        return telefonoCliente;
    }
    public String getEmailCliente() {
        return emailCliente;
    }
    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }
    public Date getFechaEntrada() {
        return fechaEntrada;
    }
    public Date getFechaSalida() {
        return fechaSalida;
    }
}
