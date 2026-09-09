package com.hotel.dao;

import com.hotel.modelo.Reserva;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de acceso a datos (DAO) para la entidad Reserva.
 * Persiste y recupera la lista de reservas usando serialización
 * de objetos en un archivo binario .dat.
 */
public class ReservaDAO {

    private static final String RUTA_ARCHIVO = "datos/reservas.dat";

    /**
     * Constructor de ReservaDAO.
     * Se asegura de que la carpeta de datos exista antes de operar.
     */
    public ReservaDAO() {
        File carpeta = new File("datos");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    /**
     * Guarda la lista completa de reservas en reservas.dat.
     *
     * @param reservas lista de reservas a persistir
     */
    public synchronized void guardarReservas(List<Reserva> reservas) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RUTA_ARCHIVO))) {
            oos.writeObject(new ArrayList<>(reservas));
        } catch (IOException e) {
            System.err.println("Error al guardar reservas en " + RUTA_ARCHIVO + ": " + e.getMessage());
        }
    }

    /**
     * Carga la lista completa de reservas desde reservas.dat.
     * Si el archivo no existe todavía, retorna una lista vacía.
     *
     * @return lista de reservas persistidas, o lista vacía si no hay datos previos
     */
    @SuppressWarnings("unchecked")
    public synchronized List<Reserva> cargarReservas() {
        File archivo = new File(RUTA_ARCHIVO);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
            return (List<Reserva>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al cargar reservas desde " + RUTA_ARCHIVO + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Agrega una única reserva y persiste la lista actualizada.
     *
     * @param reserva reserva a agregar y persistir
     */
    public synchronized void guardarReserva(Reserva reserva) {
        List<Reserva> reservas = cargarReservas();
        reservas.add(reserva);
        guardarReservas(reservas);
    }

    /**
     * Busca una reserva por su id dentro de los datos persistidos.
     *
     * @param id identificador único de la reserva
     * @return la reserva si existe, null en caso contrario
     */
    public synchronized Reserva buscarPorId(String id) {
        for (Reserva reserva : cargarReservas()) {
            if (reserva.getId().equals(id)) {
                return reserva;
            }
        }
        return null;
    }

    /**
     * Actualiza una reserva existente (por ejemplo tras cancelarla)
     * reemplazando el registro con el mismo id en el archivo persistido.
     *
     * @param reservaActualizada reserva con los datos actualizados
     */
    public synchronized void actualizarReserva(Reserva reservaActualizada) {
        List<Reserva> reservas = cargarReservas();
        for (int i = 0; i < reservas.size(); i++) {
            if (reservas.get(i).getId().equals(reservaActualizada.getId())) {
                reservas.set(i, reservaActualizada);
                break;
            }
        }
        guardarReservas(reservas);
    }
}
