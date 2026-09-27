package com.hotel.dto;

import java.util.Date;

public class ReservaResumenDTO {

    private final String idReserva;
    private final String nombreCliente;
    private final int numeroHabitacion;
    private final String tipoHabitacion;
    private final Date fechaEntrada;
    private final Date fechaSalida;
    private final double total;
    private final String estado;

    public ReservaResumenDTO(String idReserva, String nombreCliente, int numeroHabitacion, String tipoHabitacion,
                             Date fechaEntrada, Date fechaSalida, double total, String estado) {
        this.idReserva = idReserva;
        this.nombreCliente = nombreCliente;
        this.numeroHabitacion = numeroHabitacion;
        this.tipoHabitacion = tipoHabitacion;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.total = total;
        this.estado = estado;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }

    public String getTipoHabitacion() {
        return tipoHabitacion;
    }

    public Date getFechaEntrada() {
        return fechaEntrada;
    }

    public Date getFechaSalida() {
        return fechaSalida;
    }

    public double getTotal() {
        return total;
    }

    public String getEstado() {
        return estado;
    }
}
