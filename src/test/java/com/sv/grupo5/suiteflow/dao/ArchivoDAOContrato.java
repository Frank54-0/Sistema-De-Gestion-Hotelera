package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contrato CRUD más las garantías propias de un DAO respaldado por archivos:
 * persistencia entre instancias, tolerancia a archivos corruptos y
 * concurrencia entre instancias distintas.
 *
 * <p>Estos DAO usan nombres de archivo fijos ({@code datos/habitaciones.dat},
 * etc.). Para no destruir los datos reales de quien corre las pruebas, antes de
 * cada una se aparta el archivo real y al terminar se restaura.</p>
 */
abstract class ArchivoDAOContrato<T extends Serializable, ID> extends CrudDAOContrato<T, ID> {

    private Path respaldo;

    /** @return nombre del archivo .dat del DAO, por ejemplo "habitaciones.dat" */
    protected abstract String nombreArchivo();

    private Path rutaArchivo() {
        return Paths.get("datos", nombreArchivo());
    }

    @Override
    protected void prepararAlmacen() {
        try {
            Files.createDirectories(Paths.get("datos"));
            respaldo = null;
            if (Files.exists(rutaArchivo())) {
                respaldo = Files.createTempFile("respaldo-", ".dat");
                Files.copy(rutaArchivo(), respaldo, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(rutaArchivo());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void limpiarAlmacen() {
        try {
            Files.deleteIfExists(rutaArchivo());
            if (respaldo != null) {
                Files.move(respaldo, rutaArchivo(), StandardCopyOption.REPLACE_EXISTING);
                respaldo = null;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Archivo: lo guardado lo ve otra instancia del DAO (sobrevive a un reinicio)")
    void persistenciaEntreInstancias() {
        T entidad = nueva(1);
        dao.guardar(entidad);

        CrudDAO<T, ID> otraInstancia = crearDAO();

        assertTrue(otraInstancia.buscarPorId(idDe(entidad)).isPresent());
        assertEquals(1, otraInstancia.listarTodos().size());
    }

    @Test
    @DisplayName("Archivo: un archivo corrupto lanza PersistenciaException al leer")
    void archivoCorruptoLanzaExcepcion() throws IOException {
        Files.write(rutaArchivo(), new byte[] {9, 8, 7, 6, 5, 4, 3});

        assertThrows(PersistenciaException.class, () -> dao.listarTodos());
        assertThrows(PersistenciaException.class, () -> dao.guardar(nueva(1)));
    }

    @Test
    @DisplayName("Archivo CONCURRENCIA: instancias distintas guardando a la vez no pierden datos")
    void guardadoConcurrenteDesdeInstanciasDistintas() throws Exception {
        final int total = 30;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        final CountDownLatch salida = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<Future<Void>>();

        for (int i = 0; i < total; i++) {
            final int n = i;
            futuros.add(pool.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    salida.await();
                    crearDAO().guardar(nueva(n));
                    return null;
                }
            }));
        }
        salida.countDown();
        for (Future<Void> futuro : futuros) {
            futuro.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        List<T> guardadas = dao.listarTodos();
        Set<ID> idsDistintos = new HashSet<ID>();
        for (T entidad : guardadas) {
            idsDistintos.add(idDe(entidad));
        }
        assertEquals(total, guardadas.size(), "No debe perderse ninguna escritura");
        assertEquals(total, idsDistintos.size(), "No debe haber duplicados");
    }
}
