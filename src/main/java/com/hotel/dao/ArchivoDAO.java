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
 * Base genérica de la capa DAO. Concentra la serialización sobre archivos .dat
 * para que cada DAO concreto solo indique su nombre de archivo.
 *
 * <p><b>Concurrencia.</b> Se sincroniza sobre un candado estático y no sobre la
 * instancia: con candado por instancia, dos {@code new HabitacionDAO()} podrían
 * escribir el mismo archivo a la vez y corromperlo (Error #3 encontrado por QA).
 * Un solo candado para todos los archivos es menos eficiente que uno por archivo, pero
 * más simple y suficiente para este sistema.</p>
 *
 * @param <T> tipo de entidad persistida; debe ser Serializable
 */
public abstract class ArchivoDAO<T extends java.io.Serializable> {

    protected static final String CARPETA_DATOS = "datos";

    private static final Object CANDADO_ARCHIVOS = new Object();

    private final File archivo;

    /**
     * Crea la carpeta de datos si no existe.
     *
     * @param nombreArchivo nombre del archivo .dat dentro de {@code datos/}
     * @throws PersistenciaException si no se puede crear la carpeta
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
     * Sobrescribe el archivo con la lista completa.
     *
     * @param entidades entidades a persistir; null se trata como lista vacía
     * @throws PersistenciaException si ocurre un error de escritura
     */
    protected void guardarTodos(List<T> entidades) {
        synchronized (CANDADO_ARCHIVOS) {
            // Se serializa una copia: si otro hilo modifica la lista original durante
            // la escritura, writeObject lanzaría ConcurrentModificationException.
            List<T> copia = (entidades == null) ? new ArrayList<T>() : new ArrayList<T>(entidades);
            try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivo))) {
                salida.writeObject(copia);
            } catch (IOException e) {
                throw new PersistenciaException(
                        "Error al guardar en " + archivo.getPath(), e);
            }
        }
    }

    /**
     * Carga la lista completa. Que el archivo no exista es normal en la primera
     * ejecución y devuelve una lista vacía.
     *
     * @return entidades persistidas; nunca null
     * @throws PersistenciaException si el archivo existe pero no se puede leer
     */
    @SuppressWarnings("unchecked")
    protected List<T> cargarTodos() {
        synchronized (CANDADO_ARCHIVOS) {
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
    }

    /**
     * Agrega una entidad al final de la lista persistida.
     *
     * @param entidad entidad a agregar; se ignora si es null
     */
    protected void agregar(T entidad) {
        // Leer, agregar y guardar bajo el mismo candado; si no, otro hilo podría
        // escribir entre la lectura y la escritura y su cambio se perdería.
        synchronized (CANDADO_ARCHIVOS) {
            if (entidad == null) {
                return;
            }
            List<T> entidades = cargarTodos();
            entidades.add(entidad);
            guardarTodos(entidades);
        }
    }

    /** @return lista de solo lectura con las entidades persistidas */
    protected List<T> consultarTodos() {
        synchronized (CANDADO_ARCHIVOS) {
            return Collections.unmodifiableList(cargarTodos());
        }
    }

    /** @return true si ya hay datos persistidos para este DAO */
    public boolean existeArchivo() {
        synchronized (CANDADO_ARCHIVOS) {
            return archivo.exists();
        }
    }

    /** @return ruta relativa del archivo .dat */
    public String getRutaArchivo() {
        return archivo.getPath();
    }
}
