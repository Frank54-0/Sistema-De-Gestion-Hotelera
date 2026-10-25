package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.HabitacionNoDisponibleException;
import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.Reserva;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Guarda las reservas en MySQL/MariaDB con JDBC. La reserva va en la tabla
 * {@code reserva} y su cliente en {@code cliente}.
 *
 * <p>{@link #guardarSiHabitacionLibre(Reserva)} evita la doble reserva con una
 * transacción y {@code SELECT ... FOR UPDATE}: la base de datos hace esperar al
 * segundo que pide la misma habitación.</p>
 */
public class ReservaJdbcDAO implements ReservaDAO {

    private static final String SQL_INSERTAR_CLIENTE =
            "INSERT IGNORE INTO cliente (id, nombre, telefono, email) VALUES (?, ?, ?, ?)";
    private static final String SQL_INSERTAR_RESERVA =
            "INSERT INTO reserva (id, cliente_id, numero_habitacion, fecha_entrada, fecha_salida, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_ACTUALIZAR =
            "UPDATE reserva SET fecha_entrada = ?, fecha_salida = ?, estado = ? WHERE id = ?";
    private static final String SQL_SELECCIONAR =
            "SELECT r.id, r.numero_habitacion, r.fecha_entrada, r.fecha_salida, r.estado, "
                    + "c.id AS cliente_id, c.nombre, c.telefono, c.email "
                    + "FROM reserva r JOIN cliente c ON c.id = r.cliente_id";
    private static final String SQL_BUSCAR = SQL_SELECCIONAR + " WHERE r.id = ?";
    private static final String SQL_LISTAR = SQL_SELECCIONAR + " ORDER BY r.fecha_entrada, r.id";
    private static final String SQL_ELIMINAR = "DELETE FROM reserva WHERE id = ?";

    // Bloquea la fila de la habitación hasta que termine la transacción.
    private static final String SQL_BLOQUEAR_HABITACION =
            "SELECT estado FROM habitacion WHERE numero = ? FOR UPDATE";
    // Cuenta reservas activas de la habitación que se cruzan con las fechas pedidas.
    private static final String SQL_CONTAR_SOLAPES =
            "SELECT COUNT(*) FROM reserva WHERE numero_habitacion = ? AND estado = 'ACTIVA' "
                    + "AND fecha_entrada < ? AND fecha_salida > ?";
    private static final String SQL_OCUPAR_HABITACION =
            "UPDATE habitacion SET estado = 'OCUPADA' WHERE numero = ?";

    private final DataSource dataSource;

    /**
     * @param dataSource pool de conexiones
     * @throws IllegalArgumentException si el pool es null
     */
    public ReservaJdbcDAO(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("El DataSource no puede ser null");
        }
        this.dataSource = dataSource;
    }

    /**
     * Guarda la reserva y su cliente en una sola transacción.
     *
     * @param reserva reserva a guardar
     * @throws PersistenciaException si falla la base de datos
     */
    @Override
    public void guardar(Reserva reserva) {
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                insertarClienteYReserva(conexion, reserva);
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la reserva " + reserva.getId(), e);
        }
    }

    /**
     * Guarda la reserva solo si la habitación está libre. Bloquea la habitación,
     * revisa mantenimiento y fechas, guarda, la marca OCUPADA y confirma; si algo
     * falla, deshace todo.
     *
     * @param reserva reserva a guardar
     * @throws HabitacionNoDisponibleException si no existe, está en mantenimiento
     *         o ya está reservada en esas fechas
     * @throws PersistenciaException           si falla la base de datos
     */
    @Override
    public void guardarSiHabitacionLibre(Reserva reserva) throws HabitacionNoDisponibleException {
        int numero = reserva.getNumeroHabitacion();
        try (Connection conexion = dataSource.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                verificarHabitacionLibre(conexion, reserva);
                insertarClienteYReserva(conexion, reserva);
                ocuparHabitacion(conexion, numero);
                conexion.commit();
            } catch (HabitacionNoDisponibleException | SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo guardar la reserva de la habitación " + numero, e);
        }
    }

    @Override
    public void actualizar(Reserva reserva) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setDate(1, aSql(reserva.getFechaEntrada()));
            ps.setDate(2, aSql(reserva.getFechaSalida()));
            ps.setString(3, reserva.getEstado());
            ps.setString(4, reserva.getId());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException(
                        "No existe la reserva a actualizar: " + reserva.getId());
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No se pudo actualizar la reserva " + reserva.getId(), e);
        }
    }

    @Override
    public Optional<Reserva> buscarPorId(String id) {
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
            throw new PersistenciaException("No se pudo buscar la reserva " + id, e);
        }
    }

    @Override
    public List<Reserva> listarTodos() {
        List<Reserva> reservas = new ArrayList<Reserva>();
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement ps = conexion.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reservas.add(construir(rs));
            }
            return reservas;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo listar las reservas", e);
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
            throw new PersistenciaException("No se pudo eliminar la reserva " + id, e);
        }
    }

    // Bloquea la habitación y comprueba que se pueda reservar.
    private void verificarHabitacionLibre(Connection conexion, Reserva reserva)
            throws SQLException, HabitacionNoDisponibleException {
        int numero = reserva.getNumeroHabitacion();

        try (PreparedStatement ps = conexion.prepareStatement(SQL_BLOQUEAR_HABITACION)) {
            ps.setInt(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new HabitacionNoDisponibleException(
                            "La habitación " + numero + " no está registrada");
                }
                if (Habitacion.MANTENIMIENTO.equals(rs.getString("estado"))) {
                    throw new HabitacionNoDisponibleException(
                            "La habitación " + numero + " está en mantenimiento");
                }
            }
        }

        try (PreparedStatement ps = conexion.prepareStatement(SQL_CONTAR_SOLAPES)) {
            ps.setInt(1, numero);
            ps.setDate(2, aSql(reserva.getFechaSalida()));
            ps.setDate(3, aSql(reserva.getFechaEntrada()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new HabitacionNoDisponibleException(
                            "La habitación " + numero + " ya está reservada en esas fechas");
                }
            }
        }
    }

    // Guarda el cliente (si ya existe no se toca) y la reserva.
    private void insertarClienteYReserva(Connection conexion, Reserva reserva) throws SQLException {
        Cliente cliente = reserva.getCliente();
        try (PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR_CLIENTE)) {
            ps.setString(1, cliente.getId());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conexion.prepareStatement(SQL_INSERTAR_RESERVA)) {
            ps.setString(1, reserva.getId());
            ps.setString(2, cliente.getId());
            ps.setInt(3, reserva.getNumeroHabitacion());
            ps.setDate(4, aSql(reserva.getFechaEntrada()));
            ps.setDate(5, aSql(reserva.getFechaSalida()));
            ps.setString(6, reserva.getEstado());
            ps.executeUpdate();
        }
    }

    private void ocuparHabitacion(Connection conexion, int numero) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(SQL_OCUPAR_HABITACION)) {
            ps.setInt(1, numero);
            ps.executeUpdate();
        }
    }

    // Convierte la fila en una Reserva (la habitación queda null).
    private static Reserva construir(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(
                rs.getString("cliente_id"),
                rs.getString("nombre"),
                rs.getString("telefono"),
                rs.getString("email"));
        return new Reserva(
                rs.getString("id"),
                cliente,
                rs.getInt("numero_habitacion"),
                aUtil(rs.getDate("fecha_entrada")),
                aUtil(rs.getDate("fecha_salida")),
                rs.getString("estado"));
    }

    private static java.sql.Date aSql(Date fecha) {
        return new java.sql.Date(fecha.getTime());
    }

    private static Date aUtil(java.sql.Date fecha) {
        return new Date(fecha.getTime());
    }
}
