package com.hotel.dao;

import com.hotel.modelo.Factura;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de acceso a datos (DAO) para la entidad Factura.
 * Persiste y recupera la lista de facturas usando serialización
 * de objetos en un archivo binario .dat.
 *
 * Es utilizada tanto por la capa de servicio como por ProcesadorFacturas
 * (hilo que procesa facturas en segundo plano), por lo que todos sus
 * métodos son synchronized para evitar condiciones de carrera al
 * leer/escribir el archivo desde varios hilos a la vez.
 */
public class FacturaDAO {

    private static final String RUTA_ARCHIVO = "datos/facturas.dat";

    /**
     * Constructor de FacturaDAO.
     * Se asegura de que la carpeta de datos exista antes de operar.
     */
    public FacturaDAO() {
        File carpeta = new File("datos");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    /**
     * Guarda la lista completa de facturas en facturas.dat.
     *
     * @param facturas lista de facturas a persistir
     */
    public synchronized void guardarFacturas(List<Factura> facturas) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RUTA_ARCHIVO))) {
            oos.writeObject(new ArrayList<>(facturas));
        } catch (IOException e) {
            System.err.println("Error al guardar facturas en " + RUTA_ARCHIVO + ": " + e.getMessage());
        }
    }

    /**
     * Carga la lista completa de facturas desde facturas.dat.
     * Si el archivo no existe todavía, retorna una lista vacía.
     *
     * @return lista de facturas persistidas, o lista vacía si no hay datos previos
     */
    @SuppressWarnings("unchecked")
    public synchronized List<Factura> cargarFacturas() {
        File archivo = new File(RUTA_ARCHIVO);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
            return (List<Factura>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al cargar facturas desde " + RUTA_ARCHIVO + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Agrega una única factura y persiste la lista actualizada.
     * Usado por ProcesadorFacturas cada vez que termina de procesar una factura.
     *
     * @param factura factura a agregar y persistir
     */
    public synchronized void guardarFactura(Factura factura) {
        List<Factura> facturas = cargarFacturas();
        facturas.add(factura);
        guardarFacturas(facturas);
    }
}
