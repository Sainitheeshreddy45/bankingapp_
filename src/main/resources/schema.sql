-- Ensure the procedural language engine is loaded before compilation
CREATE EXTENSION IF NOT EXISTS plpgsql;

-- =========================================================================
-- 1. STRUCTURAL IDENTITY & RBAC CORE
-- =========================================================================

-- Maps directly to Role.java
CREATE TABLE IF NOT EXISTS roles (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) UNIQUE NOT NULL
);

-- Maps directly to the @ElementCollection inside Role.java
CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                                permission VARCHAR(255) NOT NULL,
                                                PRIMARY KEY (role_id, permission)
);

-- Maps directly to User.java (Note: email acts as the username identity string)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(255) UNIQUE NOT NULL,
                                     password_hash VARCHAR(255) NOT NULL,
                                     password_updated_at TIMESTAMP,
                                     account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
                                     failed_login_attempts INT NOT NULL DEFAULT 0,
                                     lockout_end_time TIMESTAMP
);

-- Maps directly to the @ManyToMany join table inside User.java
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                          PRIMARY KEY (user_id, role_id)
);

-- Maps directly to the @ElementCollection inside User.java
CREATE TABLE IF NOT EXISTS user_password_history (
                                                     user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                     historic_password_hash VARCHAR(255) NOT NULL
);

-- High-performance optimization index for historical password checks
CREATE INDEX IF NOT EXISTS idx_user_password_history_lookup ON user_password_history(user_id);


-- =========================================================================
-- 2. BUSINESS DOMAINS & PLATFORM OPERATIONS
-- =========================================================================

-- Maps directly to Merchant.java
CREATE TABLE IF NOT EXISTS merchants (
                                         id BIGSERIAL PRIMARY KEY,
                                         owner_email VARCHAR(255) UNIQUE NOT NULL,
                                         business_name VARCHAR(255) NOT NULL,
                                         company_type VARCHAR(255),
                                         account_number VARCHAR(255),
                                         ifsc_code VARCHAR(11),
                                         director_details TEXT,
                                         kyc_status VARCHAR(32) NOT NULL,

    -- Module 1 Fine-Grained Document Management & Storage (byte[] maps directly to BYTEA)
                                         pan_file_name VARCHAR(255),
                                         pan_status VARCHAR(32),
                                         gst_file_name VARCHAR(255),
                                         gst_status VARCHAR(32),
                                         pan_data BYTEA,
                                         gst_data BYTEA
);

-- Maps directly to MerchantDocument.java
CREATE TABLE IF NOT EXISTS merchant_documents (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
                                                  document_type VARCHAR(32) NOT NULL,
                                                  file_path VARCHAR(255) NOT NULL,
                                                  status VARCHAR(32) NOT NULL,
                                                  uploaded_at TIMESTAMP NOT NULL
);

-- Explicitly matches the @Index metadata definition rules inside MerchantDocument.java
CREATE INDEX IF NOT EXISTS idx_doc_lookup ON merchant_documents(merchant_id, document_type);

-- Maps directly to MerchantImpersonation.java
CREATE TABLE IF NOT EXISTS merchant_impersonations (
                                                       id BIGSERIAL PRIMARY KEY,
                                                       admin_username VARCHAR(255) NOT NULL,
                                                       target_merchant_id VARCHAR(255) NOT NULL, -- Keep as String/VARCHAR per Java type specs
                                                       reason VARCHAR(500) NOT NULL,
                                                       active BOOLEAN NOT NULL,
                                                       created_at TIMESTAMP NOT NULL,
                                                       expires_at TIMESTAMP NOT NULL
);

-- Explicitly matches the @Index metadata definition rules inside MerchantImpersonation.java
CREATE INDEX IF NOT EXISTS idx_active_impersonations ON merchant_impersonations(admin_username, active);

-- Maps directly to Transaction.java
CREATE TABLE IF NOT EXISTS transactions (
                                            id VARCHAR(255) PRIMARY KEY, -- String Identifier matching gateway requirements
                                            merchant_id BIGINT,         -- Kept as Long/BIGINT type to match Transaction.java field specs
                                            amount NUMERIC(15, 2) NOT NULL,
                                            status VARCHAR(255) NOT NULL,
                                            payment_mode VARCHAR(255) NOT NULL,
                                            created_at TIMESTAMP NOT NULL
);

-- Composite high-performance query index for sorting/paginating transactions
CREATE INDEX IF NOT EXISTS idx_tx_merchant_pagination ON transactions(merchant_id, created_at DESC, status);


-- =========================================================================
-- 3. DISTRIBUTED CONCURRENCY LOCKS & AUDIT LOGGING
-- =========================================================================

-- Maps directly to IdempotentRequest.java
CREATE TABLE IF NOT EXISTS idempotent_requests (
                                                   idempotency_key VARCHAR(128) PRIMARY KEY,
                                                   request_path VARCHAR(255) NOT NULL,
                                                   response_body TEXT,
                                                   status VARCHAR(20) NOT NULL,
                                                   created_at TIMESTAMP NOT NULL
);

-- Explicitly matches the @Index metadata definition rules inside IdempotentRequest.java
CREATE INDEX IF NOT EXISTS idx_idempotency_lookup ON idempotent_requests(idempotency_key, status);

-- Maps directly to AdminAuditLog.java
CREATE TABLE IF NOT EXISTS admin_audit_logs (
                                                id BIGSERIAL PRIMARY KEY,
                                                admin_username VARCHAR(255) NOT NULL,
                                                action VARCHAR(255) NOT NULL,
                                                ip_address VARCHAR(45) NOT NULL,
                                                timestamp TIMESTAMP NOT NULL,
                                                pre_state_json TEXT,
                                                post_state_json TEXT
);