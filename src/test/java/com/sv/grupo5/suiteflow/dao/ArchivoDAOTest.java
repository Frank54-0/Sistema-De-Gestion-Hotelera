package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de ArchivoDAO: serialización, operaciones genéricas, manejo de
 * archivos corruptos y, sobre todo, la garantía de concurrencia entre
 * INSTANCIAS distintas del mismo DAO (Error #3 reportado por QA).
 *
 * Cada prueba usa un archivo con nombre único dentro de datos/ y lo borra al
 * terminar, así no se tocan los .dat reales del sistema.
 */
class ArchivoDAOTest {

    /** Entidad mínima serializable para probar el DAO genérico. */
    private static class Dato implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int id;
        private final String valor;

        Dato(int id, String valor) {
            this.id = id;
            this.valor = valor;
        }

        int getId() {
            return id;
        }

        String getValor() {
            return valor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dato)) return false;
            Dato otro = (Dato) o;
            return id == otro.id && Objects.equals(valor, otro.valor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, valor);
        }
    }

    /** DAO concreto mínimo; los métodos protected son visibles por estar en el mismo paquete. */
    private static class DatoDAO extends ArchivoDAO<Dato> {
        DatoDAO(String nombreArchivo) {
            super(nombreArchivo);
        }
    }

    private String nombreArchivo;
    private DatoDAO dao;

    @BeforeEach
    void prepararDAO() {
        nombreArchivo = "test-" + UUID.randomUUID() + ".dat";
        dao = new DatoDAO(nombreArchivo);
    }

    @AfterEach
    void limpiarArchivo() throws IOException {
        Files.deleteIfExists(Paths.get(dao.getRutaArchivo()));
    }

    // ------------------------------------------------------------------
    // Construcción y estado inicial
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El constructor garantiza que la carpeta datos/ exista")
    void constructorCreaCarpetaDeDatos() {
        assertTrue(new File("datos").isDirectory());
    }

    @Test
    @DisplayName("Sin archivo previo: existeArchivo es false y cargarTodos devuelve lista vacía")
    void sinArchivoDevuelveListaVacia() {
        assertFalse(dao.existeArchivo());
        assertTrue(dao.cargarTodos().isEmpty());
    }

    @Test
    @DisplayName("getRutaArchivo apunta a datos/<nombre>")
    void rutaApuntaACarpetaDatos() {
        assertEquals(new File("datos", nombreArchivo).getPath(), dao.getRutaArchivo());
    }

    // ------------------------------------------------------------------
    // guardarTodos / cargarTodos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Guardar y cargar devuelve las mismas entidades en el mismo orden")
    void guardarYCargarRoundTrip() {
        List<Dato> originales = Arrays.asList(new Dato(1, "a"), new Dato(2, "b"), new Dato(3, "c"));

        dao.guardarTodos(originales);

        assertTrue(dao.existeArchivo());
        assertEquals(originales, dao.cargarTodos());
    }

    @Test
    @DisplayName("guardarTodos sobrescribe el contenido anterior")
    void guardarSobrescribe() {
        dao.guardarTodos(Arrays.asList(new Dato(1, "a"), new Dato(2, "b")));
        dao.guardarTodos(Arrays.asList(new Dato(9, "z")));

        assertEquals(Arrays.asList(new Dato(9, "z")), dao.cargarTodos());
    }

    @Test
    @DisplayName("guardarTodos(null) se trata como lista vacía")
    void guardarNullPersisteListaVacia() {
        dao.guardarTodos(null);

        assertTrue(dao.existeArchivo());
        assertTrue(dao.cargarTodos().isEmpty());
    }

    @Test
    @DisplayName("Modificar la lista original después de guardar no altera lo persistido")
    void guardarSerializaUnaCopia() {
        List<Dato> original = new ArrayList<Dato>();
        original.add(new Dato(1, "a"));
        dao.guardarTodos(original);

        original.add(new Dato(2, "b"));

        assertEquals(1, dao.cargarTodos().size());
    }

    @Test
    @DisplayName("Otra instancia sobre el mismo archivo lee lo que guardó la primera")
    void otraInstanciaLeeLoPersistido() {
        dao.guardarTodos(Arrays.asList(new Dato(1, "a")));

        DatoDAO otra = new DatoDAO(nombreArchivo);

        assertEquals(Arrays.asList(new Dato(1, "a")), otra.cargarTodos());
    }

    // ------------------------------------------------------------------
    // agregar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("agregar añade la entidad al final de la lista persistida")
    void agregarAlFinal() {
        dao.agregar(new Dato(1, "a"));
        dao.agregar(new Dato(2, "b"));

        assertEquals(Arrays.asList(new Dato(1, "a"), new Dato(2, "b")), dao.cargarTodos());
    }

    @Test
    @DisplayName("agregar(null) se ignora y no crea el archivo")
    void agregarNullSeIgnora() {
        dao.agregar(null);

        assertFalse(dao.existeArchivo());
    }

    // ------------------------------------------------------------------
    // consultarTodos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("consultarTodos devuelve una vista de solo lectura")
    void consultarTodosEsInmutable() {
        dao.agregar(new Dato(1, "a"));

        List<Dato> consulta = dao.consultarTodos();

        assertEquals(1, consulta.size());
        assertThrows(UnsupportedOperationException.class, () -> consulta.add(new Dato(2, "b")));
    }

    // ------------------------------------------------------------------
    // buscarPrimero
    // ------------------------------------------------------------------

    @Test
    @DisplayName("buscarPrimero devuelve la primera coincidencia")
    void buscarPrimeroEncuentra() {
        dao.guardarTodos(Arrays.asList(new Dato(1, "x"), new Dato(2, "x"), new Dato(3, "y")));

        Optional<Dato> encontrado = dao.buscarPrimero(d -> "x".equals(d.getValor()));

        assertTrue(encontrado.isPresent());
        assertEquals(1, encontrado.get().getId());
    }

    @Test
    @DisplayName("buscarPrimero devuelve Optional vacío si nada coincide")
    void buscarPrimeroSinCoincidencia() {
        dao.agregar(new Dato(1, "a"));

        assertFalse(dao.buscarPrimero(d -> d.getId() == 99).isPresent());
    }

    // ------------------------------------------------------------------
    // reemplazarPrimero
    // ------------------------------------------------------------------

    @Test
    @DisplayName("reemplazarPrimero sustituye la entidad y conserva el orden")
    void reemplazarConservaOrden() {
        dao.guardarTodos(Arrays.asList(new Dato(1, "a"), new Dato(2, "b"), new Dato(3, "c")));

        boolean reemplazado = dao.reemplazarPrimero(d -> d.getId() == 2, new Dato(2, "B-nuevo"));

        assertTrue(reemplazado);
        assertEquals(Arrays.asList(new Dato(1, "a"), new Dato(2, "B-nuevo"), new Dato(3, "c")),
                dao.cargarTodos());
    }

    @Test
    @DisplayName("reemplazarPrimero devuelve false y no cambia nada si no hay coincidencia")
    void reemplazarSinCoincidencia() {
        dao.guardarTodos(Arrays.asList(new Dato(1, "a")));

        assertFalse(dao.reemplazarPrimero(d -> d.getId() == 99, new Dato(99, "x")));
        assertEquals(Arrays.asList(new Dato(1, "a")), dao.cargarTodos());
    }

    // ------------------------------------------------------------------
    // eliminarSi
    // ------------------------------------------------------------------

    @Test
    @DisplayName("eliminarSi elimina todas las coincidencias y persiste el resultado")
    void eliminarVariasCoincidencias() {
        dao.guardarTodos(Arrays.asList(new Dato(1, "x"), new Dato(2, "y"), new Dato(3, "x")));

        boolean elimino = dao.eliminarSi(d -> "x".equals(d.getValor()));

        assertTrue(elimino);
        assertEquals(Arrays.asList(new Dato(2, "y")), dao.cargarTodos());
    }

    @Test
    @DisplayName("eliminarSi devuelve false si nada coincide")
    void eliminarSinCoincidencia() {
        dao.guardarTodos(Arrays.asList(new Dato(1, "a")));

        assertFalse(dao.eliminarSi(d -> d.getId() == 99));
        assertEquals(1, dao.cargarTodos().size());
    }

    // ------------------------------------------------------------------
    // Errores de persistencia
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un archivo corrupto lanza PersistenciaException al leer")
    void archivoCorruptoLanzaExcepcion() throws IOException {
        Files.write(Paths.get(dao.getRutaArchivo()),
                "esto no es un objeto serializado".getBytes(StandardCharsets.UTF_8));

        PersistenciaException ex = assertThrows(PersistenciaException.class, () -> dao.cargarTodos());
        assertTrue(ex.getMessage().contains(nombreArchivo));
    }

    @Test
    @DisplayName("Las operaciones que leen también propagan el error de un archivo corrupto")
    void operacionesPropagaCorrupcion() throws IOException {
        Files.write(Paths.get(dao.getRutaArchivo()), new byte[] {1, 2, 3, 4, 5});

        assertThrows(PersistenciaException.class, () -> dao.agregar(new Dato(1, "a")));
        assertThrows(PersistenciaException.class, () -> dao.buscarPrimero(d -> true));
        assertThrows(PersistenciaException.class, () -> dao.eliminarSi(d -> true));
    }

    // ------------------------------------------------------------------
    // Concurrencia (regresión del Error #3)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CONCURRENCIA: instancias distintas agregando al mismo archivo no pierden datos")
    void agregarConcurrenteDesdeInstanciasDistintas() throws Exception {
        final int totalTareas = 60;
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<Future<Void>>();

        for (int i = 0; i < totalTareas; i++) {
            final int id = i;
            futuros.add(pool.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    salida.await(); // todos arrancan a la vez
                    // Instancia NUEVA en cada tarea: es justo el escenario del Error #3
                    new DatoDAO(nombreArchivo).agregar(new Dato(id, "v" + id));
                    return null;
                }
            }));
        }

        salida.countDown();
        for (Future<Void> futuro : futuros) {
            futuro.get(30, TimeUnit.SECONDS); // propaga cualquier excepción de los hilos
        }
        pool.shutdown();

        List<Dato> persistidos = dao.cargarTodos();
        Set<Integer> ids = new HashSet<Integer>();
        for (Dato d : persistidos) {
            ids.add(d.getId());
        }
        assertEquals(totalTareas, persistidos.size(), "No debe perderse ninguna escritura");
        assertEquals(totalTareas, ids.size(), "No debe haber duplicados");
    }

    @Test
    @DisplayName("CONCURRENCIA: leer mientras otros escriben nunca ve un archivo a medias")
    void lecturaConcurrenteConEscrituras() throws Exception {
        dao.guardarTodos(Arrays.asList(new Dato(0, "inicial")));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<Future<Void>>();

        for (int i = 1; i <= 20; i++) {
            final int id = i;
            futuros.add(pool.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    salida.await();
                    new DatoDAO(nombreArchivo).agregar(new Dato(id, "w" + id));
                    return null;
                }
            }));
            futuros.add(pool.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    salida.await();
                    // Si la lectura viera un archivo a medias, lanzaría PersistenciaException
                    assertFalse(new DatoDAO(nombreArchivo).cargarTodos().isEmpty());
                    return null;
                }
            }));
        }

        salida.countDown();
        for (Future<Void> futuro : futuros) {
            futuro.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(21, dao.cargarTodos().size());
    }

    @Test
    @DisplayName("CONCURRENCIA: reemplazar y eliminar concurrentes dejan el archivo consistente")
    void reemplazarYEliminarConcurrentes() throws Exception {
        List<Dato> inicial = new ArrayList<Dato>();
        for (int i = 0; i < 20; i++) {
            inicial.add(new Dato(i, "v" + i));
        }
        dao.guardarTodos(inicial);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> futuros = new ArrayList<Future<Boolean>>();

        // Los ids pares se reemplazan; los impares se eliminan
        for (int i = 0; i < 20; i++) {
            final int id = i;
            futuros.add(pool.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    salida.await();
                    DatoDAO otro = new DatoDAO(nombreArchivo);
                    if (id % 2 == 0) {
                        return otro.reemplazarPrimero(d -> d.getId() == id, new Dato(id, "nuevo"));
                    }
                    return otro.eliminarSi(d -> d.getId() == id);
                }
            }));
        }

        salida.countDown();
        for (Future<Boolean> futuro : futuros) {
            assertTrue(futuro.get(30, TimeUnit.SECONDS), "Cada operación debía encontrar su entidad");
        }
        pool.shutdown();

        List<Dato> finales = dao.cargarTodos();
        assertEquals(10, finales.size());
        for (Dato d : finales) {
            assertEquals(0, d.getId() % 2, "Solo deben sobrevivir los ids pares");
            assertEquals("nuevo", d.getValor());
        }
    }
}
