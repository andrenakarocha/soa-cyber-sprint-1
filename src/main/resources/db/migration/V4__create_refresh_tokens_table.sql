-- SEC: refresh tokens persistidos para permitir revogação individual e rotação obrigatória
--      token_hash armazena SHA-256 do token real — o valor original nunca fica no banco
CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    subject     VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    -- SEC: flag de revogação para invalidação imediata em caso de logout ou comprometimento
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_subject ON refresh_tokens (subject);
-- SEC: índice no hash para busca rápida na validação — evita full scan em cada request de refresh
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);
