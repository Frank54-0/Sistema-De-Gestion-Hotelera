package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato CRUD y garantías de archivo aplicados a ClienteArchivoDAO. */
class ClienteArchivoDAOTest extends ArchivoDAOContrato<Cliente, String> {

    @Override
    protected CrudDAO<Cliente, String> crearDAO() {
        return new ClienteArchivoDAO();
    }

    @Override
    protected String nombreArchivo() {
        return "clientes.dat";
    }

    @Override
    protected Cliente nueva(int n) {
        return new Cliente("Cliente Prueba", String.valueOf(77771000 + n), "c" + n + "@ejemplo.com");
    }

    @Override
    protected String idDe(Cliente c) {
        return c.getId();
    }

    @Override
    protected Cliente modificar(Cliente original) {
        original.setNombre("Nombre Modificado");
        return original;
    }

    @Override
    protected boolean estaModificada(Cliente c) {
        return "Nombre Modificado".equals(c.getNombre());
    }

    @Test
    @DisplayName("Un cliente cargado conserva todos sus datos de contacto")
    void clienteConservaSusDatos() {
        Cliente c = nueva(1);
        dao.guardar(c);

        Cliente cargado = dao.buscarPorId(c.getId()).get();

        assertEquals("Cliente Prueba", cargado.getNombre());
        assertEquals("77771001", cargado.getTelefono());
        assertEquals("c1@ejemplo.com", cargado.getEmail());
    }
}
