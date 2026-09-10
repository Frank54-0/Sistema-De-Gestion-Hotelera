package com.hotel.dao;

import com.hotel.modelo.Factura;

import java.util.List;

/**
 * Acceso a datos de la entidad Factura. Persiste en datos/facturas.dat.
 *
 * Es usado tanto por el hilo principal como por ProcesadorFacturas, por lo
 * que la sincronización heredada de ArchivoDAO es indispensable aquí.
 */
public class FacturaDAO extends ArchivoDAO<Factura> {

    private static final String ARCHIVO = "facturas.dat";

    /** Construye el DAO apuntando a datos/facturas.dat. */
    public FacturaDAO() {
        super(ARCHIVO);
    }

    /**
     * Guarda la lista completa de facturas, sobrescribiendo la anterior.
     *
     * @param facturas lista de facturas a persistir
     */
    public void guardarFacturas(List<Factura> facturas) {
        guardarTodos(facturas);
    }

    /**
     * Carga todas las facturas persistidas.
     *
     * @return lista de facturas; vacía si aún no hay datos
     */
    public List<Factura> cargarFacturas() {
        return cargarTodos();
    }

    /**
     * Agrega una factura a los datos persistidos.
     * Lo invoca ProcesadorFacturas cada vez que termina de procesar una.
     *
     * @param factura factura a agregar
     */
    public void guardarFactura(Factura factura) {
        agregar(factura);
    }
}
