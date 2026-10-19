package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Factura;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.Reserva;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de prueba: implementa el contrato CrudDAO guardando en memoria, sin tocar
 * la carpeta {@code datos/}.
 *
 * <p>Aunque es en memoria, serializa de verdad a un arreglo de bytes. Un DAO
 * que solo guardara la lista devolvería siempre los mismos objetos y ocultaría
 * el error #1 que encontró QA; al deserializar, cada lectura reconstruye
 * instancias nuevas, igual que al reiniciar el programa.</p>
 *
 * @param <T>  tipo de entidad; debe ser Serializable
 * @param <ID> tipo del identificador
 */
public abstract class DAOEnMemoria<T extends Serializable, ID> implements CrudDAO<T, ID> {

    private byte[] almacen;

    /**
     * @param entidad entidad de la que se quiere el identificador
     * @return identificador de la entidad
     */
    protected abstract ID idDe(T entidad);

    @Override
    public synchronized void guardar(T entidad) {
        List<T> entidades = leer();
        entidades.add(entidad);
        escribir(entidades);
    }

    @Override
    public synchronized void actualizar(T entidad) {
        List<T> entidades = leer();
        for (int i = 0; i < entidades.size(); i++) {
            if (idDe(entidades.get(i)).equals(idDe(entidad))) {
                entidades.set(i, entidad);
                escribir(entidades);
                return;
            }
        }
        throw new PersistenciaException("No existe la entidad a actualizar: " + idDe(entidad));
    }

    @Override
    public synchronized Optional<T> buscarPorId(ID id) {
        for (T entidad : leer()) {
            if (idDe(entidad).equals(id)) {
                return Optional.of(entidad);
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<T> listarTodos() {
        return leer();
    }

    @Override
    public synchronized boolean eliminar(ID id) {
        List<T> entidades = leer();
        boolean elimino = entidades.removeIf(entidad -> idDe(entidad).equals(id));
        if (elimino) {
            escribir(entidades);
        }
        return elimino;
    }

    private void escribir(List<T> entidades) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(new ArrayList<T>(entidades));
            salida.flush();
            almacen = bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> leer() {
        if (almacen == null) {
            return new ArrayList<T>();
        }
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(almacen))) {
            return (List<T>) entrada.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** DAO falso de reservas. */
    public static class ReservaEnMemoria extends DAOEnMemoria<Reserva, String> implements ReservaDAO {
        @Override
        protected String idDe(Reserva reserva) {
            return reserva.getId();
        }
    }

    /** DAO falso de habitaciones; el identificador es el número. */
    public static class HabitacionEnMemoria extends DAOEnMemoria<Habitacion, Integer> implements HabitacionDAO {
        @Override
        protected Integer idDe(Habitacion habitacion) {
            return habitacion.getNumero();
        }
    }

    /** DAO falso de facturas. */
    public static class FacturaEnMemoria extends DAOEnMemoria<Factura, String> implements FacturaDAO {
        @Override
        protected String idDe(Factura factura) {
            return factura.getId();
        }
    }
}