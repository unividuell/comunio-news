CREATE TABLE lineups
(
    id      INT PRIMARY KEY,
    version INT,
    hash    CHARACTER(64),
    json    JSON NOT NULL
);