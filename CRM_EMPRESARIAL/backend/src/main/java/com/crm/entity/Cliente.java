/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clientes_tenant_email", columnNames = {"tenant_id", "email"}),
        @UniqueConstraint(name = "uk_clientes_tenant_identificacion", columnNames = {"tenant_id", "identificacion"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false)
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    @Column(nullable = false)
    private String apellido;
    
    @Email(message = "Email inválido")
    @Column
    private String email;
    
    @Column(length = 20)
    private String telefono;
    
    @Column(length = 20)
    private String celular;
    
    private String direccion;
    
    private String ciudad;
    
    private String pais;
    
    @Column(length = 10)
    private String codigoPostal;
    
    @Column(length = 20)
    private String identificacion;
    
    @Column(length = 20)
    private String tipoIdentificacion;
    
    private String empresa;
    
    private String cargo;
    
    private String sector;
    
    @Column(columnDefinition = "TEXT")
    private String notas;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCliente estado = EstadoCliente.ACTIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_negociacion")
    private EstadoNegociacion estadoNegociacion = EstadoNegociacion.SIN_NEGOCIACION;

    @Column(name = "tiene_cartera")
    private Boolean tieneCartera = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gestion_comercial")
    private TipoGestionComercial tipoGestionComercial = TipoGestionComercial.SIN_CLASIFICAR;

    @Column(name = "responsable_comercial")
    private String responsableComercial;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Venta> ventas = new ArrayList<>();
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Cotizacion> cotizaciones = new ArrayList<>();
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Pedido> pedidos = new ArrayList<>();
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ServicioCliente> servicios = new ArrayList<>();
    
    public enum EstadoCliente {
        ACTIVO, INACTIVO, BLOQUEADO
    }

    public enum EstadoNegociacion {
        SIN_NEGOCIACION, EN_NEGOCIACION, ACUERDO_ACTIVO
    }

    public enum TipoGestionComercial {
        AGENCIA, FREELANCE, SIN_CLASIFICAR
    }
}
