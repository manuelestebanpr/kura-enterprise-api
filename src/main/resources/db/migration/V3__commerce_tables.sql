-- V3__commerce_tables.sql
-- KURA Commerce: Orders, Cart, Walk-In Tickets, Payments

-- ORDERS
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number    VARCHAR(50) NOT NULL UNIQUE,
    user_id         UUID REFERENCES users(id),
    pos_id          UUID NOT NULL REFERENCES points_of_service(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method  VARCHAR(30) NOT NULL DEFAULT 'PAY_AT_LAB',
    subtotal        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total           DECIMAL(12, 2) NOT NULL DEFAULT 0,
    notes           TEXT,
    guest_name      VARCHAR(255),
    guest_email     VARCHAR(255),
    guest_phone     VARCHAR(20),
    expires_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_status CHECK (status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'SAMPLE_TAKEN', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('PAY_AT_LAB', 'MERCADOPAGO'))
);

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_pos ON orders (pos_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_number ON orders (order_number);
CREATE INDEX idx_orders_created ON orders (created_at);

-- ORDER ITEMS
CREATE TABLE order_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL REFERENCES orders(id),
    service_id      UUID NOT NULL REFERENCES master_services(id),
    service_name    VARCHAR(255) NOT NULL,
    price           DECIMAL(12, 2) NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- WALK-IN TICKETS
CREATE TABLE walkin_tickets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticket_code     VARCHAR(20) NOT NULL UNIQUE,
    order_id        UUID NOT NULL REFERENCES orders(id),
    pos_id          UUID NOT NULL REFERENCES points_of_service(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    redeemed_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ticket_status CHECK (status IN ('ACTIVE', 'REDEEMED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_walkin_tickets_code ON walkin_tickets (ticket_code);
CREATE INDEX idx_walkin_tickets_order ON walkin_tickets (order_id);
CREATE INDEX idx_walkin_tickets_pos ON walkin_tickets (pos_id);
CREATE INDEX idx_walkin_tickets_expires ON walkin_tickets (expires_at);

-- PAYMENTS
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id            UUID NOT NULL REFERENCES orders(id),
    payment_method      VARCHAR(30) NOT NULL,
    external_id         VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount              DECIMAL(12, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'COP',
    provider_response   JSONB,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REFUNDED'))
);

CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_external ON payments (external_id);
CREATE INDEX idx_payments_status ON payments (status);
