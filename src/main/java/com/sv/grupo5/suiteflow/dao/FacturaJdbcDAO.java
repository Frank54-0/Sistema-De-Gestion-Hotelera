package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Factura;
import com.sv.grupo5.suiteflow.model.Reserva;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link FacturaDAO} con JDBC sobre MySQL/MariaDB.
 *
 * <p>La factura va en la tabla {@code factura} y apunta a su reserva con
 * {@code reserva_id} (que es UNIQUE: una reserva no se factura dos veces). La
 * reserva debe estar guardada antes que la factura. Al leer se hace un JOIN con
 * {@code reserva} y {@code cliente} para reconstruir la factura completa; la
 * habitación de la reserva queda sin enlazar, igual que en {@link ReservaJdbcDAO}.</p>
 *
 * <p>Cada método pide su propia conexión al pool y la devuelve al terminar
 * (try-with-resources). Cualquier {@link SQLException} se traduce a
 * {@link PersistenciaException}: los detalles de JDBC no salen de la capa DAO.</p>
 */
public class FacturaJdbcDAO implements FacturaDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO factura (id, reserva_id, total, forma_pago, estado) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_ACTUALIZAR =
            "UPDATE factura SET total = ?, forma_pago = ?, estado = ? WHERE id = ?";
    private static final String SQL_SELECCIONAR =
            "SELECT f.id, f.total, f.forma_pago, f.estado, "
                    + "r.id AS reserva_id, r.numero_habitacion, r.fecha_entrada, r.fecha_salida, "
                    + "r.estado AS estado_reserva, "
                    + "c.id AS cliente_id, c.nombre, c.telefono, c.email "
                    + "FROM factura f "
                    + "JOIN reserva r ON r.id = f.reserva_id "
                    + "JOIN cliente c ON c.id = r.cliente_id";
    private static final String SQL_BUSCAR = SQL_SELECCIONAR + " WHERE f.id = ?";
    private static final String SQL_LISTAR = SQL_SELECCIONAR + " ORDER BY f.id";
    private static final String SQL_ELIMINAR = "DELETE FROM factura WHERE id = ?";

    private final DataSource dataSource;

    /**
     * @param dataSource pool de conexiones del que se toman las conexiones
     * @throws IllegalArgumentException si el pool es null
     */
    public FacturaJdbcDAO(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("El DataSource no puede ser null");
        }
        this.dataSource = dataSource;
    }

    @Override
    public void guardar(Factura factura) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR)) {
            ps.setString(1, factura.getId());
            ps.setString(2, factura.getReserva().getId());
            ps.setDouble(3, factura.getTotal());
            ps.setString(4, factura.getFormaPago());
            ps.setString(5, factura.getEstado());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            // Llave única repetida (la reserva ya tiene factura) o llave foránea
            // (la reserva todavía no está guardada).
            throw new PersistenciaException(
                    "No se pudo guardar la factura " + factura.getId() + ": la reserva "
                            + factura.getReserva().getId()
                            + " ya tiene factura o todavía no está guardada", e);
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo guardar la factura " + factura.getId(), e);
        }
    }

    @Override
    public void actualizar(Factura factura) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setDouble(1, factura.getTotal());
            ps.setString(2, factura.getFormaPago());
            ps.setString(3, factura.getEstado());
            ps.setString(4, factura.getId());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException(
                        "No existe la factura a actualizar: " + factura.getId());
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo actualizar la factura " + factura.getId(), e);
        }
    }

    @Override
    public Optional<Factura> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_BUSCAR)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(construir(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo buscar la factura " + id, e);
        }
    }

    @Override
    public List<Factura> listarTodos() {
        List<Factura> facturas = new ArrayList<Factura>();
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                facturas.add(construir(rs));
            }
            return facturas;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo listar las facturas", e);
        }
    }

    @Override
    public boolean eliminar(String id) {
        if (id == null) {
            return false;
        }
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_ELIMINAR)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar la factura " + id, e);
        }
    }

    // Convierte la fila (factura + reserva + cliente) en una Factura completa.
    private static Factura construir(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(
                rs.getString("cliente_id"),
                rs.getString("nombre"),
                rs.getString("telefono"),
                rs.getString("email"));
        Reserva reserva = new Reserva(
                rs.getString("reserva_id"),
                cliente,
                rs.getInt("numero_habitacion"),
                aUtil(rs.getDate("fecha_entrada")),
                aUtil(rs.getDate("fecha_salida")),
                rs.getString("estado_reserva"));
        return new Factura(
                rs.getString("id"),
                reserva,
                rs.getDouble("total"),
                rs.getString("forma_pago"),
                rs.getString("estado"));
    }

    private static Date aUtil(java.sql.Date fecha) {
        return new Date(fecha.getTime());
    }
}