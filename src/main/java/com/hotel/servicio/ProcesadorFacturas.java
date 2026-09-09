package com.hotel.servicio;

import com.hotel.dao.FacturaDAO;
import com.hotel.modelo.Factura;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Hilo (Thread) que procesa facturas en segundo plano, de forma asíncrona
 * respecto al flujo principal del sistema.
 *
 * Justificación técnica de la concurrencia:
 * En un hotel real, "generar" una factura no es instantáneo: implica armar
 * el documento, calcular impuestos/descuentos, enviarla por correo al
 * cliente, etc. Si esa operación se hiciera de forma síncrona cada vez que
 * se crea una factura, el recepcionista (o el hilo principal del sistema)
 * quedaría bloqueado esperando. Por eso, las facturas se encolan y un hilo
 * dedicado (ProcesadorFacturas) las va procesando en paralelo, sin detener
 * el resto de operaciones del hotel (nuevas reservas, consultas, etc.).
 *
 * La cola interna se protege con métodos synchronized porque puede ser
 * escrita por el hilo principal (al encolar una factura nueva) mientras
 * este hilo la está leyendo para procesar la siguiente.
 */
public class ProcesadorFacturas extends Thread {

    private final Queue<Factura> colaFacturas;
    private final FacturaDAO facturaDAO;
    private volatile boolean ejecutando;

    /**
     * Constructor de ProcesadorFacturas.
     * Inicializa la cola de facturas pendientes y el DAO de persistencia.
     */
    public ProcesadorFacturas() {
        super("Hilo-ProcesadorFacturas");
        this.colaFacturas = new LinkedList<>();
        this.facturaDAO = new FacturaDAO();
        this.ejecutando = true;
    }

    /**
     * Agrega una factura a la cola de procesamiento pendiente.
     * Sincronizado porque el hilo principal puede llamarlo mientras
     * este hilo está leyendo/removiendo de la misma cola.
     *
     * @param factura factura pendiente de procesar
     */
    public synchronized void encolarFactura(Factura factura) {
        colaFacturas.add(factura);
        System.out.println("[ProcesadorFacturas] Factura " + factura.getId() + " encolada para procesamiento.");
        notifyAll();
    }

    /**
     * Bucle principal del hilo: mientras esté en ejecución, revisa la cola,
     * y si hay facturas pendientes las procesa una por una simulando el
     * costo real de la operación (armar documento, enviar notificación, etc.)
     * y luego las persiste como PROCESADA mediante FacturaDAO.
     */
    @Override
    public void run() {
        while (ejecutando || !colaVacia()) {
            Factura factura = obtenerSiguiente();
            if (factura != null) {
                procesarFactura(factura);
            } else {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("[ProcesadorFacturas] Hilo detenido. No quedan facturas pendientes.");
    }

    /**
     * Obtiene y remueve la siguiente factura de la cola, de forma sincronizada.
     *
     * @return la siguiente factura pendiente, o null si la cola está vacía
     */
    private synchronized Factura obtenerSiguiente() {
        return colaFacturas.poll();
    }

    /**
     * Indica si la cola de facturas pendientes está vacía.
     *
     * @return true si no hay facturas pendientes
     */
    private synchronized boolean colaVacia() {
        return colaFacturas.isEmpty();
    }

    /**
     * Simula el procesamiento real de una factura (tarea costosa) y
     * la marca como PROCESADA, persistiéndola con FacturaDAO.
     *
     * @param factura factura a procesar
     */
    private void procesarFactura(Factura factura) {
        try {
            System.out.println("[ProcesadorFacturas] Procesando factura " + factura.getId() + "...");
            Thread.sleep(1500); // simula generación de documento / envío de correo
            factura.generarFactura(); // marca estado = PROCESADA
            facturaDAO.guardarFactura(factura);
            System.out.println("[ProcesadorFacturas] Factura " + factura.getId() + " procesada y guardada. Total: $"
                    + String.format("%.2f", factura.getTotal()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Señala al hilo que termine su ejecución una vez procesadas
     * las facturas pendientes en la cola (apagado ordenado).
     */
    public void detener() {
        this.ejecutando = false;
    }
}
