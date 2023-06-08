CREATE TABLE IF NOT EXISTS firmware (
    "manufacturer" varchar(255) NOT NULL,
    "model" varchar(255) NOT NULL,
    "version" varchar(255) NOT NULL,
    "data" bytea NOT NULL,
    "size" INT NOT NULL,
    UNIQUE ("manufacturer", "model", "version")
);