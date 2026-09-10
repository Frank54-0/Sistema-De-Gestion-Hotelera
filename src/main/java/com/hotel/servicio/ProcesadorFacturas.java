package com.hotel.servicio;

import com.hotel.dao.FacturaDAO;
import com.hotel.modelo.Factura;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Hilo que procesa facturas en segundo plano, de forma asíncrona respecto al
 * flujo principal del sistema.
 *
 * <b>Justificación técnica de la concurrencia.</b> Generar una factura en un
 * hotel real no es instantáneo: hay que armar el documento, calcular impuestos
 * y enviarlo al cliente. Si eso se hiciera de forma síncrona, el recepcionista
 * quedaría bloqueado en cada cobro. Aquí las facturas se encolan y este hilo
 * dedicado las va procesando, sin detener el resto de operaciones del hotel.
 *
 * <b>Coordinación entre hilos.</b> El hilo espera con wait() cuando la cola
 * está vacía y es despertado con notifyAll() al encolarse una factura o al
 * pedirse el apagado. El trabajo pesado (procesarFactura) se ejecuta FUERA
 * del bloque sincronizado, de modo que quien encola nunca queda esperando a
 * que termine una factura en curso.
 */
public class ProcesadorFacturas extends Thread {

    /** Milisegundos que simulan el costo real de generar y enviar la factura. */
    private static final long DURACION_PROCESO_MS = 1500L;

    private final Queue<Factura> colaFacturas = new LinkedList<Factura>();
    private final FacturaDAO facturaDAO;

    private boolean ejecutando = true;
    private int totalProcesadas = 0;

    /** Construye el procesador con el DAO de facturas por defecto. */
    public ProcesadorFacturas() {
        this(new FacturaDAO());
    }

    /**
     * Construye el procesador con un DAO indicado por el llamador.
     * Permite inyectar un DAO de prueba en los tests unitarios.
     *
     * @param facturaDAO DAO encargado de persistir las facturas procesadas
     */
    public ProcesadorFacturas(FacturaDAO facturaDAO) {
        super("Hilo-ProcesadorFacturas");
        if (facturaDAO == null) {
            throw new IllegalArgumentException("El DAO de facturas no puede ser null");
        }
        this.facturaDAO = facturaDAO;
    }

    /**
     * Encola una factura para su procesamiento y despierta al hilo si estaba
     * esperando. No bloquea al llamador.
     *
     * @param factura factura pendiente de procesar; se ignora si es null
     */
    public synchronized void encolarFactura(Factura factura) {
        if (factura == null) {
            return;
        }
        colaFacturas.add(factura);
        System.out.println("[ProcesadorFacturas] Factura " + factura.getId() + " encolada.");
        notifyAll();
    }

    /**
     * Bucle principal del hilo. Espera mientras la cola esté vacía, procesa
     * cada factura que llegue y termina de forma ordenada cuando se solicita
     * el apagado y ya no quedan facturas pendientes.
     */
    @Override
    public void run() {
        while (true) {
            Factura factura = tomarSiguienteOEsperar();
            if (factura == null) {
                break; // apagado solicitado y cola vacía
            }
            procesarFactura(factura);
        }
        System.out.println("[ProcesadorFacturas] Hilo detenido. Facturas procesadas: " + totalProcesadas);
    }

    /**
     * Extrae la siguiente factura de la cola. Si la cola está vacía y el hilo
     * sigue activo, espera hasta que llegue una o se pida el apagado.
     *
     * @return la siguiente factura, o null si corresponde terminar el hilo
     */
    private synchronized Factura tomarSiguienteOEsperar() {
        while (colaFacturas.isEmpty() && ejecutando) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return colaFacturas.poll();
    }

    /**
     * Genera y persiste una factura, simulando el costo real de la operación.
     *
     * @param factura factura a procesar
     */
    private void procesarFactura(Factura factura) {
        try {
            System.out.println("[ProcesadorFacturas] Procesando factura " + factura.getId() + "...");
            Thread.sleep(DURACION_PROCESO_MS);
            factura.generarFactura();
            facturaDAO.guardarFactura(factura);
            registrarProcesada();
            System.out.println("[ProcesadorFacturas] Factura " + factura.getId()
                    + " procesada. Total: $" + String.format("%.2f", factura.getTotal()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Incrementa el contador de facturas procesadas de forma segura. */
    private synchronized void registrarProcesada() {
        totalProcesadas++;
    }

    /**
     * Solicita el apagado ordenado: el hilo terminará una vez procesadas las
     * facturas que ya estaban en la cola.
     */
    public synchronized void detener() {
        this.ejecutando = false;
        notifyAll();
    }

    /**
     * Cantidad de facturas efectivamente procesadas por este hilo.
     * Útil para validar en pruebas que la cola se vació por completo.
     *
     * @return número de facturas procesadas
     */
    public synchronized int getTotalProcesadas() {
        return totalProcesadas;
    }

    /**
     * Cantidad de facturas que aún esperan en la cola.
     *
     * @return número de facturas pendientes
     */
    public synchronized int getPendientes() {
        return colaFacturas.size();
    }
}
