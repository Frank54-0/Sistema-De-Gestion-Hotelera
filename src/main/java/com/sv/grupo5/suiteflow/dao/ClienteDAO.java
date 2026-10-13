package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.Cliente;

//
//  Contrato de acceso a datos de Cliente; el identificador es el id
//  de tipo String del cliente.
// /

public interface ClienteDAO extends CrudDAO<Cliente, String> {
}
