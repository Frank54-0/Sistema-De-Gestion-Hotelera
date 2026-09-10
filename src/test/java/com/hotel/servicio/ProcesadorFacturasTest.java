package com.hotel.servicio;

import com.hotel.dao.FacturaDAO;
import com.hotel.modelo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del hilo ProcesadorFacturas: procesamiento asíncrono y
 * apagado ordenado sin perder facturas encoladas.
 */
class ProcesadorFacturasTest {

    /** DAO de prueba: guarda en memoria en vez de en disco. */
    private static class FacturaDAOEnMemoria extends FacturaDAO {
        private final List<Factura> almacen = new ArrayList<Factura>();

        @Override
        public synchronized void guardarFactura(Factura factura) {
            almacen.add(factura);
        }

        synchronized int cantidad() {
            return almacen.size();
        }
    }

    private Factura nuevaFactura() {
        Cliente cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        Habitacion habitacion = new HabitacionSencilla(101, 100.0);
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 86_400_000L);
        return new Factura(new Reserva(cliente, habitacion, entrada, salida), "TARJETA");
    }

    @Test
    @DisplayName("Una factura nueva nace PENDIENTE")
    void facturaNuevaEstaPendiente() {
        assertEquals(Factura.PENDIENTE, nuevaFactura().getEstado());
    }

    @Test
    @DisplayName("El apagado ordenado procesa todas las facturas encoladas")
    void procesaTodaLaColaAntesDeDetenerse() throws Exception {
        FacturaDAOEnMemoria dao = new FacturaDAOEnMemoria();
        ProcesadorFacturas procesador = new ProcesadorFacturas(dao);

        Factura f1 = nuevaFactura();
        Factura f2 = nuevaFactura();

        procesador.start();
        procesador.encolarFactura(f1);
        procesador.encolarFactura(f2);
        procesador.detener();
        procesador.join(15_000); // tope de seguridad: no debe quedarse colgado

        assertFalse(procesador.isAlive(), "El hilo debió terminar tras detener()");
        assertEquals(2, procesador.getTotalProcesadas());
        assertEquals(0, procesador.getPendientes());
        assertEquals(2, dao.cantidad());
        assertEquals(Factura.PROCESADA, f1.getEstado());
        assertEquals(Factura.PROCESADA, f2.getEstado());
    }

    @Test
    @DisplayName("Encolar no bloquea al llamador mientras se procesa una factura")
    void encolarNoBloquea() throws Exception {
        ProcesadorFacturas procesador = new ProcesadorFacturas(new FacturaDAOEnMemoria());
        procesador.start();
        procesador.encolarFactura(nuevaFactura()); // ocupa el hilo ~1.5 s

        long inicio = System.currentTimeMillis();
        procesador.encolarFactura(nuevaFactura());
        long transcurrido = System.currentTimeMillis() - inicio;

        assertTrue(transcurrido < 500,
                "encolarFactura debe retornar de inmediato, tardó " + transcurrido + " ms");

        procesador.detener();
        procesador.join(15_000);
    }
}
