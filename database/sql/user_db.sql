-- CREATE TABLE IF NOT EXISTS user (
--     id SERIAL PRIMARY KEY,
--     username VARCHAR(50) NOT NULL UNIQUE,
--     email VARCHAR(100) NOT NULL UNIQUE,
--     password VARCHAR(100) NOT NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );

-- CREATE TABLE IF NOT EXISTS roles (
--     id SERIAL PRIMARY KEY,
--     role_name VARCHAR(50) NOT NULL UNIQUE
-- );

-- CREATE TABLE IF NOT EXISTS user_roles (
--     user_id INT REFERENCES users(id),
--     role_id INT REFERENCES roles(id),
--     PRIMARY KEY (user_id, role_id)
-- );