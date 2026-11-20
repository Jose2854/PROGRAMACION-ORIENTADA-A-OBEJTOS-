ALTER TABLE facturas ADD COLUMN IF NOT EXISTS numero_zeuz VARCHAR(100);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS fuente VARCHAR(50);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS vendedor VARCHAR(255);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS cantidad_items INTEGER DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_facturas_tenant_fuente_numero
    ON facturas(tenant_id, fuente, numero);

CREATE TABLE IF NOT EXISTS factura_detalles (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    factura_id BIGINT NOT NULL,
    venta_id BIGINT,
    documento VARCHAR(255) NOT NULL,
    pasajero VARCHAR(500),
    ruta VARCHAR(500),
    aerolinea VARCHAR(255),
    localizador VARCHAR(100),
    fecha_viaje DATE,
    tarifa DECIMAL(15,2),
    moneda VARCHAR(3),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_factura_detalle_documento UNIQUE (tenant_id, factura_id, documento)
);

CREATE INDEX IF NOT EXISTS idx_factura_detalles_factura
    ON factura_detalles(tenant_id, factura_id);
