ALTER TABLE users ADD COLUMN role character varying(255) DEFAULT 'USER' NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
