ALTER TABLE clientes ADD COLUMN IF NOT EXISTS estado_negociacion VARCHAR(30) NOT NULL DEFAULT 'SIN_NEGOCIACION';
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS tiene_cartera BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_clientes_negociacion ON clientes(tenant_id, estado_negociacion);
CREATE INDEX IF NOT EXISTS idx_clientes_cartera ON clientes(tenant_id, tiene_cartera);
