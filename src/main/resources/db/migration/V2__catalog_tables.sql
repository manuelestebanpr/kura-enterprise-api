-- V2__catalog_tables.sql
-- KURA Catalog: Master Services, Bundles, Lab Offerings, Test Dependencies (BOM)

-- MASTER SERVICES (Composite Pattern: SINGLE or BUNDLE)
CREATE TABLE master_services (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    service_type    VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    category        VARCHAR(100),
    base_price      DECIMAL(12, 2),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_custom       BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_type CHECK (service_type IN ('SINGLE', 'BUNDLE'))
);

CREATE INDEX idx_master_services_code ON master_services (code);
CREATE INDEX idx_master_services_name_trgm ON master_services USING gin (name gin_trgm_ops);
CREATE INDEX idx_master_services_category ON master_services (category);
CREATE INDEX idx_master_services_type ON master_services (service_type);

-- BUNDLE ITEMS (links BUNDLE -> SINGLE services)
CREATE TABLE bundle_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bundle_id       UUID NOT NULL REFERENCES master_services(id),
    service_id      UUID NOT NULL REFERENCES master_services(id),
    quantity        INTEGER NOT NULL DEFAULT 1,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_bundle_service UNIQUE (bundle_id, service_id),
    CONSTRAINT chk_bundle_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_bundle_items_bundle ON bundle_items (bundle_id);
CREATE INDEX idx_bundle_items_service ON bundle_items (service_id);

-- LAB OFFERINGS (what each lab/PoS offers and at what price)
CREATE TABLE lab_offerings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    laboratory_id   UUID NOT NULL REFERENCES laboratories(id),
    pos_id          UUID NOT NULL REFERENCES points_of_service(id),
    service_id      UUID NOT NULL REFERENCES master_services(id),
    price           DECIMAL(12, 2) NOT NULL,
    turnaround_hours INTEGER,
    is_available    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_lab_offering UNIQUE (pos_id, service_id)
);

CREATE INDEX idx_lab_offerings_lab ON lab_offerings (laboratory_id);
CREATE INDEX idx_lab_offerings_pos ON lab_offerings (pos_id);
CREATE INDEX idx_lab_offerings_service ON lab_offerings (service_id);

-- TEST DEPENDENCIES / BOM (Bill of Materials — what inventory items a test consumes)
CREATE TABLE test_dependencies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_id      UUID NOT NULL REFERENCES master_services(id),
    item_code       VARCHAR(50) NOT NULL,
    quantity_needed INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_test_dep UNIQUE (service_id, item_code),
    CONSTRAINT chk_dep_quantity CHECK (quantity_needed > 0)
);

CREATE INDEX idx_test_deps_service ON test_dependencies (service_id);
CREATE INDEX idx_test_deps_item ON test_dependencies (item_code);
