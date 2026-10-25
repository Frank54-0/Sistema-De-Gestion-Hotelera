package com.sv.grupo5.suiteflow.config;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configura el pool de conexiones (DataSource) de SuiteFlow.
 *
 * <p><b>Por qué un pool.</b> Abrir una conexión física a MySQL cuesta decenas de
 * milisegundos (TCP, autenticación, sesión). Con un pool las conexiones se abren
 * una vez y se reutilizan: cada DAO pide una con {@code getConnection()} y al
 * cerrarla dentro de un try-with-resources la devuelve al pool en lugar de
 * cerrarla de verdad.</p>
 *
 * <p><b>Dimensionamiento.</b> Los valores se calcularon a partir de los hilos que
 * pueden usar la base de datos al mismo tiempo en este sistema:</p>
 * <ul>
 *   <li>{@value #HILOS_RESERVA} hilos del {@code ProcesadorSolicitudesReserva}
 *       (ExecutorService que procesa lotes de solicitudes);</li>
 *   <li>1 hilo de la consola de recepción (VistaReservas);</li>
 *   <li>1 hilo del {@code ProcesadorFacturas}.</li>
 * </ul>
 * <p>En el peor caso hay 10 conexiones en uso simultáneo; {@code maxActive = 15}
 * deja un margen del 50 % para no bloquear a nadie.</p>
 *
 * <p>Los datos de conexión (URL, usuario, contraseña) se leen de
 * {@code db.properties} y se pueden sobrescribir con variables de entorno
 * ({@code SUITEFLOW_DB_URL}, {@code SUITEFLOW_DB_USUARIO},
 * {@code SUITEFLOW_DB_CONTRASENA}) para no subir contraseñas reales a Git.</p>
 */
public final class DataSourceConfig {

    /** Hilos del ExecutorService de reservas; base del cálculo de maxActive. */
    public static final int HILOS_RESERVA = 8;

    /**
     * Consulta mínima para comprobar que una conexión sigue viva. {@code SELECT 1}
     * funciona en MySQL y en H2 (pruebas) y no toca ninguna tabla.
     */
    public static final String VALIDATION_QUERY = "SELECT 1";

    /**
     * Conexiones que se abren al arrancar. Con 5 la consola, el hilo de facturas y
     * el primer lote de reservas no esperan a que se abran conexiones nuevas.
     */
    public static final int INITIAL_SIZE = 5;

    /** Mínimo de conexiones inactivas que se conservan; igual a initialSize para no reabrir tras un periodo tranquilo. */
    public static final int MIN_IDLE = 5;

    /** Máximo de conexiones inactivas; por encima de 10 el pool cierra las sobrantes y libera recursos de MySQL. */
    public static final int MAX_IDLE = 10;

    /** Máximo de conexiones en uso: 10 en el peor caso (ver arriba) más un 50 % de margen. */
    public static final int MAX_ACTIVE = 15;

    /**
     * Milisegundos que un hilo espera por una conexión antes de recibir error.
     * 10 s: una transacción de reserva tarda milisegundos, así que esperar más
     * indicaría un problema real (pool agotado o BD caída) que conviene reportar.
     */
    public static final int MAX_WAIT_MS = 10_000;

    /** Validar al prestar: evita entregar a un DAO una conexión que MySQL ya cerró. */
    public static final boolean TEST_ON_BORROW = true;

    /** No validar al devolver: ya se valida al prestar; validar dos veces solo agrega consultas. */
    public static final boolean TEST_ON_RETURN = false;

    /** Validar las inactivas en segundo plano: detecta conexiones cortadas sin hacer esperar a nadie. */
    public static final boolean TEST_WHILE_IDLE = true;

    /** Validar al crear: si los datos de conexión son incorrectos, falla al arrancar y no en la primera reserva. */
    public static final boolean TEST_ON_CONNECT = true;

    /** Cada 30 s el pool revisa las conexiones inactivas (validación y expulsión). */
    public static final int TIME_BETWEEN_EVICTION_RUNS_MS = 30_000;

    /** Una conexión inactiva por más de 60 s puede cerrarse, siempre que queden al menos minIdle. */
    public static final int MIN_EVICTABLE_IDLE_TIME_MS = 60_000;

    /**
     * Recuperar conexiones que un DAO tomó y nunca devolvió (fuga). Con
     * try-with-resources no debería pasar; es una red de seguridad.
     */
    public static final boolean REMOVE_ABANDONED = true;

    /**
     * Segundos tras los cuales una conexión prestada se considera abandonada.
     * La operación más larga del sistema (reservar) dura milisegundos; 60 s
     * detecta fugas sin riesgo de quitarle la conexión a una operación legítima.
     */
    public static final int REMOVE_ABANDONED_TIMEOUT_S = 60;

    /**
     * Una misma conexión se valida como máximo cada 30 s. Sin este límite,
     * testOnBorrow ejecutaría SELECT 1 antes de cada operación.
     */
    public static final long VALIDATION_INTERVAL_MS = 30_000L;

    private static final String ARCHIVO_PROPIEDADES = "db.properties";

    private DataSourceConfig() {
        // Clase de configuración: no se instancia.
    }

    /**
     * Crea el pool con los datos de {@code db.properties}, sobrescritos por las
     * variables de entorno si existen.
     *
     * @return pool listo para usar; quien lo crea debe llamar a {@code close()} al terminar
     * @throws PersistenciaException si no se encuentra o no se puede leer db.properties
     */
    public static DataSource crearDesdeConfiguracion() {
        Properties propiedades = cargarPropiedades();
        sobrescribirConEntorno(propiedades, "db.url", "SUITEFLOW_DB_URL");
        sobrescribirConEntorno(propiedades, "db.usuario", "SUITEFLOW_DB_USUARIO");
        sobrescribirConEntorno(propiedades, "db.contrasena", "SUITEFLOW_DB_CONTRASENA");
        return crear(
                propiedades.getProperty("db.driver"),
                propiedades.getProperty("db.url"),
                propiedades.getProperty("db.usuario"),
                propiedades.getProperty("db.contrasena", ""));
    }

    /**
     * Crea el pool con los datos de conexión indicados y los parámetros
     * documentados en esta clase. Las pruebas lo usan con una base H2 en memoria.
     *
     * @param driver     clase del driver JDBC (por ejemplo {@code com.mysql.cj.jdbc.Driver})
     * @param url        URL JDBC de la base de datos
     * @param usuario    usuario de la base de datos
     * @param contrasena contraseña del usuario
     * @return pool configurado
     * @throws IllegalArgumentException si falta la URL o el usuario
     */
    public static DataSource crear(String driver, String url, String usuario, String contrasena) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Falta la URL de la base de datos (db.url)");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("Falta el usuario de la base de datos (db.usuario)");
        }

        PoolProperties p = new PoolProperties();
        p.setDriverClassName(driver);
        p.setUrl(url);
        p.setUsername(usuario);
        p.setPassword(contrasena);

        p.setValidationQuery(VALIDATION_QUERY);
        p.setInitialSize(INITIAL_SIZE);
        p.setMinIdle(MIN_IDLE);
        p.setMaxIdle(MAX_IDLE);
        p.setMaxActive(MAX_ACTIVE);
        p.setMaxWait(MAX_WAIT_MS);
        p.setTestOnBorrow(TEST_ON_BORROW);
        p.setTestOnReturn(TEST_ON_RETURN);
        p.setTestWhileIdle(TEST_WHILE_IDLE);
        p.setTestOnConnect(TEST_ON_CONNECT);
        p.setTimeBetweenEvictionRunsMillis(TIME_BETWEEN_EVICTION_RUNS_MS);
        p.setMinEvictableIdleTimeMillis(MIN_EVICTABLE_IDLE_TIME_MS);
        p.setRemoveAbandoned(REMOVE_ABANDONED);
        p.setRemoveAbandonedTimeout(REMOVE_ABANDONED_TIMEOUT_S);
        p.setValidationInterval(VALIDATION_INTERVAL_MS);
        // Si se detecta una conexión abandonada, registra dónde se pidió (ayuda a encontrar la fuga).
        p.setLogAbandoned(true);

        DataSource dataSource = new DataSource();
        dataSource.setPoolProperties(p);
        return dataSource;
    }

    private static Properties cargarPropiedades() {
        Properties propiedades = new Properties();
        try (InputStream entrada = DataSourceConfig.class.getClassLoader()
                .getResourceAsStream(ARCHIVO_PROPIEDADES)) {
            if (entrada == null) {
                throw new PersistenciaException(
                        "No se encontró " + ARCHIVO_PROPIEDADES + " en src/main/resources");
            }
            propiedades.load(entrada);
            return propiedades;
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer " + ARCHIVO_PROPIEDADES, e);
        }
    }

    private static void sobrescribirConEntorno(Properties propiedades, String clave, String variable) {
        String valor = System.getenv(variable);
        if (valor != null && !valor.isBlank()) {
            propiedades.setProperty(clave, valor);
        }
    }
}