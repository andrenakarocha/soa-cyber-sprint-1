-- Tabela de veículos registrados na plataforma HENRY
-- VIN (Vehicle Identification Number) é a chave natural do setor automotivo
CREATE TABLE vehicles (
    vin         VARCHAR(17) PRIMARY KEY,
    model       VARCHAR(100) NOT NULL,
    year        INTEGER     NOT NULL,
    -- SEC: owner_id referencia o usuário autenticado — base para controle de acesso horizontal
    owner_id    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicles_owner_id ON vehicles (owner_id);
