package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionDoble;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.model.Suite;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link HabitacionDAO} con JDBC sobre MySQL/MariaDB.
 *
 * <p>La tabla {@code habitacion} guarda una sola fila por habitación, con la
 * columna {@code tipo} (SENCILLA, DOBLE o SUITE). Al leer, ese valor decide qué
 * subclase de {@link Habitacion} se construye, de modo que el resto del sistema
 * sigue trabajando por polimorfismo.</p>
 *
 * <p>Cada método pide su propia conexión al pool y la devuelve al terminar
 * (try-with-resources). Cualquier {@link SQLException} se traduce a
 * {@link PersistenciaException}: los detalles de JDBC no salen de la capa DAO.</p>
 */
public class HabitacionJdbcDAO implements HabitacionDAO {

    private static final String TIPO_SENCILLA = "SENCILLA";
    private static final String TIPO_DOBLE = "DOBLE";
    private static final String TIPO_SUITE = "SUITE";

    private static final String SQL_INSERTAR =
            "INSERT INTO habitacion (numero, tipo, precio_base, estado) VALUES (?, ?, ?, ?)";
    private static final String SQL_ACTUALIZAR =
            "UPDATE habitacion SET tipo = ?, precio_base = ?, estado = ? WHERE numero = ?";
    private static final String SQL_BUSCAR =
            "SELECT numero, tipo, precio_base, estado FROM habitacion WHERE numero = ?";
    private static final String SQL_LISTAR =
            "SELECT numero, tipo, precio_base, estado FROM habitacion ORDER BY numero";
    private static final String SQL_ELIMINAR =
            "DELETE FROM habitacion WHERE numero = ?";

    private final DataSource dataSource;

    /**
     * @param dataSource pool de conexiones del que se toman las conexiones
     * @throws IllegalArgumentException si el pool es null
     */
    public HabitacionJdbcDAO(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("El DataSource no puede ser null");
        }
        this.dataSource = dataSource;
    }

    @Override
    public void guardar(Habitacion habitacion) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR)) {
            ps.setInt(1, habitacion.getNumero());
            ps.setString(2, tipoDe(habitacion));
            ps.setDouble(3, habitacion.getPrecioBase());
            ps.setString(4, habitacion.getEstado());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo guardar la habitación " + habitacion.getNumero(), e);
        }
    }

    @Override
    public void actualizar(Habitacion habitacion) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, tipoDe(habitacion));
            ps.setDouble(2, habitacion.getPrecioBase());
            ps.setString(3, habitacion.getEstado());
            ps.setInt(4, habitacion.getNumero());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException(
                        "No existe la habitación a actualizar: " + habitacion.getNumero());
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo actualizar la habitación " + habitacion.getNumero(), e);
        }
    }

    @Override
    public Optional<Habitacion> buscarPorId(Integer numero) {
        if (numero == null) {
            return Optional.empty();
        }
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_BUSCAR)) {
            ps.setInt(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(construir(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la habitación " + numero, e);
        }
    }

    @Override
    public List<Habitacion> listarTodos() {
        List<Habitacion> habitaciones = new ArrayList<Habitacion>();
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                habitaciones.add(construir(rs));
            }
            return habitaciones;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo listar las habitaciones", e);
        }
    }

    @Override
    public boolean eliminar(Integer numero) {
        if (numero == null) {
            return false;
        }
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_ELIMINAR)) {
            ps.setInt(1, numero);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar la habitación " + numero, e);
        }
    }

    // Traduce la subclase Java al valor de la columna "tipo".
    private static String tipoDe(Habitacion habitacion) {
        if (habitacion instanceof Suite) {
            return TIPO_SUITE;
        }
        if (habitacion instanceof HabitacionDoble) {
            return TIPO_DOBLE;
        }
        if (habitacion instanceof HabitacionSencilla) {
            return TIPO_SENCILLA;
        }
        throw new IllegalArgumentException(
                "Tipo de habitación desconocido: " + habitacion.getClass().getSimpleName());
    }

    // Convierte la fila actual del ResultSet en la subclase que corresponde a su tipo.
    private static Habitacion construir(ResultSet rs) throws SQLException {
        int numero = rs.getInt("numero");
        String tipo = rs.getString("tipo");
        double precioBase = rs.getDouble("precio_base");

        Habitacion habitacion;
        switch (tipo) {
            case TIPO_SENCILLA:
                habitacion = new HabitacionSencilla(numero, precioBase);
                break;
            case TIPO_DOBLE:
                habitacion = new HabitacionDoble(numero, precioBase);
                break;
            case TIPO_SUITE:
                habitacion = new Suite(numero, precioBase);
                break;
            default:
                throw new PersistenciaException(
                        "La habitación " + numero + " tiene un tipo desconocido en la base de datos: " + tipo);
        }
        habitacion.setEstado(rs.getString("estado"));
        return habitacion;
    }
}
