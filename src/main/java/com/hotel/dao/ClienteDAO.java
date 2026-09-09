package com.hotel.dao;

import com.hotel.modelo.Cliente;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de acceso a datos (DAO) para la entidad Cliente.
 * Se encarga de persistir y recuperar la lista de clientes usando
 * serialización de objetos en un archivo binario .dat.
 *
 * Capa de Acceso a Datos dentro de la arquitectura por capas del sistema:
 * Presentación → Servicio → DAO → Modelo.
 */
public class ClienteDAO {

    private static final String RUTA_ARCHIVO = "datos/clientes.dat";

    /**
     * Constructor de ClienteDAO.
     * Se asegura de que la carpeta de datos exista antes de operar.
     */
    public ClienteDAO() {
        File carpeta = new File("datos");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    /**
     * Guarda la lista completa de clientes en el archivo clientes.dat,
     * sobrescribiendo el contenido anterior.
     *
     * @param clientes lista de clientes a persistir
     */
    public synchronized void guardarClientes(List<Cliente> clientes) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RUTA_ARCHIVO))) {
            oos.writeObject(new ArrayList<>(clientes));
        } catch (IOException e) {
            System.err.println("Error al guardar clientes en " + RUTA_ARCHIVO + ": " + e.getMessage());
        }
    }

    /**
     * Carga la lista completa de clientes desde el archivo clientes.dat.
     * Si el archivo no existe todavía (primera ejecución), retorna una lista vacía.
     *
     * @return lista de clientes persistidos, o lista vacía si no hay datos previos
     */
    @SuppressWarnings("unchecked")
    public synchronized List<Cliente> cargarClientes() {
        File archivo = new File(RUTA_ARCHIVO);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
            return (List<Cliente>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al cargar clientes desde " + RUTA_ARCHIVO + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Agrega un único cliente y persiste la lista actualizada.
     * Método de conveniencia: carga, agrega, y vuelve a guardar.
     *
     * @param cliente cliente a agregar y persistir
     */
    public synchronized void guardarCliente(Cliente cliente) {
        List<Cliente> clientes = cargarClientes();
        clientes.add(cliente);
        guardarClientes(clientes);
    }

    /**
     * Busca un cliente por su id dentro de los datos persistidos.
     *
     * @param id identificador único del cliente
     * @return el cliente si existe, null en caso contrario
     */
    public synchronized Cliente buscarPorId(String id) {
        for (Cliente cliente : cargarClientes()) {
            if (cliente.getId().equals(id)) {
                return cliente;
            }
        }
        return null;
    }
}
