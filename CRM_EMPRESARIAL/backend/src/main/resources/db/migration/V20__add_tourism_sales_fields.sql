ALTER TABLE ventas ADD COLUMN IF NOT EXISTS tipo_producto_turistico VARCHAR(30);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS proveedor VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS aerolinea VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS hotel VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS origen VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS destino VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS localizador VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS canal_venta VARCHAR(100);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fecha_viaje_inicio DATE;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fecha_viaje_fin DATE;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS numero_pasajeros INT;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS moneda VARCHAR(3) DEFAULT 'COP';
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS costo DECIMAL(15,2);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS external_id VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS backoffice_origen VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_ventas_tenant_fecha_cierre ON ventas(tenant_id, fecha_cierre);
CREATE INDEX IF NOT EXISTS idx_ventas_tenant_producto ON ventas(tenant_id, tipo_producto_turistico);
CREATE INDEX IF NOT EXISTS idx_ventas_tenant_proveedor ON ventas(tenant_id, proveedor);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ventas_tenant_backoffice
    ON ventas(tenant_id, backoffice_origen, external_id);
