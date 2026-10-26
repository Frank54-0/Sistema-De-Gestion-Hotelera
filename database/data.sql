-- =============================================================
-- SuiteFlow - Datos iniciales
-- INSERT IGNORE: si la habitación ya existe no se toca, así el
-- script se puede ejecutar en cada arranque sin duplicar datos.
-- =============================================================

INSERT IGNORE INTO habitacion (numero, tipo, precio_base, estado) VALUES
    (101, 'SENCILLA', 100.00, 'DISPONIBLE'),
    (102, 'DOBLE',    150.00, 'DISPONIBLE'),
    (103, 'SENCILLA', 100.00, 'DISPONIBLE'),
    (104, 'DOBLE',    150.00, 'DISPONIBLE'),
    (201, 'SUITE',    300.00, 'DISPONIBLE'),
    (202, 'SUITE',    300.00, 'DISPONIBLE');