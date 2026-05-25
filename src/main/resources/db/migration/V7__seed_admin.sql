INSERT INTO users (email, password_hash, role, full_name, active, created_at)
VALUES (
  'admin@libratrack.com',
  '$2a$12$a5OtfrN0GECYDWI3LS8OaOXzzuM4euT3XIDa6yp2HFSgC1m9obG/i',
  'ADMIN',
  'System Administrator',
  true,
  NOW()
)
ON CONFLICT (email) DO NOTHING;