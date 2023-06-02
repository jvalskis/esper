CREATE TABLE IF NOT EXISTS firmware (
    "manufacturer" char(255) NOT NULL,
    "model" char(255) NOT NULL,
    "version" char(255) NOT NULL,
    "data" bytea NOT NULL,
    "size" INT NOT NULL
);