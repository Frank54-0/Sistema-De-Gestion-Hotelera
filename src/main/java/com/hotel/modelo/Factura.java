package com.hotel.modelo;

import java.io.Serializable;
import java.util.UUID;

/**
 * Representa una factura emitida por una reserva.
 * Contiene los detalles de cobro, monto total y forma de pago.
 * Implementa Serializable para permitir su persistencia en archivos .dat.
 */
public class Factura implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Estado: la factura aún no ha sido procesada por ProcesadorFacturas. */
    public static final String PENDIENTE = "PENDIENTE";
    /** Estado: la factura ya fue generada y persistida. */
    public static final String PROCESADA = "PROCESADA";

    private final String id;
    private Reserva reserva;
    private double total;
    private String formaPago;
    private String estado;

    /**
     * Constructor de Factura.
     * El id se genera automáticamente mediante UUID.
     * El total se calcula automáticamente a partir de la reserva.
     * La factura nace en estado PENDIENTE hasta que el ProcesadorFacturas la
     * procese.
     *
     * @param reserva   reserva asociada a la factura
     * @param formaPago forma de pago (EFECTIVO, TARJETA, TRANSFERENCIA, etc.)
     */
    public Factura(Reserva reserva, String formaPago) {
        this.id = UUID.randomUUID().toString();
        this.reserva = reserva;
        this.total = reserva.calcularTotal();
        this.formaPago = formaPago;
        this.estado = PENDIENTE;
    }

    /**
     * Obtiene el estado de procesamiento de la factura.
     *
     * @return PENDIENTE o PROCESADA
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Establece el estado de procesamiento de la factura.
     *
     * @param estado nuevo estado (PENDIENTE, PROCESADA)
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Obtiene el id único de la factura.
     *
     * @return identificador único de la factura
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene la reserva asociada a la factura.
     *
     * @return reserva vinculada a esta factura
     */
    public Reserva getReserva() {
        return reserva;
    }

    /**
     * Obtiene el monto total de la factura.
     *
     * @return total a pagar
     */
    public double getTotal() {
        return total;
    }

    /**
     * Obtiene la forma de pago de la factura.
     *
     * @return forma de pago utilizada
     */
    public String getFormaPago() {
        return formaPago;
    }

    /**
     * Establece la forma de pago de la factura.
     *
     * @param formaPago nueva forma de pago
     */
    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    /**
     * Genera la factura: marca su estado como PROCESADA.
     * En el sistema, esta operación es realizada por ProcesadorFacturas
     * en un hilo aparte, ya que simula tareas complicadas como:
     * (armar PDF, enviar email, etc.).
     */
    public void generarFactura() {
        this.estado = PROCESADA;
    }

    @Override
    public String toString() {
        return "Factura{" +
                "id='" + id + '\'' +
                ", reserva=" + reserva.getId() +
                ", total=" + total +
                ", formaPago='" + formaPago + '\'' +
                '}';
    }
}
