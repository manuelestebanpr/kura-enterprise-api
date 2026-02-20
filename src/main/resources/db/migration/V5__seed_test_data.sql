-- V5__seed_test_data.sql
-- KURA: Seed data for development/testing
-- Creates: 1 lab, 2 PoS, 1 admin user, 1 patient user, catalog services, lab offerings, inventory

-- ============================================================
-- 1. LABORATORY
-- ============================================================
INSERT INTO laboratories (id, name, nit, legal_name, email, phone, is_active, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Laboratorio Clínico San Rafael',
    '900123456-1',
    'Laboratorio Clínico San Rafael S.A.S.',
    'admin@sanrafael-lab.co',
    '6012345678',
    true, NOW(), NOW()
);

-- ============================================================
-- 2. POINTS OF SERVICE
-- ============================================================
INSERT INTO points_of_service (id, laboratory_id, name, address, city, department, phone, email, latitude, longitude, is_active, created_at, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000001',
     'San Rafael - Sede Norte',
     'Calle 100 #15-20, Usaquén',
     'Bogotá', 'Cundinamarca', '6019876543', 'norte@sanrafael-lab.co',
     4.6867, -74.0465, true, NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000001',
     'San Rafael - Sede Chapinero',
     'Carrera 7 #45-10, Chapinero',
     'Bogotá', 'Cundinamarca', '6019876544', 'chapinero@sanrafael-lab.co',
     4.6351, -74.0703, true, NOW(), NOW());

-- ============================================================
-- 3. ADMIN USER (login: admin@kura.com.co / Admin123!)
-- ============================================================
INSERT INTO users (id, cedula, full_name, email, password_hash, phone, role, laboratory_id, pos_id, consent_ley1581, consent_date, is_active, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    '1000000001',
    'Administrador KURA',
    'admin@kura.com.co',
    '$2b$10$YT2aVI/QIfzRQu1L/.CrnONokbwVfOS2Uwlw2iV87MYH8qJNZEiCa',
    '3001234567',
    'SUPER_ADMIN',
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    true, NOW(), true, NOW(), NOW()
);

-- ============================================================
-- 4. PATIENT USER (login: paciente@test.co / Admin123!)
-- ============================================================
INSERT INTO users (id, cedula, full_name, email, password_hash, phone, role, consent_ley1581, consent_date, is_active, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000002',
    '1000000002',
    'María García López',
    'paciente@test.co',
    '$2b$10$YT2aVI/QIfzRQu1L/.CrnONokbwVfOS2Uwlw2iV87MYH8qJNZEiCa',
    '3009876543',
    'PATIENT',
    true, NOW(), true, NOW(), NOW()
);

-- ============================================================
-- 5. MASTER SERVICES (SINGLE tests)
-- ============================================================
INSERT INTO master_services (id, code, name, description, service_type, category, base_price, is_active, is_custom, created_at, updated_at)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'HEMO-001', 'Hemograma Completo',
     'Análisis completo de sangre que incluye conteo de glóbulos rojos, blancos, plaquetas, hemoglobina y hematocrito. Fundamental para detectar anemia, infecciones y trastornos hematológicos.',
     'SINGLE', 'Hematología', 45000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000002', 'PERF-001', 'Perfil Lipídico',
     'Medición de colesterol total, HDL, LDL y triglicéridos. Esencial para evaluar riesgo cardiovascular. Requiere ayuno de 12 horas.',
     'SINGLE', 'Bioquímica', 78000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000003', 'GLUC-001', 'Glucosa en Sangre',
     'Medición de niveles de glucosa en sangre en ayunas. Indicado para diagnóstico y control de diabetes mellitus.',
     'SINGLE', 'Bioquímica', 25000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000004', 'TIRO-001', 'Perfil Tiroideo (TSH, T3, T4)',
     'Evaluación completa de la función tiroidea incluyendo TSH, T3 libre y T4 libre. Detecta hipo e hipertiroidismo.',
     'SINGLE', 'Endocrinología', 125000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000005', 'ORIN-001', 'Uroanálisis (Parcial de Orina)',
     'Examen físico, químico y microscópico de orina. Detecta infecciones urinarias, enfermedad renal y diabetes.',
     'SINGLE', 'Uroanálisis', 18000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000006', 'CREA-001', 'Creatinina Sérica',
     'Medición de creatinina en sangre para evaluar función renal. Importante en pacientes con hipertensión y diabetes.',
     'SINGLE', 'Bioquímica', 22000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000007', 'HEPA-001', 'Perfil Hepático',
     'Incluye ALT, AST, bilirrubinas, fosfatasa alcalina y GGT. Evalúa función del hígado y vías biliares.',
     'SINGLE', 'Bioquímica', 95000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000008', 'VITA-001', 'Vitamina D (25-OH)',
     'Medición de niveles de vitamina D en sangre. Importante para salud ósea y sistema inmunológico.',
     'SINGLE', 'Endocrinología', 65000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000009', 'PCR-001', 'Proteína C Reactiva (PCR)',
     'Marcador de inflamación sistémica. Útil para evaluar infecciones, enfermedades autoinmunes y riesgo cardiovascular.',
     'SINGLE', 'Inmunología', 35000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000010', 'HIERR-001', 'Hierro Sérico + Ferritina',
     'Evaluación de reservas de hierro en el organismo. Esencial para diagnóstico de anemia ferropénica.',
     'SINGLE', 'Hematología', 55000.00, true, false, NOW(), NOW());

-- ============================================================
-- 6. MASTER SERVICES (BUNDLES)
-- ============================================================
INSERT INTO master_services (id, code, name, description, service_type, category, base_price, is_active, is_custom, created_at, updated_at)
VALUES
    ('d0000000-0000-0000-0000-000000000020', 'CHKP-001', 'Chequeo General Básico',
     'Paquete completo de exámenes de rutina: hemograma, glucosa, perfil lipídico y uroanálisis. Ideal para control anual de salud.',
     'BUNDLE', 'Chequeos', 140000.00, true, false, NOW(), NOW()),

    ('d0000000-0000-0000-0000-000000000021', 'CHKP-002', 'Chequeo Ejecutivo Premium',
     'Paquete avanzado que incluye hemograma, perfil lipídico, perfil tiroideo, perfil hepático, glucosa, creatinina, vitamina D y PCR. Para una evaluación integral.',
     'BUNDLE', 'Chequeos', 420000.00, true, false, NOW(), NOW());

-- ============================================================
-- 7. BUNDLE ITEMS
-- ============================================================
-- Chequeo General Básico: hemograma + glucosa + perfil lipídico + uroanálisis
INSERT INTO bundle_items (id, bundle_id, service_id, quantity, sort_order, created_at) VALUES
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000001', 1, 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000003', 1, 2, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000002', 1, 3, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000020', 'd0000000-0000-0000-0000-000000000005', 1, 4, NOW());

-- Chequeo Ejecutivo: hemograma + lipídico + tiroideo + hepático + glucosa + creatinina + vitD + PCR
INSERT INTO bundle_items (id, bundle_id, service_id, quantity, sort_order, created_at) VALUES
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000001', 1, 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000002', 1, 2, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000004', 1, 3, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000007', 1, 4, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000003', 1, 5, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000006', 1, 6, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000008', 1, 7, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000021', 'd0000000-0000-0000-0000-000000000009', 1, 8, NOW());

-- ============================================================
-- 8. LAB OFFERINGS (both PoS offer all services)
-- ============================================================
INSERT INTO lab_offerings (id, laboratory_id, pos_id, service_id, price, turnaround_hours, is_available, created_at, updated_at)
VALUES
    -- Sede Norte offerings
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 42000.00, 4, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 75000.00, 6, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 23000.00, 2, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000004', 120000.00, 24, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000005', 16000.00, 2, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000006', 20000.00, 4, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000007', 90000.00, 8, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000008', 62000.00, 24, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000009', 33000.00, 4, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000010', 52000.00, 6, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000020', 135000.00, 8, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000021', 399000.00, 48, true, NOW(), NOW()),
    -- Sede Chapinero offerings (slightly different prices)
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 44000.00, 6, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 76000.00, 8, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', 24000.00, 3, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000004', 122000.00, 24, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000005', 17000.00, 3, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000020', 138000.00, 10, true, NOW(), NOW()),
    (uuid_generate_v4(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000021', 410000.00, 48, true, NOW(), NOW());

-- ============================================================
-- 9. TEST DEPENDENCIES (BOM - inventory items consumed per test)
-- ============================================================
INSERT INTO test_dependencies (id, service_id, item_code, quantity_needed, created_at) VALUES
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'TUBE-EDTA', 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000001', 'NEEDLE-21G', 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'TUBE-SST', 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000002', 'NEEDLE-21G', 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000003', 'TUBE-FLUORIDE', 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000005', 'CUP-URINE', 1, NOW()),
    (uuid_generate_v4(), 'd0000000-0000-0000-0000-000000000004', 'TUBE-SST', 2, NOW());

-- ============================================================
-- 10. WAREHOUSE INVENTORY (stock for Sede Norte)
-- ============================================================
INSERT INTO warehouse_inventory (id, pos_id, item_code, item_name, quantity, min_threshold, unit, last_restocked, created_at, updated_at)
VALUES
    (uuid_generate_v4(), 'b0000000-0000-0000-0000-000000000001', 'TUBE-EDTA', 'Tubo EDTA (tapa morada)', 500, 50, 'UNIT', NOW(), NOW(), NOW()),
    (uuid_generate_v4(), 'b0000000-0000-0000-0000-000000000001', 'TUBE-SST', 'Tubo SST (tapa roja)', 400, 50, 'UNIT', NOW(), NOW(), NOW()),
    (uuid_generate_v4(), 'b0000000-0000-0000-0000-000000000001', 'TUBE-FLUORIDE', 'Tubo Fluoruro (tapa gris)', 200, 30, 'UNIT', NOW(), NOW(), NOW()),
    (uuid_generate_v4(), 'b0000000-0000-0000-0000-000000000001', 'NEEDLE-21G', 'Aguja 21G', 1000, 100, 'UNIT', NOW(), NOW(), NOW()),
    (uuid_generate_v4(), 'b0000000-0000-0000-0000-000000000001', 'CUP-URINE', 'Vaso recolector orina', 300, 50, 'UNIT', NOW(), NOW(), NOW()),
    (uuid_generate_v4(), 'b0000000-0000-0000-0000-000000000001', 'ALCOHOL-PAD', 'Algodón con alcohol', 2000, 200, 'UNIT', NOW(), NOW(), NOW()),
    (uuid_generate_v4(), 'b0000000-0000-0000-0000-000000000001', 'TOURNIQUET', 'Torniquete', 50, 10, 'UNIT', NOW(), NOW(), NOW());

-- ============================================================
-- 11. AUDIT LOG ENTRY (seed event)
-- ============================================================
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, new_value, ip_address, created_at)
VALUES (
    uuid_generate_v4(),
    'c0000000-0000-0000-0000-000000000001',
    'SEED_DATA',
    'SYSTEM',
    null,
    '{"description": "V5 seed data loaded for development/testing"}',
    '127.0.0.1',
    NOW()
);
