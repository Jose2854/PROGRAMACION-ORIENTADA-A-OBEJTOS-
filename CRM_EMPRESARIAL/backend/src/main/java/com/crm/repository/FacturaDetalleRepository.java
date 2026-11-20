package com.crm.repository;

import com.crm.entity.FacturaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaDetalleRepository extends JpaRepository<FacturaDetalle, Long> {
    List<FacturaDetalle> findByTenantIdAndFacturaIdOrderByIdAsc(Long tenantId, Long facturaId);
    List<FacturaDetalle> findByTenantIdAndFacturaIdIn(Long tenantId, List<Long> facturaIds);
    void deleteByTenantIdAndFacturaId(Long tenantId, Long facturaId);
}
