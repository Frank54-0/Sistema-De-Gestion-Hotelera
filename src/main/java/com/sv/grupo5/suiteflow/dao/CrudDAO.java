package com.sv.grupo5.suiteflow.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD genérico de la capa DAO.
 *
 * <p>Define las cuatro operaciones básicas (crear, leer, actualizar y eliminar)
 * sin decir cómo se almacenan los datos. Lo implementan tanto los DAO basados en
 * archivos como los que usan JDBC, de modo que la capa de servicio depende solo
 * de esta abstracción y no de la tecnología de persistencia.</p>
 *
 * <p>Cualquier falla de almacenamiento se reporta con
 * {@code PersistenciaException}; los detalles técnicos (SQLException,
 * IOException) no salen de la capa DAO.</p>
 *
 * @param <T>  tipo de la entidad que maneja el DAO
 * @param <ID> tipo del identificador de la entidad (String, Integer, etc.)
 */

public interface CrudDAO <T, ID>{

    // Crea una entidad nueva en el almacenamiento.
    // entidad entidad a guardar

    void guardar(T entidad);

    //Actualiza una entidad que ya existe
    //entidad con los datos nuevos

    void actualizar(T entidad);

    //Busca una entidad por si identificador
    // id Identificador de la entidad
    //Retorna la entidad, o un Opcional vacio si no existe

    Optional<T> buscarPorId(ID id);

    //Devuelve todas las entidades almacenadas.
    //Retorna lista de entidades; vacia si no hay ninguna nunca null

    List<T> listarTodos();

    //Elimina una entidad por su identificador ID
    // id identificador a eliminar
    // Retorna true si existia y se elimino, false si no se encontro.

    boolean eliminar(ID id);

}
