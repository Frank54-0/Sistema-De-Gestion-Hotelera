package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.Factura;

// Contrato de acceso a datos de {@link Factura}.
//
// Lo usa tanto el hilo principal como el procesador de facturas, por lo que
// toda implementación debe ser segura para uso concurrente.</p>

public interface FacturaDAO extends CrudDAO<Factura, String> {
}
