-- =========================================================================
-- 1. STRUCTURAL RBAC CORE (Normalized User-Role-Permission Engine)
-- =========================================================================
CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE permissions (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE role_permissions (
                                  role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id BIGINT REFERENCES permissions(id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(100) UNIQUE NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
                            user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
                            role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

-- =========================================================================
-- 2. BUSINESS ENGINE (Module 1, 2, & 3 Core Domains)
-- =========================================================================
CREATE TABLE merchants (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT REFERENCES users(id),
                           business_name VARCHAR(255) NOT NULL,
                           pan_number VARCHAR(10) UNIQUE,
                           gst_number VARCHAR(15) UNIQUE,
                           bank_account_number VARCHAR(20),
                           bank_ifsc VARCHAR(11),
                           status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE merchant_documents (
                                    id BIGSERIAL PRIMARY KEY,
                                    merchant_id BIGINT REFERENCES merchants(id) ON DELETE CASCADE,
                                    document_type VARCHAR(50) NOT NULL, -- PAN, GST_CERT, BANK_PROOFS
                                    file_name VARCHAR(255) NOT NULL,
                                    file_size BIGINT NOT NULL,
                                    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
                                    rejection_reason TEXT,
                                    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              merchant_id BIGINT REFERENCES merchants(id),
                              amount NUMERIC(15, 2) NOT NULL,
                              currency VARCHAR(3) DEFAULT 'INR',
                              status VARCHAR(50) NOT NULL, -- SUCCESS, FAILED, REFUNDED, PARTIALLY_REFUNDED
                              payment_mode VARCHAR(50) NOT NULL,
                              masked_pan VARCHAR(19) NOT NULL,
                              vpa_masked VARCHAR(100),
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crucial composite high-performance index for Module 2 (Paginating 100k+ records instantly)
CREATE INDEX idx_tx_pagination ON transactions(merchant_id, created_at DESC, status);

-- =========================================================================
-- 3. DISTRIBUTED IDEMPOTENCY LOCKS & APPEND-ONLY AUDIT TRAILS
-- =========================================================================
CREATE TABLE idempotent_requests (
                                     idempotency_key VARCHAR(255) PRIMARY KEY,
                                     request_path VARCHAR(255) NOT NULL,
                                     response_body TEXT,
                                     status VARCHAR(50) NOT NULL, -- IN_PROGRESS, COMPLETED
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
                            id BIGSERIAL PRIMARY KEY,
                            action_type VARCHAR(100) NOT NULL,
                            performed_by VARCHAR(100) NOT NULL,
                            client_ip VARCHAR(45) NOT NULL,
                            payload JSONB,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- DB Trigger Engine enforcing explicit Append-Only operations
CREATE OR REPLACE FUNCTION freeze_audit_trail_history() 
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'PCI-DSS COMPLIANCE ERROR: Database level adjustments, modifications, or deletes are strictly denied on system logs.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_append_only_logs
    BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION freeze_audit_trail_history();

-- Seed initial basic definitions
INSERT INTO roles (name) VALUES ('SUPER_ADMIN'), ('OPS_ADMIN'), ('RISK_ANALYST'), ('MERCHANT_OWNER'), ('MERCHANT_VIEWER');
INSERT INTO permissions (name) VALUES ('refund:create'), ('merchant:approve'), ('dashboard:read'), ('audit:view'), ('merchant:write');

-- Map core structural components
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), -- Super admin has everything
                                                          (3, 3); -- Risk analyst read dashboard views

