-- SEC: tabela de auditoria — registra toda ação sensível com actor, ação e recurso afetado
--      sem UPDATE ou DELETE permitidos nesta tabela (enforçado via política de banco em produção)
CREATE TABLE audit_logs (
    id            BIGSERIAL    PRIMARY KEY,
    actor         VARCHAR(255) NOT NULL,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id   VARCHAR(255),
    occurred_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor ON audit_logs (actor);
CREATE INDEX idx_audit_occurred_at ON audit_logs (occurred_at DESC);
