-- =============================================================
-- SuiteFlow - Esquema de base de datos (MySQL 8)
-- Propuesta alineada con el modelo Java
-- La aplicación lo ejecuta al arrancar (InicializadorBaseDatos),
-- por eso todo es idempotente (IF NOT EXISTS).
-- =============================================================

CREATE DATABASE IF NOT EXISTS suiteflow;
USE suiteflow;

-- Habitaciones del hotel. "tipo" indica qué subclase de Habitacion se crea al leer.
CREATE TABLE IF NOT EXISTS habitacion (
    numero       INT            NOT NULL,
    tipo         VARCHAR(10)    NOT NULL,
    precio_base  DECIMAL(10, 2) NOT NULL,
    estado       VARCHAR(15)    NOT NULL DEFAULT 'DISPONIBLE',
    CONSTRAINT pk_habitacion PRIMARY KEY (numero),
    CONSTRAINT ck_habitacion_tipo   CHECK (tipo IN ('SENCILLA', 'DOBLE', 'SUITE')),
    CONSTRAINT ck_habitacion_precio CHECK (precio_base >= 0),
    CONSTRAINT ck_habitacion_estado CHECK (estado IN ('DISPONIBLE', 'OCUPADA', 'MANTENIMIENTO'))
);

-- Huéspedes. El id es el UUID que genera la clase Cliente.
CREATE TABLE IF NOT EXISTS cliente (
    id        VARCHAR(36)  NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    telefono  VARCHAR(20)  NOT NULL,
    email     VARCHAR(100),
    CONSTRAINT pk_cliente PRIMARY KEY (id)
);

-- Reservas. El índice por habitación y fechas acelera la consulta de solapamiento
-- que se ejecuta en cada reserva (ReservaJdbcDAO.guardarSiHabitacionLibre).
CREATE TABLE IF NOT EXISTS reserva (
    id                 VARCHAR(36) NOT NULL,
    cliente_id         VARCHAR(36) NOT NULL,
    numero_habitacion  INT         NOT NULL,
    fecha_entrada      DATE        NOT NULL,
    fecha_salida       DATE        NOT NULL,
    estado             VARCHAR(12) NOT NULL DEFAULT 'ACTIVA',
    CONSTRAINT pk_reserva PRIMARY KEY (id),
    CONSTRAINT fk_reserva_cliente    FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_reserva_habitacion FOREIGN KEY (numero_habitacion) REFERENCES habitacion (numero),
    CONSTRAINT ck_reserva_fechas CHECK (fecha_salida > fecha_entrada),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('ACTIVA', 'CANCELADA', 'FINALIZADA')),
    INDEX idx_reserva_habitacion_fechas (numero_habitacion, estado, fecha_entrada, fecha_salida)
);

-- Facturas. UNIQUE en reserva_id: una reserva no se puede facturar dos veces (QA-11).
CREATE TABLE IF NOT EXISTS factura (
    id          VARCHAR(36)    NOT NULL,
    reserva_id  VARCHAR(36)    NOT NULL,
    total       DECIMAL(10, 2) NOT NULL,
    forma_pago  VARCHAR(20),
    estado      VARCHAR(12)    NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT pk_factura PRIMARY KEY (id),
    CONSTRAINT uq_factura_reserva UNIQUE (reserva_id),
    CONSTRAINT fk_factura_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id),
    CONSTRAINT ck_factura_estado CHECK (estado IN ('PENDIENTE', 'PROCESADA'))
);