package com.sv.grupo5.suiteflow.view;

import com.sv.grupo5.suiteflow.controller.ReservaController;
import com.sv.grupo5.suiteflow.dao.DAOEnMemoria;
import com.sv.grupo5.suiteflow.model.HabitacionDoble;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.service.GestorHabitaciones;
import com.sv.grupo5.suiteflow.service.GestorReservas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la vista de consola. Se alimenta el Scanner con un texto que
 * simula lo que teclearía el usuario y se captura System.out para revisar lo
 * que se imprime. Toda entrada debe terminar con la opción 5 (salir); si no, el
 * Scanner se queda sin líneas.
 */
class VistaReservasTest {

    private ReservaController controller;

    @BeforeEach
    void prepararEscenario() {
        GestorHabitaciones habitaciones = new GestorHabitaciones(new DAOEnMemoria.HabitacionEnMemoria());
        habitaciones.agregarHabitacion(new HabitacionSencilla(101, 100.0));
        habitaciones.agregarHabitacion(new HabitacionDoble(102, 150.0));
        GestorReservas reservas = new GestorReservas(new DAOEnMemoria.ReservaEnMemoria(), habitaciones);
        controller = new ReservaController(reservas, habitaciones);
    }

    /** Ejecuta la vista con las líneas dadas y devuelve todo lo que imprimió. */
    private String ejecutar(String... lineas) throws UnsupportedEncodingException {
        PrintStream original = System.out;
        ByteArrayOutputStream captura = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captura, true, "UTF-8"));
            Scanner scanner = new Scanner(String.join("\n", lineas) + "\n");
            new VistaReservas(controller, scanner).iniciar();
        } finally {
            System.setOut(original);
        }
        return captura.toString("UTF-8");
    }

    @Test
    @DisplayName("La opción 5 sale del menú")
    void salir() throws Exception {
        String salida = ejecutar("5");

        assertTrue(salida.contains("Reservas"));
    }

    @Test
    @DisplayName("Una opción desconocida muestra un mensaje y vuelve al menú")
    void opcionInvalida() throws Exception {
        String salida = ejecutar("9", "5");

        assertTrue(salida.contains("no v"), "Debe avisar que la opción no es válida");
    }

    @Test
    @DisplayName("Opción 1: lista los números de las habitaciones disponibles")
    void verDisponibles() throws Exception {
        String salida = ejecutar("1", "5");

        assertTrue(salida.contains("[101, 102]"));
    }

    @Test
    @DisplayName("Opción 4 sin reservas: avisa que no hay reservas activas")
    void sinReservasActivas() throws Exception {
        String salida = ejecutar("4", "5");

        assertTrue(salida.contains("No hay reservas activas"));
    }

    @Test
    @DisplayName("Crear una reserva muestra el resumen y luego aparece en las activas")
    void crearReservaYVerla() throws Exception {
        String salida = ejecutar("2", "Ana Lopez", "7777-1234", "ana@example.com",
                "101", "20/10/2026", "22/10/2026", "4", "5");

        assertTrue(salida.contains("Ana Lopez"));
        assertTrue(salida.contains("101 (HabitacionSencilla)"));
        assertTrue(salida.contains("ACTIVA"));
    }

    @Test
    @DisplayName("Un número de habitación no numérico se rechaza con un mensaje")
    void numeroNoNumerico() throws Exception {
        String salida = ejecutar("2", "Ana Lopez", "7777-1234", "ana@example.com", "abc", "5");

        assertTrue(salida.contains("entero"));
    }

    @Test
    @DisplayName("Una fecha con formato incorrecto se rechaza con un mensaje")
    void fechaConFormatoIncorrecto() throws Exception {
        String salida = ejecutar("2", "Ana Lopez", "7777-1234", "ana@example.com",
                "101", "2026-10-20", "5");

        assertTrue(salida.contains("La fecha debe tener el formato"));
    }

    @Test
    @DisplayName("Una fecha inexistente (31/02) se rechaza porque el formato no es permisivo")
    void fechaInexistente() throws Exception {
        String salida = ejecutar("2", "Ana Lopez", "7777-1234", "ana@example.com",
                "101", "31/02/2026", "5");

        assertTrue(salida.contains("La fecha debe tener el formato"));
    }

    @Test
    @DisplayName("Reservar una habitación inexistente muestra el motivo")
    void habitacionInexistente() throws Exception {
        String salida = ejecutar("2", "Ana Lopez", "7777-1234", "ana@example.com",
                "999", "20/10/2026", "22/10/2026", "5");

        assertTrue(salida.contains("No se pudo crear la reserva"));
        assertTrue(salida.contains("999"));
    }

    @Test
    @DisplayName("Reservar dos veces la misma habitación muestra que no está disponible")
    void habitacionOcupada() throws Exception {
        String salida = ejecutar(
                "2", "Ana Lopez", "7777-1234", "ana@example.com", "101", "20/10/2026", "22/10/2026",
                "2", "Luis Mena", "7777-5678", "luis@example.com", "101", "20/10/2026", "22/10/2026",
                "5");

        assertTrue(salida.contains("no disponible: "));
    }

    @Test
    @DisplayName("Un nombre vacío se rechaza con el mensaje del modelo")
    void nombreVacio() throws Exception {
        String salida = ejecutar("2", "", "7777-1234", "a@x.com", "101", "20/10/2026", "22/10/2026", "5");

        assertTrue(salida.contains("No se pudo crear la reserva"));
    }

    @Test
    @DisplayName("Cancelar un id inexistente avisa que no se encontró")
    void cancelarInexistente() throws Exception {
        String salida = ejecutar("3", "no-existe", "5");

        assertTrue(salida.contains("No se encontr"));
    }
}
