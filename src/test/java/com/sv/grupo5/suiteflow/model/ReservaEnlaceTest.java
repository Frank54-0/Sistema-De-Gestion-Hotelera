package com.sv.grupo5.suiteflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del vínculo Reserva-Habitación tras la corrección del hallazgo #1 de
 * QA: la habitación es transient y solo se persiste su número, de modo que una
 * reserva recién cargada de disco debe poder reenlazarse con el inventario.
 */
class ReservaEnlaceTest {

    private Reserva nuevaReserva() {
        Cliente cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 86_400_000L);
        return new Reserva(cliente, habitacion, entrada, salida);
    }

    /** Simula guardar en disco y volver a leer: devuelve una copia deserializada. */
    private Reserva viajeADisco(Reserva original) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(original);
        }
        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Reserva) entrada.readObject();
        }
    }

    @Test
    @DisplayName("No se puede crear una reserva sin habitación")
    void rechazaHabitacionNull() {
        Cliente cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 86_400_000L);

        assertThrows(IllegalArgumentException.class,
                () -> new Reserva(cliente, null, entrada, salida));
    }

    @Test
    @DisplayName("Al serializar se conserva el número de habitación pero no el objeto")
    void tras_serializar_se_pierde_el_objeto_habitacion() throws Exception {
        Reserva cargada = viajeADisco(nuevaReserva());

        assertEquals(101, cargada.getNumeroHabitacion());
        assertNull(cargada.getHabitacion());
    }

    @Test
    @DisplayName("Calcular el total sin reenlazar la habitación lanza IllegalStateException")
    void totalSinEnlazarLanzaExcepcion() throws Exception {
        Reserva cargada = viajeADisco(nuevaReserva());

        assertThrows(IllegalStateException.class, cargada::calcularTotal);
    }

    @Test
    @DisplayName("Tras reenlazar, la reserva vuelve a calcular el total con sus servicios")
    void reenlazarRestauraElCalculo() throws Exception {
        Reserva original = nuevaReserva();
        original.agregarServicio(new ServicioAdicional("Desayuno", 15.0));
        Reserva cargada = viajeADisco(original);

        cargada.setHabitacion(new HabitacionSencilla(101, 100.0));

        assertEquals(115.0, cargada.calcularTotal(), 0.001);
    }

    @Test
    @DisplayName("Solo se puede reenlazar con la habitación del mismo número")
    void reenlazarConOtroNumeroSeRechaza() throws Exception {
        Reserva cargada = viajeADisco(nuevaReserva());

        assertThrows(IllegalArgumentException.class,
                () -> cargada.setHabitacion(new HabitacionSencilla(999, 100.0)));
        assertNull(cargada.getHabitacion(), "El enlace no debe haber cambiado");
    }

    @Test
    @DisplayName("No se puede reenlazar con null")
    void reenlazarConNullSeRechaza() throws Exception {
        Reserva cargada = viajeADisco(nuevaReserva());

        assertThrows(IllegalArgumentException.class, () -> cargada.setHabitacion(null));
    }

    @Test
    @DisplayName("Los servicios y el estado sobreviven a la serialización")
    void serviciosYEstadoSobreviven() throws Exception {
        Reserva original = nuevaReserva();
        original.agregarServicio(new ServicioAdicional("Spa", 25.0));
        original.cancelar();

        Reserva cargada = viajeADisco(original);

        assertEquals(original.getId(), cargada.getId());
        assertEquals(1, cargada.getServicios().size());
        assertEquals("Spa", cargada.getServicios().get(0).getNombre());
        assertEquals(Reserva.CANCELADA, cargada.getEstado());
    }

    @Test
    @DisplayName("agregarServicio(null) se ignora")
    void servicioNullSeIgnora() {
        Reserva reserva = nuevaReserva();

        reserva.agregarServicio(null);

        assertTrue(reserva.getServicios().isEmpty());
    }

    @Test
    @DisplayName("toString no falla con una reserva sin enlazar y muestra el número de habitación")
    void toStringSinEnlazar() throws Exception {
        Reserva cargada = viajeADisco(nuevaReserva());

        String texto = cargada.toString();

        assertTrue(texto.contains("101"));
    }
}
