CREATE TABLE books (id BIGSERIAL PRIMARY KEY,isbn VARCHAR(20) NOT NULL UNIQUE,title VARCHAR(300) NOT NULL,author VARCHAR(200) NOT NULL,category VARCHAR(30) NOT NULL CHECK (category IN ('SCIENCE','ENGINEERING','HUMANITIES','LAW','MEDICINE','OTHER')),publisher VARCHAR(200),published_year INT CHECK (published_year BETWEEN 1000 AND 2100),total_copies INT NOT NULL CHECK (total_copies >= 1),description TEXT,created_at TIMESTAMP NOT NULL DEFAULT now());
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_category ON books(category);
CREATE INDEX idx_books_author ON books(author);
