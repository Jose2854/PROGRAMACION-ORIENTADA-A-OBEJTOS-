/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.repository;

import com.crm.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    
    @Query("SELECT v FROM Venta v WHERE v.cliente.id = :clienteId")
    List<Venta> findByClienteId(@Param("clienteId") Long clienteId);
    @EntityGraph(attributePaths = "cliente")
    List<Venta> findByTenantId(Long tenantId);
    @EntityGraph(attributePaths = "cliente")
    List<Venta> findByTenantIdOrderByFechaVentaBackofficeDesc(Long tenantId, Pageable pageable);
    @EntityGraph(attributePaths = "cliente")
    Optional<Venta> findByIdAndTenantId(Long id, Long tenantId);
    @Query("SELECT v FROM Venta v WHERE v.tenantId = :tenantId AND v.cliente.id = :clienteId")
    @EntityGraph(attributePaths = "cliente")
    List<Venta> findByTenantIdAndClienteId(@Param("tenantId") Long tenantId, @Param("clienteId") Long clienteId);
    @EntityGraph(attributePaths = "cliente")
    List<Venta> findByTenantIdAndEstado(Long tenantId, Venta.EstadoVenta estado);
    @EntityGraph(attributePaths = "cliente")
    List<Venta> findByTenantIdAndVendedor(Long tenantId, String vendedor);
    
    List<Venta> findByEstado(Venta.EstadoVenta estado);
    
    List<Venta> findByVendedor(String vendedor);
    
    @Query("SELECT v FROM Venta v WHERE v.fechaCreacion BETWEEN :fechaInicio AND :fechaFin")
    List<Venta> findByFechaCreacionBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                            @Param("fechaFin") LocalDateTime fechaFin);
    
    @Query("SELECT v FROM Venta v WHERE v.cliente.id = :clienteId AND v.estado = :estado")
    List<Venta> findByClienteIdAndEstado(@Param("clienteId") Long clienteId, 
                                         @Param("estado") Venta.EstadoVenta estado);
    
    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.estado = 'CERRADA'")
    Double sumTotalVentasCerradas();

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.tenantId = :tenantId AND v.estado = 'CERRADA'")
    Double sumTotalVentasCerradasByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT v.externalId FROM Venta v WHERE v.tenantId = :tenantId AND v.backofficeOrigen = :source AND v.externalId IS NOT NULL")
    Set<String> findExternalIdsByTenantIdAndBackofficeOrigen(@Param("tenantId") Long tenantId, @Param("source") String source);
    
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.estado = :estado")
    Long countByEstado(@Param("estado") Venta.EstadoVenta estado);
}
