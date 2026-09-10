package com.hotel.dao;

import com.hotel.excepciones.PersistenciaException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clase base genérica de la capa DAO.
 * Concentra toda la mecánica de serialización sobre archivos .dat,
 * para que cada DAO concreto (ClienteDAO, HabitacionDAO, ReservaDAO,
 * FacturaDAO)
 * solo tenga que indicar su nombre de archivo y exponer sus métodos de negocio.
 * 
 * @param <T> tipo de entidad que persiste este DAO. Debe ser Serializable.
 */
public abstract class ArchivoDAO<T extends java.io.Serializable> {

    /** Carpeta donde viven todos los archivos .dat del sistema. */
    protected static final String CARPETA_DATOS = "datos";

    private final File archivo;

    /**
     * Construye el DAO y garantiza que la carpeta de datos exista.
     * 
     * @param nombreArchivo nombre del archivo .dat
     */
    protected ArchivoDAO(String nombreArchivo) {
        File carpeta = new File(CARPETA_DATOS);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new PersistenciaException(
                    "No se pudo crear la carpeta de datos: " + carpeta.getAbsolutePath());
        }
        this.archivo = new File(carpeta, nombreArchivo);
    }

    /**
     * Guarda la lista completa en el archivo, sobrescribiendo lo anterior.
     * Se copia la lista recibida antes de serializar para que un cambio
     * concurrente sobre ella no corrompa la escritura.
     *
     * @param entidades lista de entidades a persistir; null se trata como lista
     *                  vacía
     * @throws PersistenciaException si ocurre un error de escritura
     */
    protected synchronized void guardarTodos(List<T> entidades) {
        List<T> copia = (entidades == null) ? new ArrayList<T>() : new ArrayList<T>(entidades);
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivo))) {
            salida.writeObject(copia);
        } catch (IOException e) {
            throw new PersistenciaException(
                    "Error al guardar en " + archivo.getPath(), e);
        }
    }

    /**
     * Carga la lista completa desde el archivo.
     * Si el archivo aún no existe devuelve
     * una lista vacía, que es una situación normal y no un error.
     *
     * @return lista de entidades persistidas; nunca null
     * @throws PersistenciaException si el archivo existe pero no se puede leer
     */
    @SuppressWarnings("unchecked")
    protected synchronized List<T> cargarTodos() {
        if (!archivo.exists()) {
            return new ArrayList<T>();
        }
        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(archivo))) {
            return (List<T>) entrada.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenciaException(
                    "Error al leer " + archivo.getPath()
                            + ". El archivo puede estar corrupto o pertenecer a otra versión del modelo.",
                    e);
        }
    }

    /**
     * Agrega una entidad al final de la lista persistida.
     *
     * @param entidad entidad a agregar; se ignora si es null
     */
    protected synchronized void agregar(T entidad) {
        if (entidad == null) {
            return;
        }
        List<T> entidades = cargarTodos();
        entidades.add(entidad);
        guardarTodos(entidades);
    }

    /**
     * Devuelve una vista de solo lectura de la lista persistida.
     * Es útil para consultas ya que evita que el llamador modifique los datos por
     * accidente.
     *
     * @return lista inmutable de entidades
     */
    protected synchronized List<T> consultarTodos() {
        return Collections.unmodifiableList(cargarTodos());
    }

    /**
     * Indica si el archivo de este DAO ya existe en disco.
     *
     * @return true si hay datos persistidos previamente
     */
    public synchronized boolean existeArchivo() {
        return archivo.exists();
    }

    /**
     * Ruta del archivo administrado por este DAO. Útil para mensajes de log.
     *
     * @return ruta relativa del archivo .dat
     */
    public String getRutaArchivo() {
        return archivo.getPath();
    }
}
