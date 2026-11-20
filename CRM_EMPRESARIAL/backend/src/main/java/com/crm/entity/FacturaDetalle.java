package com.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "factura_detalles", uniqueConstraints =
        @UniqueConstraint(name = "uk_factura_detalle_documento", columnNames = {"tenant_id", "factura_id", "documento"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(nullable = false)
    private String documento;

    private String pasajero;
    private String ruta;
    private String aerolinea;
    private String localizador;

    @Column(name = "fecha_viaje")
    private LocalDate fechaViaje;

    @Column(precision = 15, scale = 2)
    private BigDecimal tarifa;

    @Column(length = 3)
    private String moneda;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
