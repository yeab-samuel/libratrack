INSERT INTO book_copies (book_id, copy_number, condition, status, added_at)
SELECT id, CONCAT(isbn, '-001'), 'GOOD', 'AVAILABLE', NOW()
FROM books
ON CONFLICT DO NOTHING;