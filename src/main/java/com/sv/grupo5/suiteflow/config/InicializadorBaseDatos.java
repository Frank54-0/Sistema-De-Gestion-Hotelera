package com.sv.grupo5.suiteflow.config;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta {@code schema.sql} y {@code data.sql} al arrancar, para que el proyecto
 * funcione recién clonado sin crear las tablas a mano.
 *
 * <p>Ambos scripts son idempotentes ({@code CREATE TABLE IF NOT EXISTS},
 * {@code INSERT IGNORE}): ejecutarlos en cada arranque no borra ni duplica datos.</p>
 */
public final class InicializadorBaseDatos {

    private InicializadorBaseDatos() {
        // Clase utilitaria: no se instancia.
    }

    /**
     * Crea las tablas y carga las habitaciones iniciales si aún no existen.
     *
     * @param dataSource pool de conexiones
     * @throws PersistenciaException si un script no existe o una sentencia falla
     */
    public static void inicializar(DataSource dataSource) {
        ejecutarScript(dataSource, "schema.sql");
        ejecutarScript(dataSource, "data.sql");
    }

    /**
     * Ejecuta un script SQL del classpath, sentencia por sentencia.
     *
     * @param dataSource pool de conexiones
     * @param recurso    nombre del script dentro de src/main/resources
     * @throws PersistenciaException si el script no existe o una sentencia falla
     */
    public static void ejecutarScript(DataSource dataSource, String recurso) {
        List<String> sentencias = leerSentencias(recurso);
        try (Connection conexion = dataSource.getConnection();
             Statement statement = conexion.createStatement()) {
            for (String sentencia : sentencias) {
                statement.executeUpdate(sentencia);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Error al ejecutar " + recurso + ": " + e.getMessage(), e);
        }
    }

    /**
     * Separa el script en sentencias por ';' e ignora los comentarios de línea
     * ({@code --}) y las sentencias CREATE DATABASE / USE, que solo aplican cuando
     * el script se ejecuta a mano en MySQL Workbench.
     */
    private static List<String> leerSentencias(String recurso) {
        String contenido;
        try (InputStream entrada = InicializadorBaseDatos.class.getClassLoader()
                .getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new PersistenciaException("No se encontró el script " + recurso);
            }
            contenido = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el script " + recurso, e);
        }

        StringBuilder sinComentarios = new StringBuilder();
        for (String linea : contenido.split("\\R")) {
            if (!linea.trim().startsWith("--")) {
                sinComentarios.append(linea).append('\n');
            }
        }

        List<String> sentencias = new ArrayList<String>();
        for (String sentencia : sinComentarios.toString().split(";")) {
            String limpia = sentencia.trim();
            String mayusculas = limpia.toUpperCase();
            if (limpia.isEmpty() || mayusculas.startsWith("CREATE DATABASE")
                    || mayusculas.startsWith("USE ")) {
                continue;
            }
            sentencias.add(limpia);
        }
        return sentencias;
    }
}