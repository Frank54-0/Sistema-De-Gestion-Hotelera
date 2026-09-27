package com.hotel.vista;

import com.hotel.controlador.ReservaController;
import com.hotel.dto.NuevaReservaDTO;
import com.hotel.dto.ReservaResumenDTO;
import com.hotel.excepciones.HabitacionNoDisponibleException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

// Vista de consola para el módulo de reservas.
// Es la única clase que lee del teclado (Scanner) e imprime en pantalla
// para este módulo. Nunca llama directamente a GestorReservas ni a los DAO:
// todo pasa por el ReservaController.
public class VistaReservas {

    private final ReservaController controller;
    private final Scanner scanner;
    private final SimpleDateFormat formatoFecha;

    public VistaReservas(ReservaController controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
        this.formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        this.formatoFecha.setLenient(false);
    }

    // Bucle principal del menú de reservas.
    public void iniciar() {
        boolean continuar = true;
        while (continuar) {
            mostrarMenu();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    verHabitacionesDisponibles();
                    break;
                case "2":
                    crearReserva();
                    break;
                case "3":
                    cancelarReserva();
                    break;
                case "4":
                    verReservasActivas();
                    break;
                case "5":
                    continuar = false;
                    break;
                default:
                    System.out.println("Opción no válida.\n");
            }
        }
    }

    private void mostrarMenu() {
        System.out.println("=== Menú de Reservas ===");
        System.out.println("1. Ver habitaciones disponibles");
        System.out.println("2. Crear una reserva");
        System.out.println("3. Cancelar una reserva");
        System.out.println("4. Ver reservas activas");
        System.out.println("5. Salir");
        System.out.print("Elige una opción: ");
    }

    private void verHabitacionesDisponibles() {
        List<Integer> numeros = controller.listarNumerosHabitacionesDisponibles();
        if (numeros.isEmpty()) {
            System.out.println("No hay habitaciones disponibles en este momento.\n");
            return;
        }
        System.out.println("Habitaciones disponibles: " + numeros + "\n");
    }

    private void crearReserva() {
        try {
            System.out.print("Nombre del cliente: ");
            String nombre = scanner.nextLine().trim();

            System.out.print("Teléfono del cliente: ");
            String telefono = scanner.nextLine().trim();

            System.out.print("Email del cliente: ");
            String email = scanner.nextLine().trim();

            System.out.print("Número de habitación: ");
            int numeroHabitacion = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Fecha de entrada (dd/MM/yyyy): ");
            Date fechaEntrada = formatoFecha.parse(scanner.nextLine().trim());

            System.out.print("Fecha de salida (dd/MM/yyyy): ");
            Date fechaSalida = formatoFecha.parse(scanner.nextLine().trim());

            NuevaReservaDTO datos = new NuevaReservaDTO(
                    nombre, telefono, email, numeroHabitacion, fechaEntrada, fechaSalida);

            ReservaResumenDTO resumen = controller.crearReserva(datos);
            mostrarResumen(resumen);

        } catch (NumberFormatException e) {
            System.out.println("El número de habitación debe ser un número entero.\n");
        } catch (ParseException e) {
            System.out.println("La fecha debe tener el formato dd/MM/yyyy.\n");
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo crear la reserva: " + e.getMessage() + "\n");
        } catch (HabitacionNoDisponibleException e) {
            System.out.println("Habitación no disponible: " + e.getMessage() + "\n");
        }
    }

    private void cancelarReserva() {
        System.out.print("ID de la reserva a cancelar: ");
        String idReserva = scanner.nextLine().trim();

        boolean cancelada = controller.cancelarReserva(idReserva);
        if (cancelada) {
            System.out.println("Reserva cancelada correctamente.\n");
        } else {
            System.out.println("No se encontró una reserva activa con ese ID.\n");
        }
    }

    private void verReservasActivas() {
        List<ReservaResumenDTO> reservas = controller.listarReservasActivas();
        if (reservas.isEmpty()) {
            System.out.println("No hay reservas activas.\n");
            return;
        }
        for (ReservaResumenDTO r : reservas) {
            mostrarResumen(r);
        }
    }

    private void mostrarResumen(ReservaResumenDTO r) {
        System.out.println("--- Reserva " + r.getIdReserva() + " ---");
        System.out.println("Cliente: " + r.getNombreCliente());
        System.out.println("Habitación: " + r.getNumeroHabitacion() + " (" + r.getTipoHabitacion() + ")");
        System.out.println("Entrada: " + formatoFecha.format(r.getFechaEntrada()));
        System.out.println("Salida: " + formatoFecha.format(r.getFechaSalida()));
        System.out.println("Total: $" + String.format("%.2f", r.getTotal()));
        System.out.println("Estado: " + r.getEstado());
        System.out.println();
    }
}