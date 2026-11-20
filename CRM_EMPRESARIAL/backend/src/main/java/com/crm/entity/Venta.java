/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @NotNull(message = "El cliente es obligatorio")
    @JsonIgnoreProperties({"ventas", "cotizaciones", "pedidos", "servicios", "hibernateLazyInitializer", "handler"})
    private Cliente cliente;
    
    @Column(nullable = false)
    private String codigo;
    
    @Column(nullable = false)
    private String descripcion;
    
    @NotNull(message = "El monto es obligatorio")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal descuento;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal impuesto;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal total;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal comision;
    
    private String vendedor;
    
    @Column(columnDefinition = "TEXT")
    private String notas;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estado = EstadoVenta.PENDIENTE;
    
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago = MetodoPago.TRANSFERENCIA;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;
    
    private LocalDateTime fechaCierre;
    
    private Long cotizacionId;

    @Transient
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto_turistico")
    private TipoProductoTuristico tipoProductoTuristico = TipoProductoTuristico.OTRO;

    private String proveedor;
    private String aerolinea;
    private String hotel;
    private String origen;
    private String destino;
    private String localizador;

    @Column(name = "canal_venta")
    private String canalVenta;

    @Column(name = "fecha_viaje_inicio")
    private LocalDate fechaViajeInicio;

    @Column(name = "fecha_viaje_fin")
    private LocalDate fechaViajeFin;

    @Column(name = "numero_pasajeros")
    private Integer numeroPasajeros;

    @Column(length = 3)
    private String moneda = "COP";

    @Column(precision = 15, scale = 2)
    private BigDecimal costo;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "backoffice_origen")
    private String backofficeOrigen;

    @Column(name = "fecha_venta_backoffice")
    private LocalDate fechaVentaBackoffice;

    @Column(name = "fecha_expedicion")
    private LocalDate fechaExpedicion;

    private String pasajero;

    @Column(name = "clase_tarifaria")
    private String claseTarifaria;

    @Column(precision = 15, scale = 2)
    private BigDecimal tarifa;

    @Column(precision = 15, scale = 2)
    private BigDecimal combustible;

    @Column(precision = 15, scale = 2)
    private BigDecimal otros;

    private String nacionalidad;
    private String tiqueteador;

    @Column(name = "entidad_vendedora")
    private String entidadVendedora;

    @Column(name = "entidad_distribuidora")
    private String entidadDistribuidora;

    @Column(columnDefinition = "TEXT")
    private String vuelo;

    @Column(name = "fare_basis")
    private String fareBasis;
    
    public enum EstadoVenta {
        PENDIENTE, EN_PROCESO, CERRADA, CANCELADA
    }
    
    public enum MetodoPago {
        EFECTIVO, TARJETA, TRANSFERENCIA, CHEQUE
    }

    public enum TipoProductoTuristico {
        VUELO, HOTEL, PAQUETE, CRUCERO, TRASLADO, EXCURSION, SEGURO, OTRO
    }
}
