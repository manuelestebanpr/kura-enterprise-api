-- V4__results_and_shares.sql
-- KURA: Patient Results, Audio Notes, Share Links

-- PATIENT RESULTS
CREATE TABLE patient_results (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL REFERENCES orders(id),
    order_item_id   UUID REFERENCES order_items(id),
    patient_id      UUID REFERENCES users(id),
    pos_id          UUID NOT NULL REFERENCES points_of_service(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    result_data     JSONB,
    notes           TEXT,
    audio_url       VARCHAR(500),
    sample_taken_at TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_result_status CHECK (status IN ('PENDING', 'SAMPLE_TAKEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_results_order ON patient_results (order_id);
CREATE INDEX idx_results_patient ON patient_results (patient_id);
CREATE INDEX idx_results_pos ON patient_results (pos_id);
CREATE INDEX idx_results_status ON patient_results (status);

-- SHARE LINKS (48h expiring public access)
CREATE TABLE share_links (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    share_uuid      UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    result_id       UUID NOT NULL REFERENCES patient_results(id),
    created_by      UUID REFERENCES users(id),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    accessed_count  INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_share_links_uuid ON share_links (share_uuid);
CREATE INDEX idx_share_links_result ON share_links (result_id);
CREATE INDEX idx_share_links_expires ON share_links (expires_at);
