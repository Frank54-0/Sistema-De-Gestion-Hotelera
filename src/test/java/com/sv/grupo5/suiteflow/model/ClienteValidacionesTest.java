package com.sv.grupo5.suiteflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de negocio de Cliente (regex de nombre, teléfono y email).
 * Cada test recorre varios valores: si uno falla, el mensaje indica cuál.
 */
class ClienteValidacionesTest {

    private static final String NOMBRE_OK = "Juan Perez";
    private static final String TEL_OK = "75375027";
    private static final String EMAIL_OK = "juanito@example.com";

    // ---- Nombre --------------------------------------------------------------

    @Test
    @DisplayName("Nombres válidos: letras, tildes, ñ y espacios (3 a 50 caracteres)")
    void nombresValidos() {
        String[] validos = {"Juan Perez", "María José", "Ñandú Peña", "Ana", "a".repeat(50)};
        for (String nombre : validos) {
            Cliente c = assertDoesNotThrow(() -> new Cliente(nombre, TEL_OK, EMAIL_OK),
                    "Debió aceptar el nombre: " + nombre);
            assertEquals(nombre, c.getNombre());
        }
    }

    @Test
    @DisplayName("Nombres inválidos: muy corto, muy largo, con números o símbolos")
    void nombresInvalidos() {
        String[] invalidos = {"A", "Al", "Juan123", "Nombre!", "Juan_Perez", "a".repeat(51)};
        for (String nombre : invalidos) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Cliente(nombre, TEL_OK, EMAIL_OK),
                    "Debió rechazar el nombre: " + nombre);
        }
    }

    // ---- Teléfono ------------------------------------------------------------

    @Test
    @DisplayName("Teléfonos válidos: exactamente 8 dígitos")
    void telefonosValidos() {
        String[] validos = {"75375027", "12345678", "77771234"};
        for (String tel : validos) {
            Cliente c = assertDoesNotThrow(() -> new Cliente(NOMBRE_OK, tel, EMAIL_OK),
                    "Debió aceptar el teléfono: " + tel);
            assertEquals(tel, c.getTelefono());
        }
    }

    @Test
    @DisplayName("Teléfonos inválidos: guiones, espacios, texto o longitud distinta de 8")
    void telefonosInvalidos() {
        String[] invalidos = {"123-4567", "7777-1234", "123456789", "1234567", "1234 5678", "texto", ""};
        for (String tel : invalidos) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Cliente(NOMBRE_OK, tel, EMAIL_OK),
                    "Debió rechazar el teléfono: '" + tel + "'");
        }
    }

    // ---- Email ---------------------------------------------------------------

    @Test
    @DisplayName("Emails válidos: formato estándar usuario@dominio.ext")
    void emailsValidos() {
        String[] validos = {"juanito@example.com", "test@test.net", "a.b+c@sub.dominio.org"};
        for (String email : validos) {
            Cliente c = assertDoesNotThrow(() -> new Cliente(NOMBRE_OK, TEL_OK, email),
                    "Debió aceptar el email: " + email);
            assertEquals(email, c.getEmail());
        }
    }

    @Test
    @DisplayName("Emails inválidos: sin arroba, sin dominio, dominio vacío o extensión de 1 letra")
    void emailsInvalidos() {
        String[] invalidos = {"sin-correo", "juanito@", "test@.com", "@test.com", "a@b.c"};
        for (String email : invalidos) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Cliente(NOMBRE_OK, TEL_OK, email),
                    "Debió rechazar el email: " + email);
        }
    }

    // ---- Setters aplican las mismas reglas ------------------------------------

    @Test
    @DisplayName("Los setters también rechazan datos inválidos")
    void settersValidan() {
        Cliente cliente = new Cliente(NOMBRE_OK, TEL_OK, EMAIL_OK);

        assertThrows(IllegalArgumentException.class, () -> cliente.setNombre("Juan123"));
        assertThrows(IllegalArgumentException.class, () -> cliente.setTelefono("123-4567"));
        assertThrows(IllegalArgumentException.class, () -> cliente.setEmail("juanito@"));
    }
}
