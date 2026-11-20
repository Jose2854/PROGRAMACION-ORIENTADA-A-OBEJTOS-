/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "facturas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Factura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    private String numero;

    @Column(name = "numero_zeuz")
    private String numeroZeuz;

    private String fuente;

    private String vendedor;

    @Column(name = "cantidad_items")
    private Integer cantidadItems;
    private String descripcion;
    private String tipo;
    private String estado;
    
    @Column(name = "cliente_id")
    private Long clienteId;
    
    @Column(name = "venta_id")
    private Long ventaId;
    
    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;
    
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    
    @Column(name = "fecha_pago")
    private LocalDate fechaPago;
    
    private BigDecimal subtotal;
    private BigDecimal impuesto;
    private BigDecimal total;
    
    private String moneda;
    
    @Column(name = "metodo_pago")
    private String metodoPago;
    
    @Column(name = "url_factura")
    private String urlFactura;
    
    private String notas;

    @Transient
    private String clienteNombre;

    @Transient
    private List<FacturaDetalle> detalles;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
