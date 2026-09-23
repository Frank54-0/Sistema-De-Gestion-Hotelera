package com.hotel.modelo;

import java.io.Serializable;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Reserva de una habitación por parte de un cliente, con sus fechas, estado y
 * servicios adicionales.
 *
 * <p>La habitación no se persiste como objeto: solo se guarda su número y
 * {@link com.hotel.servicio.GestorReservas} la reenlaza con el inventario al
 * cargar. Si se serializara completa, {@code reservas.dat} y
 * {@code habitaciones.dat} reconstruirían dos copias independientes de la misma
 * habitación y sus estados divergirían (hallazgo #1 de QA).</p>
 */
public class Reserva implements Serializable {

    // v2: la habitación dejó de serializarse. Los reservas.dat de v1 no son compatibles.
    private static final long serialVersionUID = 2L;

    public static final String ACTIVA = "ACTIVA";
    public static final String CANCELADA = "CANCELADA";
    public static final String FINALIZADA = "FINALIZADA";

    private final String id;
    private Cliente cliente;
    private transient Habitacion habitacion;
    private final int numeroHabitacion;
    private Date fechaEntrada;
    private Date fechaSalida;
    private String estado;
    private List<ServicioAdicional> servicios;

    /**
     * Crea una reserva en estado {@link #ACTIVA} con un id UUID generado.
     *
     * @param cliente      cliente que realiza la reserva
     * @param habitacion   habitación a reservar
     * @param fechaEntrada fecha de check-in
     * @param fechaSalida  fecha de check-out
     * @throws IllegalArgumentException si la habitación es null
     */
    public Reserva(Cliente cliente, Habitacion habitacion, Date fechaEntrada, Date fechaSalida) {
        this.id = UUID.randomUUID().toString();
        this.cliente = cliente;
        if (habitacion == null) {
            throw new IllegalArgumentException("La reserva requiere una habitación");
        }
        this.habitacion = habitacion;
        this.numeroHabitacion = habitacion.getNumero();
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.estado = ACTIVA;
        this.servicios = new ArrayList<>();
    }

    /** @return identificador único de la reserva */
    public String getId() {
        return id;
    }

    /** @return cliente que realizó la reserva */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * @return habitación reservada, o null si la reserva se acaba de cargar desde
     *         disco y aún no fue reenlazada
     */
    public Habitacion getHabitacion() {
        return habitacion;
    }

    /**
     * Número de la habitación reservada. A diferencia de {@link #getHabitacion()},
     * siempre tiene valor, incluso antes de reenlazar.
     *
     * @return número de la habitación
     */
    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }

    /**
     * Reenlaza la reserva con la habitación del inventario tras cargarla desde
     * disco. Pensado para uso exclusivo de GestorReservas.
     *
     * @param habitacion habitación del inventario con el mismo número
     * @throws IllegalArgumentException si es null o su número no coincide
     */
    public void setHabitacion(Habitacion habitacion) {
        if (habitacion == null || habitacion.getNumero() != numeroHabitacion) {
            throw new IllegalArgumentException(
                    "Solo se puede enlazar la habitación número " + numeroHabitacion);
        }
        this.habitacion = habitacion;
    }

    /** @return fecha de check-in */
    public Date getFechaEntrada() {
        return fechaEntrada;
    }

    /** @return fecha de check-out */
    public Date getFechaSalida() {
        return fechaSalida;
    }

    /** @return estado actual: ACTIVA, CANCELADA o FINALIZADA */
    public String getEstado() {
        return estado;
    }

    /** @return vista de solo lectura de los servicios adicionales */
    public List<ServicioAdicional> getServicios() {
        return Collections.unmodifiableList(servicios);
    }

    /**
     * Agrega un servicio adicional a la reserva.
     *
     * @param servicio servicio a agregar; se ignora si es null
     */
    public void agregarServicio(ServicioAdicional servicio) {
        if (servicio != null) {
            servicios.add(servicio);
        }
    }

    /**
     * Calcula el costo de la habitación más los servicios adicionales.
     *
     * <p>Usa factor de demanda 1.0 de forma fija; conectar el factor real está
     * pendiente de definición con el Architect (hallazgo #5 de QA).</p>
     *
     * @return costo total de la reserva
     * @throws IllegalStateException si la reserva aún no fue reenlazada con su
     *                               habitación
     */
    public double calcularTotal() {
        if (habitacion == null) {
            throw new IllegalStateException(
                    "La reserva " + id + " no está enlazada con la habitación " + numeroHabitacion);
        }
        double total = habitacion.calcularPrecio(1.0);
        for (ServicioAdicional servicio : servicios) {
            total += servicio.getCosto();
        }
        return total;
    }

    /** Cambia el estado de la reserva a {@link #CANCELADA}. */
    public void cancelar() {
        this.estado = CANCELADA;
    }

    /** @return true si el estado es ACTIVA */
    public boolean estaActiva() {
        return ACTIVA.equals(estado);
    }

    @Override
    public String toString() {
        return "Reserva{" +
                "id='" + id + '\'' +
                ", cliente=" + cliente +
                ", habitacion=" + (habitacion != null ? habitacion : "N° " + numeroHabitacion) +
                ", fechaEntrada=" + fechaEntrada +
                ", fechaSalida=" + fechaSalida +
                ", estado='" + estado + '\'' +
                ", servicios=" + servicios +
                '}';
    }
}
