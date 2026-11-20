package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record TourismReportResponse(
        LocalDate from,
        LocalDate to,
        Metric totals,
        Map<String, Metric> ventasPorDia,
        Map<String, Metric> ventasPorCliente,
        Map<String, Metric> ventasPorTipoProducto,
        Map<String, Metric> ventasPorAerolinea,
        Map<String, Metric> ventasPorHotel,
        Map<String, Metric> ventasPorProveedor,
        Map<String, Metric> ventasPorDestino,
        Map<String, Metric> ventasPorCanal,
        Map<String, Metric> ventasPorMoneda
) {
    public record Metric(long ventas, int pasajeros, long ventasConCosto, BigDecimal ingreso, BigDecimal costo, BigDecimal margen) {}
}
