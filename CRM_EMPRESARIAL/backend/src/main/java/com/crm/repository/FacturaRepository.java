/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.repository;

import com.crm.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    List<Factura> findByTenantId(Long tenantId);
    List<Factura> findTop500ByTenantIdOrderByFechaEmisionDescIdDesc(Long tenantId);
    long countByTenantId(Long tenantId);
    @Query("select coalesce(sum(f.total), 0), " +
            "coalesce(sum(case when f.estado in ('PENDIENTE','VENCIDA') then f.total else 0 end), 0), " +
            "coalesce(sum(case when f.estado = 'PAGADA' then f.total else 0 end), 0) " +
            "from Factura f where f.tenantId = ?1")
    Object[] summarizeByTenantId(Long tenantId);
    Optional<Factura> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Factura> findByTenantIdAndNumero(Long tenantId, String numero);
    Optional<Factura> findByTenantIdAndFuenteAndNumero(Long tenantId, String fuente, String numero);
    List<Factura> findByTenantIdAndFuente(Long tenantId, String fuente);
    List<Factura> findByTenantIdAndEstado(Long tenantId, String estado);
    List<Factura> findByTenantIdAndClienteId(Long tenantId, Long clienteId);
    List<Factura> findByTenantIdAndFechaVencimientoBefore(Long tenantId, LocalDate fecha);
    
    List<Factura> findByEstado(String estado);
    List<Factura> findByTipo(String tipo);
    List<Factura> findByClienteId(Long clienteId);
    List<Factura> findByVentaId(Long ventaId);
    List<Factura> findByFechaEmisionBetween(LocalDate inicio, LocalDate fin);
    List<Factura> findByFechaVencimientoBefore(LocalDate fecha);
    Optional<Factura> findByNumero(String numero);
}
