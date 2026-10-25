package com.sv.grupo5.suiteflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de validación y encapsulamiento de Cliente. */
class ClienteTest {

    @Test
    @DisplayName("Un cliente válido conserva sus datos y recibe un id")
    void clienteValido() {
        Cliente cliente = new Cliente("Juan Pérez", "77771234", "juan@example.com");

        assertNotNull(cliente.getId());
        assertEquals("Juan Pérez", cliente.getNombre());
        assertEquals("77771234", cliente.getTelefono());
        assertEquals("juan@example.com", cliente.getEmail());
    }

    @Test
    @DisplayName("Dos clientes distintos reciben ids distintos")
    void idsUnicos() {
        Cliente a = new Cliente("Ana", "77770001", "a@x.com");
        Cliente b = new Cliente("Ana", "77770001", "a@x.com");

        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    @DisplayName("El nombre no puede ser null, vacío ni solo espacios")
    void rechazaNombreInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Cliente(null, "77771234", "a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> new Cliente("", "77771234", "a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> new Cliente("   ", "77771234", "a@x.com"));
    }

    @Test
    @DisplayName("El teléfono no puede ser null, vacío ni solo espacios")
    void rechazaTelefonoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Cliente("Ana", null, "a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> new Cliente("Ana", "", "a@x.com"));
        assertThrows(IllegalArgumentException.class, () -> new Cliente("Ana", "   ", "a@x.com"));
    }

    @Test
    @DisplayName("Los setters actualizan los datos de contacto")
    void settersActualizan() {
        Cliente cliente = new Cliente("Ana", "77770001", "a@x.com");

        cliente.setNombre("Ana María");
        cliente.setTelefono("77770002");
        cliente.setEmail("nuevo@x.com");

        assertEquals("Ana María", cliente.getNombre());
        assertEquals("77770002", cliente.getTelefono());
        assertEquals("nuevo@x.com", cliente.getEmail());
    }
}
