/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.service;

import com.crm.entity.Cliente;
import com.crm.entity.Venta;
import com.crm.repository.ClienteRepository;
import com.crm.repository.VentaRepository;
import com.crm.security.TenantAccessDeniedException;
import com.crm.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class VentaService {
    
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    
    public List<Venta> findAll() {
        return ventaRepository.findByTenantId(tenantId());
    }

    public List<Venta> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return ventaRepository.findByTenantIdOrderByFechaVentaBackofficeDesc(tenantId(), PageRequest.of(0, safeLimit));
    }
    
    public Optional<Venta> findById(Long id) {
        return ventaRepository.findByIdAndTenantId(id, tenantId());
    }
    
    public Venta save(Venta venta) {
        Long tenantId = tenantId();
        Cliente cliente = clienteRepository.findByIdAndTenantId(clienteId(venta), tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Cliente"));
        
        venta.setCliente(cliente);
        venta.setTenantId(tenantId);
        String airline = venta.getAerolinea() != null && !venta.getAerolinea().isBlank()
                ? venta.getAerolinea()
                : venta.getProveedor();
        venta.setAerolinea(airline);
        venta.setProveedor(airline);
        
        if (venta.getTotal() == null) {
            BigDecimal total = venta.getMonto();
            if (venta.getDescuento() != null) {
                total = total.subtract(venta.getDescuento());
            }
            if (venta.getImpuesto() != null) {
                total = total.add(venta.getImpuesto());
            }
            venta.setTotal(total);
        }
        
        return ventaRepository.save(venta);
    }
    
    public Venta update(Long id, Venta venta) {
        Long tenantId = tenantId();
        Venta existingVenta = ventaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Venta"));
        
        Cliente cliente = clienteRepository.findByIdAndTenantId(clienteId(venta), tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException("Cliente"));
        
        existingVenta.setCliente(cliente);
        existingVenta.setCodigo(venta.getCodigo());
        existingVenta.setDescripcion(venta.getDescripcion());
        existingVenta.setMonto(venta.getMonto());
        existingVenta.setDescuento(venta.getDescuento());
        existingVenta.setImpuesto(venta.getImpuesto());
        existingVenta.setComision(venta.getComision());
        existingVenta.setVendedor(venta.getVendedor());
        existingVenta.setNotas(venta.getNotas());
        existingVenta.setEstado(venta.getEstado());
        existingVenta.setMetodoPago(venta.getMetodoPago());
        copyTourismFields(venta, existingVenta);
        
        BigDecimal total = venta.getMonto();
        if (venta.getDescuento() != null) {
            total = total.subtract(venta.getDescuento());
        }
        if (venta.getImpuesto() != null) {
            total = total.add(venta.getImpuesto());
        }
        existingVenta.setTotal(total);
        
        if (venta.getEstado() == Venta.EstadoVenta.CERRADA && existingVenta.getFechaCierre() == null) {
            existingVenta.setFechaCierre(LocalDateTime.now());
        }
        
        return ventaRepository.save(existingVenta);
    }
    
    public void delete(Long id) {
        Venta venta = ventaRepository.findByIdAndTenantId(id, tenantId())
                .orElseThrow(() -> new TenantAccessDeniedException("Venta"));
        ventaRepository.delete(venta);
    }
    
    public List<Venta> findByClienteId(Long clienteId) {
        return ventaRepository.findByTenantIdAndClienteId(tenantId(), clienteId);
    }
    
    public List<Venta> findByEstado(Venta.EstadoVenta estado) {
        return ventaRepository.findByTenantIdAndEstado(tenantId(), estado);
    }
    
    public List<Venta> findByVendedor(String vendedor) {
        return ventaRepository.findByTenantIdAndVendedor(tenantId(), vendedor);
    }
    
    public Venta cerrarVenta(Long id) {
        Venta venta = ventaRepository.findByIdAndTenantId(id, tenantId())
                .orElseThrow(() -> new TenantAccessDeniedException("Venta"));
        venta.setEstado(Venta.EstadoVenta.CERRADA);
        venta.setFechaCierre(LocalDateTime.now());
        return ventaRepository.save(venta);
    }
    
    public Double getTotalVentasCerradas() {
        return ventaRepository.sumTotalVentasCerradasByTenantId(tenantId());
    }

    private Long tenantId() { return TenantContext.requireCurrentTenant(); }

    private Long clienteId(Venta venta) {
        if (venta.getClienteId() != null) return venta.getClienteId();
        if (venta.getCliente() != null && venta.getCliente().getId() != null) return venta.getCliente().getId();
        throw new IllegalArgumentException("El cliente es obligatorio");
    }

    private void copyTourismFields(Venta source, Venta target) {
        target.setTipoProductoTuristico(source.getTipoProductoTuristico());
        String airline = source.getAerolinea() != null && !source.getAerolinea().isBlank()
                ? source.getAerolinea()
                : source.getProveedor();
        target.setProveedor(airline);
        target.setAerolinea(airline);
        target.setHotel(source.getHotel());
        target.setOrigen(source.getOrigen());
        target.setDestino(source.getDestino());
        target.setLocalizador(source.getLocalizador());
        target.setCanalVenta(source.getCanalVenta());
        target.setFechaViajeInicio(source.getFechaViajeInicio());
        target.setFechaViajeFin(source.getFechaViajeFin());
        target.setNumeroPasajeros(source.getNumeroPasajeros());
        target.setMoneda(source.getMoneda());
        target.setCosto(source.getCosto());
        target.setExternalId(source.getExternalId());
        target.setBackofficeOrigen(source.getBackofficeOrigen());
        target.setFechaVentaBackoffice(source.getFechaVentaBackoffice());
        target.setFechaExpedicion(source.getFechaExpedicion());
        target.setPasajero(source.getPasajero());
        target.setClaseTarifaria(source.getClaseTarifaria());
        target.setTarifa(source.getTarifa());
        target.setCombustible(source.getCombustible());
        target.setOtros(source.getOtros());
        target.setNacionalidad(source.getNacionalidad());
        target.setTiqueteador(source.getTiqueteador());
        target.setEntidadVendedora(source.getEntidadVendedora());
        target.setEntidadDistribuidora(source.getEntidadDistribuidora());
        target.setVuelo(source.getVuelo());
        target.setFareBasis(source.getFareBasis());
    }
}
