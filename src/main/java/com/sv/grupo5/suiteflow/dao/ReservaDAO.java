package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.Reserva;

//Contrato de acceso a datos de Reserva.

//Hereda las operaciones CRUD de CrudDAO; el identificador de una reserva
// es su id de tipo String. Aqui se agregaria las consultas propias
// de las reservas que no sean CRUD basico.

public interface ReservaDAO extends CrudDAO<Reserva, String>{
}
