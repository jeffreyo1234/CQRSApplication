CREATE TABLE user_projections (
    user_id BIGINT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    user_email VARCHAR(100) NOT NULL UNIQUE,
    version BIGINT NOT NULL
);
