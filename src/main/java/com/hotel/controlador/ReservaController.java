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

public class ReservaController {

    private final GestorReservas gestorReservas;
    private final GestorHabitaciones gestorHabitaciones;

    public ReservaController(GestorReservas gestorReservas, GestorHabitaciones gestorHabitaciones) {
        if (gestorReservas == null || gestorHabitaciones == null) {
            throw new IllegalArgumentException("Los gestores no pueden ser nuell");
        }
        this.gestorReservas = gestorReservas;
        this.gestorHabitaciones = gestorHabitaciones;
    }



    public ReservaResumenDTO crearReserva(NuevaReservaDTO datos) throws HabitacionNoDisponibleException {
        Habitacion habitacion = gestorHabitaciones.obtenerHabitacion(datos.getNumeroHabitacion());
        if (habitacion == null) {
            throw new IllegalArgumentException(
                    "No existe ninguna habitacion con el numero " + datos.getNumeroHabitacion());

        }

        Cliente cliente = new Cliente(
                datos.getNombreCliente(), datos.getTelefonoCliente(), datos.getEmailCliente()
        );
        Reserva reserva = gestorReservas.crearReserva(
                cliente, habitacion, datos.getFechaEntrada(), datos.getFechaSalida()
        );

        return convertirAResumen(reserva);
    }


    public boolean cancelarReserva(String idReserva) {
        return gestorReservas.cancelarReserva(idReserva);
    }

    // Lista los números de las habitaciones que se pueden reservar ahora mismo.
    public List<Integer> listarNumerosHabitacionesDisponibles() {
        List<Integer> numeros = new ArrayList<>();
        for (Habitacion h : gestorHabitaciones.obtenerHabitacionesDisponibles()) {
            numeros.add(h.getNumero());
        }
        return numeros;
    }

    public List<ReservaResumenDTO> listarReservasActivas() {
        List<ReservaResumenDTO> resumenes = new ArrayList<ReservaResumenDTO>();
        for (Reserva reserva : gestorReservas.obtenerReservasActivas()){
            resumenes.add(convertirAResumen(reserva));
        }
        return resumenes;
    }

    private ReservaResumenDTO convertirAResumen(Reserva reserva) {
        Habitacion habitacion = reserva.getHabitacion();
        return new ReservaResumenDTO(
                reserva.getId(),
                reserva.getCliente().getNombre(),
                habitacion.getNumero(),
                habitacion.getClass().getSimpleName(),
                reserva.getFechaEntrada(),
                reserva.getFechaSalida(),
                reserva.calcularTotal(),
                reserva.getEstado());
    }

}
