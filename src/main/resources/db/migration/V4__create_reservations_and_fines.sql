CREATE TABLE reservations (id BIGSERIAL PRIMARY KEY,member_id BIGINT NOT NULL REFERENCES users(id),book_id BIGINT NOT NULL REFERENCES books(id),queue_position INT NOT NULL CHECK (queue_position >= 1),status VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING','NOTIFIED','FULFILLED','CANCELLED','EXPIRED')),reserved_at TIMESTAMP NOT NULL DEFAULT now(),notified_at TIMESTAMP,expires_at DATE);
CREATE INDEX idx_res_member_id ON reservations(member_id);
CREATE INDEX idx_res_book_status ON reservations(book_id,status);
CREATE TABLE fine_records (id BIGSERIAL PRIMARY KEY,loan_id BIGINT NOT NULL UNIQUE REFERENCES loans(id),member_id BIGINT NOT NULL REFERENCES users(id),amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID','PAID','WAIVED')),paid_at TIMESTAMP,collected_by BIGINT REFERENCES users(id),created_at TIMESTAMP NOT NULL DEFAULT now());
CREATE INDEX idx_fines_member_id ON fine_records(member_id);
CREATE INDEX idx_fines_status ON fine_records(status);
