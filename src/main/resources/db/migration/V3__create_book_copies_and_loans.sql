CREATE TABLE book_copies (id BIGSERIAL PRIMARY KEY,book_id BIGINT NOT NULL REFERENCES books(id),copy_number VARCHAR(30) NOT NULL UNIQUE,condition VARCHAR(20) NOT NULL CHECK (condition IN ('NEW','GOOD','WORN','DAMAGED')),status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','ON_LOAN','RESERVED','UNDER_REPAIR')),added_at TIMESTAMP NOT NULL DEFAULT now());
CREATE INDEX idx_copies_book_id ON book_copies(book_id);
CREATE INDEX idx_copies_status ON book_copies(status);
CREATE TABLE loans (id BIGSERIAL PRIMARY KEY,member_id BIGINT NOT NULL REFERENCES users(id),book_copy_id BIGINT NOT NULL REFERENCES book_copies(id),processed_by BIGINT REFERENCES users(id),issued_at TIMESTAMP NOT NULL DEFAULT now(),due_date DATE NOT NULL,returned_at TIMESTAMP,status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RETURNED','OVERDUE')));
CREATE INDEX idx_loans_member_id ON loans(member_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_due_date ON loans(due_date);
