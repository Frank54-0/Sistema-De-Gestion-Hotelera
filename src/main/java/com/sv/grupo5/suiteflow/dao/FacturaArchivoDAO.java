package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.exception.PersistenciaException;
import com.sv.grupo5.suiteflow.model.Factura;

import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link FacturaDAO} basada en archivos, persistida en
 * {@code datos/facturas.dat}.
 *
 * <p>Es usada tanto por el hilo principal como por ProcesadorFacturas, por lo
 * que la sincronización heredada de ArchivoDAO es indispensable aquí.</p>
 */
public class FacturaArchivoDAO extends ArchivoDAO<Factura> implements FacturaDAO {

    private static final String ARCHIVO = "facturas.dat";

    /** Construye el DAO apuntando a datos/facturas.dat. */
    public FacturaArchivoDAO() {
        super(ARCHIVO);
    }

    @Override
    public void guardar(Factura factura) {
        agregar(factura);
    }

    @Override
    public void actualizar(Factura factura) {
        boolean existe = reemplazarPrimero(f -> f.getId().equals(factura.getId()), factura);
        if (!existe) {
            throw new PersistenciaException(
                    "No existe la factura a actualizar: " + factura.getId());
        }
    }

    @Override
    public Optional<Factura> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return buscarPrimero(f -> id.equals(f.getId()));
    }

    @Override
    public List<Factura> listarTodos() {
        return cargarTodos();
    }

    @Override
    public boolean eliminar(String id) {
        if (id == null) {
            return false;
        }
        return eliminarSi(f -> id.equals(f.getId()));
    }
}