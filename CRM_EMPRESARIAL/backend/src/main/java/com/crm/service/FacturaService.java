/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.service;

import com.crm.entity.Factura;
import com.crm.entity.Cliente;
import com.crm.repository.ClienteRepository;
import com.crm.repository.FacturaDetalleRepository;
import com.crm.repository.FacturaRepository;
import com.crm.security.TenantAccessDeniedException;
import com.crm.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FacturaService {
    
    private final FacturaRepository facturaRepository;
    private final FacturaDetalleRepository facturaDetalleRepository;
    private final ClienteRepository clienteRepository;
    
    public List<Factura> findAll() {
        Long tenantId = tenantId();
        List<Factura> facturas = facturaRepository.findTop500ByTenantIdOrderByFechaEmisionDescIdDesc(tenantId);
        Map<Long, String> clientNames = clienteRepository.findByTenantId(tenantId).stream()
                .filter(cliente -> cliente.getId() != null)
                .collect(Collectors.toMap(Cliente::getId, this::clientName, (first, ignored) -> first));
        facturas.forEach(factura -> factura.setClienteNombre(clientNames.get(factura.getClienteId())));
        return facturas;
    }

    public Map<String, Object> summary() {
        Long tenantId = tenantId();
        Object[] values = facturaRepository.summarizeByTenantId(tenantId);
        Object[] row = values.length == 1 && values[0] instanceof Object[] nested ? nested : values;
        return Map.of(
                "cantidad", facturaRepository.countByTenantId(tenantId),
                "total", row.length > 0 ? row[0] : BigDecimal.ZERO,
                "pendiente", row.length > 1 ? row[1] : BigDecimal.ZERO,
                "pagada", row.length > 2 ? row[2] : BigDecimal.ZERO);
    }
    
    public Optional<Factura> findById(Long id) {
        Long tenantId = tenantId();
        return facturaRepository.findByIdAndTenantId(id, tenantId)
                .map(factura -> {
                    enrichClient(factura);
                    factura.setDetalles(facturaDetalleRepository.findByTenantIdAndFacturaIdOrderByIdAsc(tenantId, id));
                    return factura;
                });
    }
    
    public Factura save(Factura factura) {
        factura.setTenantId(tenantId());
        if (factura.getTotal() == null && factura.getSubtotal() != null) {
            BigDecimal total = factura.getSubtotal();
            if (factura.getImpuesto() != null) {
                total = total.add(factura.getImpuesto());
            }
            factura.setTotal(total);
        }
        return facturaRepository.save(factura);
    }
    
    public Factura update(Long id, Factura factura) {
        Factura existing = facturaRepository.findByIdAndTenantId(id, tenantId())
                .orElseThrow(() -> new TenantAccessDeniedException("Factura"));
        factura.setId(existing.getId());
        factura.setTenantId(existing.getTenantId());
        return facturaRepository.save(factura);
    }
    
    public void delete(Long id) {
        Long tenantId = tenantId();
        Factura factura = facturaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Factura"));
        facturaDetalleRepository.deleteByTenantIdAndFacturaId(tenantId, id);
        facturaRepository.delete(factura);
    }
    
    public List<Factura> findByClienteId(Long clienteId) {
        return facturaRepository.findByTenantIdAndClienteId(tenantId(), clienteId);
    }
    
    public List<Factura> findByEstado(String estado) {
        return facturaRepository.findByTenantIdAndEstado(tenantId(), estado);
    }
    
    public List<Factura> findVencidas() {
        return facturaRepository.findByTenantIdAndFechaVencimientoBefore(tenantId(), LocalDate.now());
    }

    private Long tenantId() { return TenantContext.requireCurrentTenant(); }

    private void enrichClient(Factura factura) {
        if (factura.getClienteId() == null) return;
        clienteRepository.findByIdAndTenantId(factura.getClienteId(), factura.getTenantId())
                .ifPresent(cliente -> factura.setClienteNombre(clientName(cliente)));
    }

    private String clientName(Cliente cliente) {
        if (cliente.getEmpresa() != null && !cliente.getEmpresa().isBlank()) return cliente.getEmpresa();
        return ((cliente.getNombre() == null ? "" : cliente.getNombre()) + " "
                + (cliente.getApellido() == null ? "" : cliente.getApellido())).trim();
    }
}
