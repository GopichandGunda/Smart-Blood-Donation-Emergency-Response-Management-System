CREATE DATABASE IF NOT EXISTS emergency_blood_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE emergency_blood_management;
SET default_storage_engine = InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(24) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'BLOOD_BANK_STAFF', 'DONOR'))
);

CREATE TABLE IF NOT EXISTS donors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL UNIQUE,
    full_name VARCHAR(140) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(30) NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    phone VARCHAR(24) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    last_donation_date DATE NULL,
    total_donations INT NOT NULL DEFAULT 0,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    eligible BOOLEAN NOT NULL DEFAULT FALSE,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_donor_age CHECK (age BETWEEN 18 AND 100),
    CONSTRAINT chk_donor_blood_group CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    CONSTRAINT chk_donor_status CHECK (account_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    INDEX idx_donor_match (blood_group, city, available, eligible, account_status)
);

CREATE TABLE IF NOT EXISTS hospitals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    phone VARCHAR(24) NOT NULL,
    email VARCHAR(254) NOT NULL,
    emergency_contact VARCHAR(24) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hospital_city (city, active)
);

CREATE TABLE IF NOT EXISTS blood_banks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    phone VARCHAR(24) NOT NULL,
    email VARCHAR(254) NOT NULL,
    storage_capacity INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_blood_bank_capacity CHECK (storage_capacity > 0)
);

CREATE TABLE IF NOT EXISTS emergency_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hospital_id BIGINT NOT NULL,
    patient_name VARCHAR(140) NOT NULL,
    patient_id VARCHAR(80) NOT NULL,
    blood_group_required VARCHAR(3) NOT NULL,
    units_required INT NOT NULL,
    emergency_level VARCHAR(16) NOT NULL,
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    required_date DATE NOT NULL,
    required_time TIME NOT NULL,
    hospital_location VARCHAR(255) NOT NULL,
    contact_number VARCHAR(24) NOT NULL,
    request_status VARCHAR(24) NOT NULL DEFAULT 'CREATED',
    assigned_donor_id BIGINT NULL,
    approved_by BIGINT NULL,
    CONSTRAINT fk_request_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_request_donor FOREIGN KEY (assigned_donor_id) REFERENCES donors(id) ON DELETE SET NULL,
    CONSTRAINT fk_request_approver FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_request_group CHECK (blood_group_required IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    CONSTRAINT chk_request_units CHECK (units_required > 0),
    CONSTRAINT chk_request_level CHECK (emergency_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_request_status CHECK (request_status IN ('CREATED', 'UNDER_REVIEW', 'MATCHING', 'DONOR_FOUND', 'BLOOD_RESERVED', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    INDEX idx_request_queue (request_status, emergency_level, required_date)
);

CREATE TABLE IF NOT EXISTS blood_units (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    blood_group VARCHAR(3) NOT NULL,
    collection_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    quantity INT NOT NULL,
    storage_location VARCHAR(120) NOT NULL,
    blood_bank_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    reserved_for_request_id BIGINT NULL,
    CONSTRAINT fk_unit_bank FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(id),
    CONSTRAINT fk_unit_request FOREIGN KEY (reserved_for_request_id) REFERENCES emergency_requests(id) ON DELETE SET NULL,
    CONSTRAINT chk_unit_group CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    CONSTRAINT chk_unit_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_unit_dates CHECK (expiry_date > collection_date),
    CONSTRAINT chk_unit_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'ISSUED', 'EXPIRED', 'DISCARDED')),
    INDEX idx_stock (blood_bank_id, blood_group, status, expiry_date)
);

CREATE TABLE IF NOT EXISTS donations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    donor_id BIGINT NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    donation_date DATE NOT NULL,
    units INT NOT NULL,
    blood_bank_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    verification_status VARCHAR(16) NOT NULL DEFAULT 'VERIFIED',
    CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) REFERENCES donors(id),
    CONSTRAINT fk_donation_bank FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(id),
    CONSTRAINT fk_donation_staff FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT chk_donation_units CHECK (units > 0),
    CONSTRAINT chk_donation_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    INDEX idx_donation_donor_date (donor_id, donation_date)
);

CREATE TABLE IF NOT EXISTS donor_matches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    emergency_request_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    matching_score INT NOT NULL,
    matched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted BOOLEAN NULL,
    CONSTRAINT fk_match_request FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_donor FOREIGN KEY (donor_id) REFERENCES donors(id),
    CONSTRAINT uq_request_donor UNIQUE (emergency_request_id, donor_id),
    CONSTRAINT chk_match_score CHECK (matching_score BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS blood_transfers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_blood_bank_id BIGINT NOT NULL,
    destination_blood_bank_id BIGINT NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    units INT NOT NULL,
    request_date DATE NOT NULL,
    approval_status VARCHAR(16) NOT NULL DEFAULT 'REQUESTED',
    transfer_date DATE NULL,
    staff_id BIGINT NOT NULL,
    CONSTRAINT fk_transfer_source FOREIGN KEY (source_blood_bank_id) REFERENCES blood_banks(id),
    CONSTRAINT fk_transfer_destination FOREIGN KEY (destination_blood_bank_id) REFERENCES blood_banks(id),
    CONSTRAINT fk_transfer_staff FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT chk_transfer_units CHECK (units > 0),
    CONSTRAINT chk_transfer_status CHECK (approval_status IN ('REQUESTED', 'APPROVED', 'IN_TRANSIT', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT chk_transfer_different_banks CHECK (source_blood_bank_id <> destination_blood_bank_id)
);

CREATE TABLE IF NOT EXISTS blood_transfer_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_id BIGINT NOT NULL,
    blood_group VARCHAR(3) NOT NULL,
    quantity INT NOT NULL,
    collection_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    storage_location VARCHAR(120) NOT NULL,
    CONSTRAINT fk_transfer_item_transfer FOREIGN KEY (transfer_id) REFERENCES blood_transfers(id) ON DELETE CASCADE,
    CONSTRAINT chk_transfer_item_quantity CHECK (quantity > 0),
    INDEX idx_transfer_item (transfer_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notification_user (user_id, is_read, created_at)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500) NOT NULL,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_created (created_at)
);
