-- Book rating table: one rating per member per book.
-- Members can only rate books they have returned (enforced at service layer).
-- The unique constraint allows upsert (update if re-rated).
CREATE TABLE book_ratings (
    id          BIGSERIAL PRIMARY KEY,
    book_id     BIGINT  NOT NULL REFERENCES books(id),
    member_id   BIGINT  NOT NULL REFERENCES users(id),
    rating      INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rating_book_member UNIQUE (book_id, member_id)
);

CREATE INDEX idx_ratings_book_id   ON book_ratings(book_id);
CREATE INDEX idx_ratings_member_id ON book_ratings(member_id);
