package com.hotel.dao;

import com.hotel.modelo.Cliente;

import java.util.List;

/**
 * Acceso a datos de la entidad Cliente. Persiste en datos/clientes.dat.
 * Toda la mecánica de serialización vive en ArchivoDAO; aquí solo quedan
 * las operaciones propias del negocio de clientes.
 */
public class ClienteDAO extends ArchivoDAO<Cliente> {

    private static final String ARCHIVO = "clientes.dat";

    /** Construye el DAO apuntando a datos/clientes.dat. */
    public ClienteDAO() {
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

    /**
     * Agrega un cliente a los datos persistidos.
     *
     * @param cliente cliente a agregar
     */
    public void guardarCliente(Cliente cliente) {
        agregar(cliente);
    }

    /**
     * Busca un cliente por su identificador único.
     *
     * @param id identificador del cliente
     * @return el cliente si existe, null en caso contrario
     */
    public Cliente buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        for (Cliente cliente : cargarTodos()) {
            if (id.equals(cliente.getId())) {
                return cliente;
            }
        }
        return null;
    }
}
