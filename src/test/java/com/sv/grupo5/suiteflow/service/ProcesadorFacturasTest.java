package com.sv.grupo5.suiteflow.service;

import com.sv.grupo5.suiteflow.dao.DAOEnMemoria;
import com.sv.grupo5.suiteflow.dao.FacturaDAO;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Factura;
import com.sv.grupo5.suiteflow.model.Habitacion;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.model.Reserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del hilo ProcesadorFacturas: procesamiento asíncrono y
 * apagado ordenado sin perder facturas encoladas.
 *
 * Usa un DAO en memoria que implementa la interfaz FacturaDAO: el servicio
 * no sabe, ni necesita saber, dónde se guardan realmente las facturas.
 */
class ProcesadorFacturasTest {

    private Factura nuevaFactura() {
        Cliente cliente = new Cliente("Juan Pérez", "77771234", "juan@example.com");
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
        FacturaDAO dao = new DAOEnMemoria.FacturaEnMemoria();
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
        assertEquals(2, dao.listarTodos().size());
        assertEquals(Factura.PROCESADA, f1.getEstado());
        assertEquals(Factura.PROCESADA, f2.getEstado());
    }

    @Test
    @DisplayName("Encolar no bloquea al llamador mientras se procesa una factura")
    void encolarNoBloquea() throws Exception {
        ProcesadorFacturas procesador = new ProcesadorFacturas(new DAOEnMemoria.FacturaEnMemoria());
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