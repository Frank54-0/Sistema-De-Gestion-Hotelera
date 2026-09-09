package com.hotel.modelo;

import java.io.Serializable;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa una reserva de habitación en el hotel.
 * Contiene información del cliente, la habitación reservada, fechas, estado y servicios adicionales.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private Cliente cliente;
    private Habitacion habitacion;
    private Date fechaEntrada;
    private Date fechaSalida;
    private String estado;
    private List<ServicioAdicional> servicios;

    /**
     * Constructor de Reserva.
     * El id se genera automáticamente mediante UUID.
     *
     * @param cliente        cliente que realiza la reserva
     * @param habitacion     habitación a reservar
     * @param fechaEntrada   fecha de check-in
     * @param fechaSalida    fecha de check-out
     */
    public Reserva(Cliente cliente, Habitacion habitacion, Date fechaEntrada, Date fechaSalida) {
        this.id = UUID.randomUUID().toString();
        this.cliente = cliente;
        this.habitacion = habitacion;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.estado = "ACTIVA";
        this.servicios = new ArrayList<>();
    }

    /**
     * Obtiene el id único de la reserva.
     *
     * @return identificador único de la reserva
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene el cliente de la reserva.
     *
     * @return cliente que realizó la reserva
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Obtiene la habitación reservada.
     *
     * @return habitación de la reserva
     */
    public Habitacion getHabitacion() {
        return habitacion;
    }

    /**
     * Obtiene la fecha de entrada.
     *
     * @return fecha de check-in
     */
    public Date getFechaEntrada() {
        return fechaEntrada;
    }

    /**
     * Obtiene la fecha de salida.
     *
     * @return fecha de check-out
     */
    public Date getFechaSalida() {
        return fechaSalida;
    }

    /**
     * Obtiene el estado actual de la reserva.
     *
     * @return estado de la reserva (ACTIVA, CANCELADA, FINALIZADA)
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Obtiene la lista de servicios adicionales asociados a la reserva.
     *
     * @return lista de servicios adicionales
     */
    public List<ServicioAdicional> getServicios() {
        return servicios;
    }

    /**
     * Agrega un servicio adicional a la reserva.
     *
     * @param servicio servicio adicional a agregar
     */
    public void agregarServicio(ServicioAdicional servicio) {
        if (servicio != null) {
            servicios.add(servicio);
        }
    }

    /**
     * Calcula el costo total de la reserva.
     * Incluye el costo de la habitación (con factor de demanda 1.0 como base)
     * más el costo de todos los servicios adicionales.
     *
     * @return costo total de la reserva
     */
    public double calcularTotal() {
        double total = habitacion.calcularPrecio(1.0);
        
        for (ServicioAdicional servicio : servicios) {
            total += servicio.getCosto();
        }
        
        return total;
    }

    /**
     * Cancela la reserva cambiando su estado a CANCELADA.
     */
    public void cancelar() {
        this.estado = "CANCELADA";
    }

    @Override
    public String toString() {
        return "Reserva{" +
                "id='" + id + '\'' +
                ", cliente=" + cliente +
                ", habitacion=" + habitacion +
                ", fechaEntrada=" + fechaEntrada +
                ", fechaSalida=" + fechaSalida +
                ", estado='" + estado + '\'' +
                ", servicios=" + servicios +
                '}';
    }
}
