package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato CRUD y garantías de archivo aplicados a FacturaArchivoDAO. */
class FacturaArchivoDAOTest extends ArchivoDAOContrato<Factura, String> {

    private final Cliente cliente = new Cliente("Juan Pérez", "77771234", "juan@example.com");

    @Override
    protected CrudDAO<Factura, String> crearDAO() {
        return new FacturaArchivoDAO();
    }

    @Override
    protected Factura nueva(int n) {
        Date entrada = new Date();
        Date salida = new Date(entrada.getTime() + 86_400_000L);
        Reserva reserva = new Reserva(cliente, new HabitacionSencilla(100 + n, 100.0), entrada, salida);
        return new Factura(reserva, "TARJETA");
    }

    @Override
    protected String nombreArchivo() {
        return "facturas.dat";
    }

    @Override
    protected String idDe(Factura f) {
        return f.getId();
    }

    @Override
    protected Factura modificar(Factura original) {
        original.generarFactura();
        return original;
    }

    @Override
    protected boolean estaModificada(Factura f) {
        return Factura.PROCESADA.equals(f.getEstado());
    }

    @Test
    @DisplayName("Una factura cargada conserva total, forma de pago y reserva asociada")
    void facturaConservaSusDatos() {
        Factura f = nueva(1);
        dao.guardar(f);

        Factura cargada = dao.buscarPorId(f.getId()).get();

        assertEquals(100.0, cargada.getTotal(), 0.001);
        assertEquals("TARJETA", cargada.getFormaPago());
        assertEquals(Factura.PENDIENTE, cargada.getEstado());
        assertEquals(f.getReserva().getId(), cargada.getReserva().getId());
    }
}
