-- Give the seeded admin account a staff ID so they can log in using their ID.
-- This migration is safe on both fresh installs (where V7 seeds the admin with
-- no university_id) and existing installs that already have an admin account.
-- The WHERE clause ensures it only runs when the column is still NULL.
UPDATE users
SET university_id = 'ADM/001/00'
WHERE email = 'admin@libratrack.com'
  AND university_id IS NULL;
