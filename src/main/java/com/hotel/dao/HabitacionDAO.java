package com.hotel.dao;

import com.hotel.modelo.Habitacion;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de acceso a datos (DAO) para la entidad Habitacion.
 * Persiste y recupera la lista de habitaciones (Sencilla, Doble, Suite)
 * usando serialización de objetos en un archivo binario .dat.
 */
public class HabitacionDAO {

    private static final String RUTA_ARCHIVO = "datos/habitaciones.dat";

    /**
     * Constructor de HabitacionDAO.
     * Se asegura de que la carpeta de datos exista antes de operar.
     */
    public HabitacionDAO() {
        File carpeta = new File("datos");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    /**
     * Guarda la lista completa de habitaciones en habitaciones.dat.
     * Gracias al polimorfismo, cada subclase (HabitacionSencilla, HabitacionDoble, Suite)
     * se serializa correctamente sin necesidad de lógica adicional.
     *
     * @param habitaciones lista de habitaciones a persistir
     */
    public synchronized void guardarHabitaciones(List<Habitacion> habitaciones) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RUTA_ARCHIVO))) {
            oos.writeObject(new ArrayList<>(habitaciones));
        } catch (IOException e) {
            System.err.println("Error al guardar habitaciones en " + RUTA_ARCHIVO + ": " + e.getMessage());
        }
    }

    /**
     * Carga la lista completa de habitaciones desde habitaciones.dat.
     * Si el archivo no existe todavía, retorna una lista vacía.
     *
     * @return lista de habitaciones persistidas, o lista vacía si no hay datos previos
     */
    @SuppressWarnings("unchecked")
    public synchronized List<Habitacion> cargarHabitaciones() {
        File archivo = new File(RUTA_ARCHIVO);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
            return (List<Habitacion>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al cargar habitaciones desde " + RUTA_ARCHIVO + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Agrega una única habitación y persiste la lista actualizada.
     *
     * @param habitacion habitación a agregar y persistir
     */
    public synchronized void guardarHabitacion(Habitacion habitacion) {
        List<Habitacion> habitaciones = cargarHabitaciones();
        habitaciones.add(habitacion);
        guardarHabitaciones(habitaciones);
    }

    /**
     * Busca una habitación por su número dentro de los datos persistidos.
     *
     * @param numero número de la habitación
     * @return la habitación si existe, null en caso contrario
     */
    public synchronized Habitacion buscarPorNumero(int numero) {
        for (Habitacion habitacion : cargarHabitaciones()) {
            if (habitacion.getNumero() == numero) {
                return habitacion;
            }
        }
        return null;
    }
}
