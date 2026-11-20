ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fecha_venta_backoffice DATE;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fecha_expedicion DATE;
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS pasajero VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS clase_tarifaria VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS tarifa DECIMAL(15,2);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS combustible DECIMAL(15,2);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS otros DECIMAL(15,2);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS nacionalidad VARCHAR(100);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS tiqueteador VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS entidad_vendedora VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS entidad_distribuidora VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS vuelo VARCHAR(255);
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS fare_basis VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_ventas_tenant_fecha_backoffice ON ventas(tenant_id, fecha_venta_backoffice);
CREATE INDEX IF NOT EXISTS idx_ventas_tenant_aerolinea ON ventas(tenant_id, aerolinea);
CREATE INDEX IF NOT EXISTS idx_ventas_tenant_destino ON ventas(tenant_id, destino);
