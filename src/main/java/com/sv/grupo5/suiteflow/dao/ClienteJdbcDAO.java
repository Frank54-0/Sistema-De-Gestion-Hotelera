package com.sv.grupo5.suiteflow.dao;
 
import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Cliente;
 
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
 
/**
 * Implementación de {@link ClienteDAO} con JDBC sobre MySQL/MariaDB.
 *
 * <p>Los clientes viven en la tabla {@code cliente}. El id es el UUID que genera
 * la clase {@link Cliente}; al leer se reconstruye con el constructor que
 * conserva ese id.</p>
 *
 * <p>Cada método pide su propia conexión al pool y la devuelve al terminar
 * (try-with-resources). Cualquier {@link SQLException} se traduce a
 * {@link PersistenciaException}: los detalles de JDBC no salen de la capa DAO.</p>
 */
public class ClienteJdbcDAO implements ClienteDAO {
 
    private static final String SQL_INSERTAR =
            "INSERT INTO cliente (id, nombre, telefono, email) VALUES (?, ?, ?, ?)";
    private static final String SQL_ACTUALIZAR =
            "UPDATE cliente SET nombre = ?, telefono = ?, email = ? WHERE id = ?";
    private static final String SQL_BUSCAR =
            "SELECT id, nombre, telefono, email FROM cliente WHERE id = ?";
    private static final String SQL_LISTAR =
            "SELECT id, nombre, telefono, email FROM cliente ORDER BY nombre, id";
    private static final String SQL_ELIMINAR =
            "DELETE FROM cliente WHERE id = ?";
 
    private final DataSource dataSource;
 
    /**
     * @param dataSource pool de las conexiones del que se toman las conexiones
     * @throws IllegalArgumentException si el pool es null
     */
    public ClienteJdbcDAO(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("El DataSource no puede ser null");
        }
        this.dataSource = dataSource;
    }
 
    @Override
    public void guardar(Cliente cliente) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR)) {
            ps.setString(1, cliente.getId());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            // Llave primaria repetida: el id ya está en la tabla.
            throw new PersistenciaException(
                    "Ya existe un cliente con el id " + cliente.getId(), e);
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo guardar el cliente " + cliente.getId(), e);
        }
    }
 
    @Override
    public void actualizar(Cliente cliente) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getTelefono());
            ps.setString(3, cliente.getEmail());
            ps.setString(4, cliente.getId());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException(
                        "No existe el cliente a actualizar: " + cliente.getId());
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo actualizar el cliente " + cliente.getId(), e);
        }
    }
 
    @Override
    public Optional<Cliente> buscarPorId(String id) {
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
            throw new PersistenciaException("No se pudo buscar el cliente " + id, e);
        }
    }
 
    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> clientes = new ArrayList<Cliente>();
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(construir(rs));
            }
            return clientes;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo listar los clientes", e);
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
        } catch (SQLIntegrityConstraintViolationException e) {
            // Llave foránea: el cliente todavía tiene reservas.
            throw new PersistenciaException(
                    "No se puede eliminar el cliente " + id + " porque tiene reservas", e);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar el cliente " + id, e);
        }
    }
 
    // Convierte la fila  del ResultSet en un Cliente (conserva el id de la base).
    private static Cliente construir(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getString("id"),
                rs.getString("nombre"),
                rs.getString("telefono"),
                rs.getString("email"));
    }
}