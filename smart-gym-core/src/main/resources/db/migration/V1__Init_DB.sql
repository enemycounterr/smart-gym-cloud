CREATE TABLE access_zone (
    id BIGSERIAL PRIMARY KEY,
    zone_name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE access_zone_clients (
    access_zone_id BIGINT NOT NULL,
    client_ids BIGINT,
    FOREIGN KEY (access_zone_id) REFERENCES access_zone(id) ON DELETE CASCADE
);

CREATE TABLE access_cards (
    id BIGSERIAL PRIMARY KEY,
    rfid_token VARCHAR(255) NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL,
    issued_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE access_logs (
    id BIGSERIAL PRIMARY KEY,
    direction VARCHAR(50) NOT NULL,
    client_id BIGINT NOT NULL,
    access_zone_id BIGINT NOT NULL,
    time_stamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    FOREIGN KEY (access_zone_id) REFERENCES access_zone(id)
);