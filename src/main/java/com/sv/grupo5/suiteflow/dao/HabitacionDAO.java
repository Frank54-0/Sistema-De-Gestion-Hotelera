package com.sv.grupo5.suiteflow.dao;

import com.sv.grupo5.suiteflow.model.Habitacion;


//  Contrato de acceso a datos de {@link Habitacion}.
//
//  <p>El identificador de una habitación es su número (Integer). Las subclases
//  (sencilla, doble y suite) se manejan por polimorfismo, sin lógica adicional
//  en el contrato.</p>


public interface HabitacionDAO extends CrudDAO<Habitacion, Integer> {
}
