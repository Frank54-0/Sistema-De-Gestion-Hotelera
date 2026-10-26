package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contrato que TODA implementación de {@link CrudDAO} debe cumplir, sea en
 * memoria, en archivos o (más adelante) JDBC.
 *
 * <p>Es una clase base abstracta: cada subclase concreta indica cómo crear su
 * DAO y sus entidades de ejemplo, y hereda todas las pruebas. Así el mismo
 * conjunto de pruebas valida a {@code DAOEnMemoria}, a los {@code *ArchivoDAO}
 * y al futuro DAO de MySQL.</p>
 *
 * @param <T>  tipo de entidad
 * @param <ID> tipo del identificador
 */
abstract class CrudDAOContrato<T extends Serializable, ID> {

    protected CrudDAO<T, ID> dao;

    /** @return un DAO nuevo y vacío */
    protected abstract CrudDAO<T, ID> crearDAO();

    /** @return entidad de ejemplo; cada valor de n produce un id distinto */
    protected abstract T nueva(int n);

    /** @return identificador de la entidad */
    protected abstract ID idDe(T entidad);

    /** @return la misma entidad (mismo id) con algún dato cambiado */
    protected abstract T modificar(T original);

    /** @return true si la entidad refleja el cambio hecho por {@link #modificar} */
    protected abstract boolean estaModificada(T entidad);

    /** Se ejecuta antes de crear el DAO; las subclases la usan para aislar el almacenamiento. */
    protected void prepararAlmacen() {
    }

    /** Se ejecuta al terminar cada prueba; las subclases la usan para limpiar. */
    protected void limpiarAlmacen() {
    }

    @BeforeEach
    void prepararDAO() {
        prepararAlmacen();
        dao = crearDAO();
    }

    @AfterEach
    void terminarPrueba() {
        limpiarAlmacen();
    }

    private List<ID> ids(List<T> entidades) {
        List<ID> ids = new ArrayList<ID>();
        for (T entidad : entidades) {
            ids.add(idDe(entidad));
        }
        return ids;
    }

    @Test
    @DisplayName("Contrato: un DAO nuevo lista una colección vacía, nunca null")
    void listarTodosVacio() {
        assertNotNull(dao.listarTodos());
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("Contrato: guardar y buscarPorId devuelve la entidad guardada")
    void guardarYBuscar() {
        T entidad = nueva(1);
        dao.guardar(entidad);

        Optional<T> encontrada = dao.buscarPorId(idDe(entidad));

        assertTrue(encontrada.isPresent());
        assertEquals(idDe(entidad), idDe(encontrada.get()));
    }

    @Test
    @DisplayName("Contrato: buscarPorId de un id inexistente devuelve Optional vacío")
    void buscarInexistente() {
        dao.guardar(nueva(1));

        assertFalse(dao.buscarPorId(idDe(nueva(2))).isPresent());
    }

    @Test
    @DisplayName("Contrato: buscarPorId(null) devuelve Optional vacío y no lanza excepción")
    void buscarPorIdNull() {
        dao.guardar(nueva(1));

        assertFalse(dao.buscarPorId(null).isPresent());
    }

    @Test
    @DisplayName("Contrato: listarTodos conserva el orden de inserción")
    void listarConservaOrden() {
        T a = nueva(1);
        T b = nueva(2);
        T c = nueva(3);
        dao.guardar(a);
        dao.guardar(b);
        dao.guardar(c);

        List<ID> esperados = new ArrayList<ID>();
        esperados.add(idDe(a));
        esperados.add(idDe(b));
        esperados.add(idDe(c));
        assertEquals(esperados, ids(dao.listarTodos()));
    }

    @Test
    @DisplayName("Contrato: actualizar reemplaza la entidad sin duplicarla")
    void actualizarReemplaza() {
        T entidad = nueva(1);
        dao.guardar(entidad);

        dao.actualizar(modificar(entidad));

        assertEquals(1, dao.listarTodos().size());
        assertTrue(estaModificada(dao.buscarPorId(idDe(entidad)).get()));
    }

    @Test
    @DisplayName("Contrato: actualizar una entidad que no existe lanza PersistenciaException")
    void actualizarInexistente() {
        dao.guardar(nueva(1));

        assertThrows(PersistenciaException.class, () -> dao.actualizar(nueva(9)));
        assertEquals(1, dao.listarTodos().size(), "No debe haberse creado nada");
    }

    @Test
    @DisplayName("Contrato: eliminar una entidad existente devuelve true y desaparece")
    void eliminarExistente() {
        T entidad = nueva(1);
        dao.guardar(entidad);

        assertTrue(dao.eliminar(idDe(entidad)));
        assertFalse(dao.buscarPorId(idDe(entidad)).isPresent());
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("Contrato: eliminar algo inexistente (o null) devuelve false")
    void eliminarInexistente() {
        dao.guardar(nueva(1));

        assertFalse(dao.eliminar(idDe(nueva(2))));
        assertFalse(dao.eliminar(null));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    @DisplayName("Contrato: eliminar solo quita la entidad indicada")
    void eliminarSoloLaIndicada() {
        T a = nueva(1);
        T b = nueva(2);
        T c = nueva(3);
        dao.guardar(a);
        dao.guardar(b);
        dao.guardar(c);

        assertTrue(dao.eliminar(idDe(b)));

        List<ID> esperados = new ArrayList<ID>();
        esperados.add(idDe(a));
        esperados.add(idDe(c));
        assertEquals(esperados, ids(dao.listarTodos()));
    }

    @Test
    @DisplayName("Contrato: la lista devuelta por listarTodos es una copia; modificarla no altera lo guardado")
    void listarTodosDevuelveCopia() {
        dao.guardar(nueva(1));

        List<T> lista = dao.listarTodos();
        lista.clear();

        assertEquals(1, dao.listarTodos().size());
    }
}
