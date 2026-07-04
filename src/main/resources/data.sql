-- =========================================================================
-- 1. SEED SYSTEM ROLES (Matching image_6811df.png Naming Strategy)
-- =========================================================================
INSERT INTO roles (name) VALUES
                             ('ROLE_SUPER_ADMIN'),
                             ('ROLE_OPS_ADMIN'),
                             ('ROLE_RISK_ANALYST'),
                             ('ROLE_MERCHANT_OWNER'),
                             ('ROLE_MERCHANT_VIEWER')
    ON CONFLICT (name) DO NOTHING;

-- =========================================================================
-- 2. SEED FINE-GRAINED ROLE PERMISSIONS (Dynamic ID Lookups)
-- =========================================================================

-- Assign Fine-Grained Authorities to ROLE_SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'refund:create' FROM roles WHERE name = 'ROLE_SUPER_ADMIN' UNION ALL
SELECT id, 'merchant:approve' FROM roles WHERE name = 'ROLE_SUPER_ADMIN' UNION ALL
SELECT id, 'dashboard:read' FROM roles WHERE name = 'ROLE_SUPER_ADMIN' UNION ALL
SELECT id, 'audit:view' FROM roles WHERE name = 'ROLE_SUPER_ADMIN' UNION ALL
SELECT id, 'merchant:write' FROM roles WHERE name = 'ROLE_SUPER_ADMIN'
    ON CONFLICT (role_id, permission) DO NOTHING;

-- Assign Fine-Grained Authorities to ROLE_RISK_ANALYST
INSERT INTO role_permissions (role_id, permission)
SELECT id, 'dashboard:read' FROM roles WHERE name = 'ROLE_RISK_ANALYST'
    ON CONFLICT (role_id, permission) DO NOTHING;