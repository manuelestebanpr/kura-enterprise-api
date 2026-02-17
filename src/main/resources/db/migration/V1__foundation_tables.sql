-- V1__foundation_tables.sql
-- KURA Foundation: Users, Laboratories, Points of Service, Inventory, Audit

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- USERS
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cedula          VARCHAR(20) NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(50) NOT NULL DEFAULT 'PATIENT',
    laboratory_id   UUID,
    pos_id          UUID,
    consent_ley1581 BOOLEAN NOT NULL DEFAULT FALSE,
    consent_date    TIMESTAMP WITH TIME ZONE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_role CHECK (role IN ('PATIENT', 'LAB_ADMIN', 'LAB_TECH', 'POS_MANAGER', 'SUPPORT', 'SUPER_ADMIN'))
);

CREATE INDEX idx_users_full_name_trgm ON users USING gin (full_name gin_trgm_ops);
CREATE INDEX idx_users_cedula ON users (cedula);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_laboratory_id ON users (laboratory_id);
CREATE INDEX idx_users_pos_id ON users (pos_id);

-- LABORATORIES
CREATE TABLE laboratories (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    nit             VARCHAR(20) NOT NULL UNIQUE,
    legal_name      VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(20),
    logo_url        VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_laboratories_nit ON laboratories (nit);
CREATE INDEX idx_laboratories_name_trgm ON laboratories USING gin (name gin_trgm_ops);

-- POINTS OF SERVICE
CREATE TABLE points_of_service (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    laboratory_id   UUID NOT NULL REFERENCES laboratories(id),
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    department      VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(255),
    latitude        DECIMAL(10, 8),
    longitude       DECIMAL(11, 8),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pos_laboratory_id ON points_of_service (laboratory_id);
CREATE INDEX idx_pos_city ON points_of_service (city);
CREATE INDEX idx_pos_name_trgm ON points_of_service USING gin (name gin_trgm_ops);

-- WAREHOUSE INVENTORY (1:1 with PoS per item)
CREATE TABLE warehouse_inventory (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pos_id          UUID NOT NULL REFERENCES points_of_service(id),
    item_code       VARCHAR(50) NOT NULL,
    item_name       VARCHAR(255) NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 0,
    min_threshold   INTEGER NOT NULL DEFAULT 0,
    unit            VARCHAR(50) NOT NULL DEFAULT 'UNIT',
    last_restocked  TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_pos_item UNIQUE (pos_id, item_code),
    CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0)
);

CREATE INDEX idx_inventory_pos_id ON warehouse_inventory (pos_id);
CREATE INDEX idx_inventory_item_code ON warehouse_inventory (item_code);

-- AUDIT LOGS (Immutable — Colombia compliance)
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID,
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at);

-- DEFERRED FOREIGN KEYS (avoid circular deps)
ALTER TABLE users ADD CONSTRAINT fk_users_laboratory FOREIGN KEY (laboratory_id) REFERENCES laboratories(id);
ALTER TABLE users ADD CONSTRAINT fk_users_pos FOREIGN KEY (pos_id) REFERENCES points_of_service(id);
