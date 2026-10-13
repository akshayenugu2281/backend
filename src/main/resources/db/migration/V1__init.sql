-- ========================================================
-- Library360 Database Schema - V1 Initial Migration (PostgreSQL)
-- ========================================================

-- ========== IDENTITY & ACCESS ==========
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_logins INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_user_email ON users (email);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    description VARCHAR(200)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    family_id CHAR(36) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    user_agent VARCHAR(255),
    ip_address VARCHAR(45),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_rt_family ON refresh_tokens (family_id);
CREATE INDEX idx_rt_hash ON refresh_tokens (token_hash);

-- ========== MEMBERS & POLICY ==========
CREATE TABLE loan_policies (
    id BIGSERIAL PRIMARY KEY,
    member_type VARCHAR(20) NOT NULL UNIQUE,
    max_loans INT NOT NULL,
    loan_days INT NOT NULL,
    max_renewals INT NOT NULL,
    max_reservations INT NOT NULL,
    fine_per_day DECIMAL(10,2) NOT NULL,
    fine_cap DECIMAL(10,2) NOT NULL,
    block_fine_threshold DECIMAL(10,2) NOT NULL,
    hold_hours INT NOT NULL DEFAULT 48,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    member_code VARCHAR(30) NOT NULL UNIQUE,
    member_type VARCHAR(20) NOT NULL,
    department VARCHAR(100),
    phone VARCHAR(30),
    address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    valid_until DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_member_code ON members (member_code);
CREATE INDEX idx_member_status ON members (status);

-- ========== CATALOG ==========
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    parent_id BIGINT NULL,
    description VARCHAR(300),
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);
CREATE INDEX idx_cat_slug ON categories (slug);

CREATE TABLE publishers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    website VARCHAR(300),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE authors (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    bio TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_author_name ON authors (full_name);

CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    isbn13 CHAR(13) UNIQUE,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    edition VARCHAR(40),
    publish_year SMALLINT,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    pages INT,
    description TEXT,
    cover_url VARCHAR(500),
    category_id BIGINT,
    publisher_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(160),
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (publisher_id) REFERENCES publishers(id)
);
CREATE INDEX idx_book_isbn ON books (isbn13);
CREATE INDEX idx_book_title ON books (title);

CREATE TABLE book_authors (
    book_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(id)
);

CREATE TABLE book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    barcode VARCHAR(40) NOT NULL UNIQUE,
    shelf VARCHAR(40),
    "condition" VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    acquired_on DATE,
    notes VARCHAR(300),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (book_id) REFERENCES books(id)
);
CREATE INDEX idx_copy_barcode ON book_copies (barcode);
CREATE INDEX idx_copy_book_status ON book_copies (book_id, status);

-- ========== CIRCULATION ==========
CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    copy_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    due_at TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    renew_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    issued_by BIGINT NOT NULL,
    returned_by BIGINT,
    notes VARCHAR(300),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (copy_id) REFERENCES book_copies(id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (issued_by) REFERENCES users(id)
);
CREATE INDEX idx_loan_member_status ON loans (member_id, status);
CREATE INDEX idx_loan_due ON loans (status, due_at);
CREATE INDEX idx_loan_copy ON loans (copy_id, status);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    queued_at TIMESTAMP NOT NULL,
    ready_at TIMESTAMP,
    expires_at TIMESTAMP,
    assigned_copy_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (assigned_copy_id) REFERENCES book_copies(id)
);
CREATE INDEX idx_res_queue ON reservations (book_id, status, queued_at);
CREATE INDEX idx_res_member ON reservations (member_id, status);
CREATE INDEX idx_res_expiry ON reservations (status, expires_at);

-- ========== FINES ==========
CREATE TABLE fines (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT,
    member_id BIGINT NOT NULL,
    reason VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    waive_note VARCHAR(255),
    waived_by BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT uq_fine_loan_reason UNIQUE (loan_id, reason)
);
CREATE INDEX idx_fine_member_status ON fines (member_id, status);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    fine_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    receipt_no VARCHAR(30) NOT NULL UNIQUE,
    collected_by BIGINT NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    FOREIGN KEY (fine_id) REFERENCES fines(id),
    FOREIGN KEY (collected_by) REFERENCES users(id)
);
CREATE INDEX idx_payment_fine ON payments (fine_id);

-- ========== ACTIVITY & NOTIFICATIONS ==========
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT,
    actor_email VARCHAR(160),
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(40),
    details JSON,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_time ON audit_logs (created_at);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(150) NOT NULL,
    body VARCHAR(500),
    link VARCHAR(255),
    "read" BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_notif_user ON notifications (user_id, "read");
CREATE INDEX idx_notif_created ON notifications (created_at);
