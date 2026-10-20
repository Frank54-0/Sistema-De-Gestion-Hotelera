package com.sv.grupo5.suiteflow.service;

import com.sv.grupo5.suiteflow.dao.DAOEnMemoria;
import com.sv.grupo5.suiteflow.model.Cliente;
import com.sv.grupo5.suiteflow.model.Factura;
import com.sv.grupo5.suiteflow.model.HabitacionSencilla;
import com.sv.grupo5.suiteflow.model.Reserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos que complementan a ProcesadorFacturasTest: entradas inválidas, apagado
 * sin trabajo, interrupción del hilo y estado de lo persistido.
 */
class ProcesadorFacturasCasosBordeTest {

    private Factura nuevaFactura() {
        Cliente cliente = new Cliente("Juan Pérez", "7777-1234", "juan@example.com");
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 86_400_000L);
        Reserva reserva = new Reserva(cliente, new HabitacionSencilla(101, 100.0), entrada, salida);
        return new Factura(reserva, "TARJETA");
    }

    @Test
    @DisplayName("El constructor rechaza un DAO null")
    void constructorRechazaDaoNull() {
        assertThrows(IllegalArgumentException.class, () -> new ProcesadorFacturas(null));
    }

    @Test
    @DisplayName("encolarFactura(null) se ignora")
    void encolarNullSeIgnora() {
        ProcesadorFacturas procesador = new ProcesadorFacturas(new DAOEnMemoria.FacturaEnMemoria());

        procesador.encolarFactura(null);

        assertEquals(0, procesador.getPendientes());
    }

    @Test
    @DisplayName("El hilo tiene un nombre identificable")
    void nombreDelHilo() {
        ProcesadorFacturas procesador = new ProcesadorFacturas(new DAOEnMemoria.FacturaEnMemoria());

        assertEquals("Hilo-ProcesadorFacturas", procesador.getName());
    }

    @Test
    @DisplayName("Detener sin facturas encoladas termina el hilo de inmediato")
    void detenerSinTrabajo() throws Exception {
        ProcesadorFacturas procesador = new ProcesadorFacturas(new DAOEnMemoria.FacturaEnMemoria());
        procesador.start();

        procesador.detener();
        procesador.join(3_000);

        assertFalse(procesador.isAlive());
        assertEquals(0, procesador.getTotalProcesadas());
    }

    @Test
    @DisplayName("Interrumpir al hilo mientras espera lo hace terminar sin colgarse")
    void interrupcionMientrasEspera() throws Exception {
        ProcesadorFacturas procesador = new ProcesadorFacturas(new DAOEnMemoria.FacturaEnMemoria());
        procesador.start();
        Thread.sleep(200); // deja que llegue a wait()

        procesador.interrupt();
        procesador.join(3_000);

        assertFalse(procesador.isAlive(), "El hilo debe salir al ser interrumpido");
    }

    @Test
    @DisplayName("Las facturas persistidas quedan PROCESADAS y conservan total y forma de pago")
    void facturaPersistidaYaProcesada() throws Exception {
        DAOEnMemoria.FacturaEnMemoria dao = new DAOEnMemoria.FacturaEnMemoria();
        ProcesadorFacturas procesador = new ProcesadorFacturas(dao);
        Factura factura = nuevaFactura();

        procesador.start();
        procesador.encolarFactura(factura);
        procesador.detener();
        procesador.join(15_000);

        List<Factura> guardadas = dao.listarTodos();
        assertEquals(1, guardadas.size());
        assertEquals(Factura.PROCESADA, guardadas.get(0).getEstado());
        assertEquals(100.0, guardadas.get(0).getTotal(), 0.001);
        assertEquals("TARJETA", guardadas.get(0).getFormaPago());
        assertEquals(factura.getId(), guardadas.get(0).getId());
    }

    @Test
    @DisplayName("Mientras la factura está en cola sigue PENDIENTE y no se ha persistido")
    void facturaEnColaNoSePersisteAntesDeProcesarse() throws Exception {
        DAOEnMemoria.FacturaEnMemoria dao = new DAOEnMemoria.FacturaEnMemoria();
        ProcesadorFacturas procesador = new ProcesadorFacturas(dao);
        Factura factura = nuevaFactura();

        // El hilo NO se inicia: la factura solo queda en cola.
        procesador.encolarFactura(factura);

        assertEquals(1, procesador.getPendientes());
        assertEquals(Factura.PENDIENTE, factura.getEstado());
        assertTrue(dao.listarTodos().isEmpty());
    }
}
