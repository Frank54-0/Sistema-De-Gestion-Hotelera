package com.hotel.controlador;

import com.hotel.dto.NuevaReservaDTO;
import com.hotel.dto.ReservaResumenDTO;
import com.hotel.excepciones.HabitacionNoDisponibleException;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.servicio.GestorHabitaciones;
import com.hotel.servicio.GestorReservas;

import java.util.ArrayList;
import java.util.List;

// Esta clase actúa como un puente entre lo que ve el usuario (la pantalla)
// y el "cerebro" del sistema (la lógica de los gestores).
public class ReservaController {

    // Estas son nuestras herramientas principales para trabajar con las reservas y las habitaciones.
    private final GestorReservas gestorReservas;
    private final GestorHabitaciones gestorHabitaciones;

    // Al arrancar este controlador, nos aseguramos de que nos entreguen las herramientas de trabajo.
    public ReservaController(GestorReservas gestorReservas, GestorHabitaciones gestorHabitaciones) {
        // Si por error no nos dan las herramientas, avisamos que algo salió mal.
        if (gestorReservas == null || gestorHabitaciones == null) {
            throw new IllegalArgumentException("Los gestores no pueden ser nuell");
        }
        this.gestorReservas = gestorReservas;
        this.gestorHabitaciones = gestorHabitaciones;
    }


    // Método principal para hacer una nueva reserva, recibiendo un paquete con los datos (NuevaReservaDTO).
    public ReservaResumenDTO crearReserva(NuevaReservaDTO datos) throws HabitacionNoDisponibleException {

        // 1. Primero, buscamos en el hotel si existe la habitación que nos están pidiendo.
        Habitacion habitacion = gestorHabitaciones.obtenerHabitacion(datos.getNumeroHabitacion());

        // Si no existe, lanzamos una alerta avisando del error.
        if (habitacion == null) {
            throw new IllegalArgumentException(
                    "No existe ninguna habitacion con el numero " + datos.getNumeroHabitacion());
        }

        // 2. Preparamos la ficha del cliente con los datos que nos enviaron.
        Cliente cliente = new Cliente(
                datos.getNombreCliente(), datos.getTelefonoCliente(), datos.getEmailCliente()
        );

        // 3. Le pedimos a nuestro gestor que haga la reserva oficial en el sistema.
        Reserva reserva = gestorReservas.crearReserva(
                cliente, habitacion, datos.getFechaEntrada(), datos.getFechaSalida()
        );

        // 4. Finalmente, devolvemos un resumen bonito de la reserva para mostrarlo en pantalla.
        return convertirAResumen(reserva);
    }


    // Método sencillo para cancelar una reserva usando su código (idReserva).
    // Devuelve 'true' si se canceló con éxito, o 'false' si hubo algún problema.
    public boolean cancelarReserva(String idReserva) {
        return gestorReservas.cancelarReserva(idReserva);
    }

    // Busca todas las habitaciones que están libres en este momento y nos devuelve
    // únicamente una lista con sus números (ej. [101, 102, 201]).
    public List<Integer> listarNumerosHabitacionesDisponibles() {
        List<Integer> numeros = new ArrayList<>();
        // Revisamos una por una las habitaciones libres y guardamos su número.
        for (Habitacion h : gestorHabitaciones.obtenerHabitacionesDisponibles()) {
            numeros.add(h.getNumero());
        }
        return numeros;
    }

    // Busca todas las reservas que están vigentes (no canceladas) y las prepara
    // en formato de "resumen" para que la pantalla pueda mostrarlas fácilmente.
    public List<ReservaResumenDTO> listarReservasActivas() {
        List<ReservaResumenDTO> resumenes = new ArrayList<ReservaResumenDTO>();
        for (Reserva reserva : gestorReservas.obtenerReservasActivas()){
            resumenes.add(convertirAResumen(reserva)); // Convierte cada reserva a su versión resumida
        }
        return resumenes;
    }

    // Es una función de ayuda (interna). Toma toda la información compleja de una reserva
    // y extrae solo lo más importante (como si armara un recibo simplificado).
    private ReservaResumenDTO convertirAResumen(Reserva reserva) {
        Habitacion habitacion = reserva.getHabitacion();
        return new ReservaResumenDTO(
                reserva.getId(),
                reserva.getCliente().getNombre(),
                habitacion.getNumero(),
                habitacion.getClass().getSimpleName(), // Saca el tipo de habitación (Ej: "HabitacionDoble")
                reserva.getFechaEntrada(),
                reserva.getFechaSalida(),
                reserva.calcularTotal(),
                reserva.getEstado());
    }

}