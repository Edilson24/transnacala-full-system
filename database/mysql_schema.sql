-- Esquema inicial de desenvolvimento do TransNacala.
-- Compatível com MySQL 8.0.16+ (CHECK constraints aplicadas).
-- Executar no MySQL Workbench. A API, e não os clientes, acede a esta base.

CREATE DATABASE IF NOT EXISTS transnacala
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE transnacala;

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(32) NULL,
    role ENUM('ADMIN', 'DRIVER', 'PASSENGER') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    KEY ix_users_role_active (role, is_active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS routes (
    id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    origin VARCHAR(160) NOT NULL,
    destination VARCHAR(160) NOT NULL,
    fare DECIMAL(10, 2) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_routes_active_name (is_active, name),
    CONSTRAINT chk_routes_fare_nonnegative CHECK (fare >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stops (
    id CHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_stops_active_name (is_active, name),
    CONSTRAINT chk_stops_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_stops_longitude CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS route_stops (
    route_id CHAR(36) NOT NULL,
    stop_id CHAR(36) NOT NULL,
    stop_order INT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (route_id, stop_id),
    UNIQUE KEY uq_route_stops_order (route_id, stop_order),
    CONSTRAINT fk_route_stops_route
        FOREIGN KEY (route_id) REFERENCES routes (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_route_stops_stop
        FOREIGN KEY (stop_id) REFERENCES stops (id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vehicles (
    id CHAR(36) NOT NULL,
    code VARCHAR(16) NOT NULL,
    driver_user_id CHAR(36) NULL,
    route_id CHAR(36) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_vehicles_code (code),
    KEY ix_vehicles_driver (driver_user_id),
    KEY ix_vehicles_route_active (route_id, is_active),
    CONSTRAINT fk_vehicles_driver
        FOREIGN KEY (driver_user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_vehicles_route
        FOREIGN KEY (route_id) REFERENCES routes (id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tracking_sessions (
    id CHAR(36) NOT NULL,
    vehicle_id CHAR(36) NOT NULL,
    driver_user_id CHAR(36) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tracking_sessions_id_vehicle (id, vehicle_id),
    KEY ix_tracking_sessions_vehicle_started (vehicle_id, started_at),
    KEY ix_tracking_sessions_driver_started (driver_user_id, started_at),
    CONSTRAINT fk_tracking_sessions_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tracking_sessions_driver
        FOREIGN KEY (driver_user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_tracking_session_end
        CHECK (ended_at IS NULL OR ended_at >= started_at)
) ENGINE=InnoDB;

-- Esta tabela contém apenas a posição mais recente de uma sessão ativa.
-- Ao terminar a sessão, a API apaga esta linha e define tracking_sessions.ended_at.
CREATE TABLE IF NOT EXISTS live_locations (
    vehicle_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    accuracy_meters DECIMAL(8, 2) NULL,
    heading_degrees DECIMAL(6, 2) NULL,
    speed_meters_per_second DECIMAL(8, 2) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (vehicle_id),
    UNIQUE KEY uq_live_locations_session (session_id),
    KEY ix_live_locations_updated (updated_at),
    CONSTRAINT fk_live_locations_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_live_locations_session_vehicle
        FOREIGN KEY (session_id, vehicle_id)
        REFERENCES tracking_sessions (id, vehicle_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_live_locations_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_live_locations_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_live_locations_accuracy
        CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0),
    CONSTRAINT chk_live_locations_heading
        CHECK (heading_degrees IS NULL OR heading_degrees BETWEEN 0 AND 360),
    CONSTRAINT chk_live_locations_speed
        CHECK (speed_meters_per_second IS NULL OR speed_meters_per_second >= 0)
) ENGINE=InnoDB;

-- A linha única da sequência deve ser bloqueada (SELECT ... FOR UPDATE),
-- incrementada e confirmada na mesma transação que grava cada evento.
CREATE TABLE IF NOT EXISTS sync_sequence (
    id TINYINT UNSIGNED NOT NULL,
    last_change_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT chk_sync_sequence_singleton CHECK (id = 1)
) ENGINE=InnoDB;

INSERT IGNORE INTO sync_sequence (id, last_change_id) VALUES (1, 0);

-- Cursor incremental e tombstones do catálogo. Para ROUTE_STOP, entity_id é
-- route_id e related_entity_id é stop_id. A API grava eventos na mesma
-- transação da alteração e atribui IDs pela sync_sequence bloqueada.
CREATE TABLE IF NOT EXISTS sync_changes (
    id BIGINT UNSIGNED NOT NULL,
    entity_type ENUM('ROUTE', 'STOP', 'ROUTE_STOP', 'VEHICLE') NOT NULL,
    entity_id CHAR(36) NOT NULL,
    related_entity_id CHAR(36) NULL,
    operation ENUM('UPSERT', 'DELETE') NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_sync_changes_entity (entity_type, entity_id, related_entity_id),
    KEY ix_sync_changes_changed_at (changed_at)
) ENGINE=InnoDB;

-- Edilson: já executei este script.
