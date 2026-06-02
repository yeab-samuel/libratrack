-- Fix university_id constraint to allow multiple NULLs
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_student_id;
ALTER TABLE users RENAME COLUMN student_id TO university_id;
CREATE UNIQUE INDEX uq_users_university_id ON users(university_id)
    WHERE university_id IS NOT NULL AND university_id <> '';

-- University registry table for cross-checking registrations
CREATE TABLE university_registry (
    id          BIGSERIAL PRIMARY KEY,
    university_id VARCHAR(50) NOT NULL UNIQUE,
    full_name   VARCHAR(200) NOT NULL,
    role        VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'FACULTY')),
    department  VARCHAR(100),
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_registry_uid ON university_registry(university_id);
CREATE INDEX idx_registry_role ON university_registry(role);
