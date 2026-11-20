ALTER TABLE clientes ADD COLUMN IF NOT EXISTS tipo_gestion_comercial VARCHAR(30) NOT NULL DEFAULT 'SIN_CLASIFICAR';
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS responsable_comercial VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_clientes_gestion_comercial
    ON clientes(tenant_id, tipo_gestion_comercial);
CREATE INDEX IF NOT EXISTS idx_clientes_responsable_comercial
    ON clientes(tenant_id, responsable_comercial);
