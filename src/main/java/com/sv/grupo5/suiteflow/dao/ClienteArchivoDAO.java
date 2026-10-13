package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Cliente;

import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link ClienteDAO} basada en archivos, persistida en
 * {@code datos/clientes.dat}. Toda la mecánica de serialización vive en
 * ArchivoDAO; aquí solo quedan las operaciones propias de los clientes.
 */
public class ClienteArchivoDAO extends ArchivoDAO<Cliente> implements ClienteDAO {

    private static final String ARCHIVO = "clientes.dat";

    /** Construye el DAO apuntando a datos/clientes.dat. */
    public ClienteArchivoDAO() {
        super(ARCHIVO);
    }

    /**
     * Guarda la lista completa de clientes, sobrescribiendo la anterior.
     *
     * @param clientes lista de clientes a persistir
     */
    public void guardarClientes(List<Cliente> clientes) {
        guardarTodos(clientes);
    }

    /**
     * Carga todos los clientes persistidos.
     *
     * @return lista de clientes; vacía si aún no hay datos
     */
    public List<Cliente> cargarClientes() {
        return cargarTodos();
    }

    @Override
    public void guardar(Cliente cliente) {
        agregar(cliente);
    }

    @Override
    public void actualizar(Cliente cliente) {
        boolean existe = reemplazarPrimero(c -> c.getId().equals(cliente.getId()), cliente);
        if (!existe) {
            throw new PersistenciaException(
                    "No existe el cliente a actualizar: " + cliente.getId());
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return buscarPrimero(c -> id.equals(c.getId()));
    }

    @Override
    public List<Cliente> listarTodos() {
        return cargarTodos();
    }

    @Override
    public boolean eliminar(String id) {
        if (id == null) {
            return false;
        }
        return eliminarSi(c -> id.equals(c.getId()));
    }
}