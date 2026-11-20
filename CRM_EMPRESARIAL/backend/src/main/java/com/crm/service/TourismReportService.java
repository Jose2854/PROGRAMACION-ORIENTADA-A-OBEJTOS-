package com.crm.service;

import com.crm.dto.TourismReportResponse;
import com.crm.entity.Venta;
import com.crm.repository.VentaRepository;
import com.crm.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourismReportService {

    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public TourismReportResponse report(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");

        List<Venta> sales = ventaRepository.findByTenantIdAndEstado(
                        TenantContext.requireCurrentTenant(), Venta.EstadoVenta.CERRADA).stream()
                .filter(sale -> saleDate(sale) != null)
                .filter(sale -> !saleDate(sale).isBefore(from) && !saleDate(sale).isAfter(to))
                .toList();

        return new TourismReportResponse(
                from,
                to,
                metric(sales),
                group(sales, sale -> saleDate(sale).toString(), true),
                group(sales, this::customerName, false),
                group(sales, sale -> sale.getTipoProductoTuristico() == null ? "OTRO" : sale.getTipoProductoTuristico().name(), false),
                group(sales, Venta::getAerolinea, false),
                group(sales, Venta::getHotel, false),
                group(sales, Venta::getProveedor, false),
                group(sales, Venta::getDestino, false),
                group(sales, Venta::getCanalVenta, false),
                group(sales, Venta::getMoneda, false)
        );
    }

    private Map<String, TourismReportResponse.Metric> group(
            List<Venta> sales, Function<Venta, String> classifier, boolean chronological) {
        Comparator<Map.Entry<String, List<Venta>>> comparator = chronological
                ? Map.Entry.comparingByKey()
                : Map.Entry.<String, List<Venta>>comparingByValue(
                        Comparator.comparing((List<Venta> values) -> income(values))).reversed();

        return sales.stream()
                .collect(Collectors.groupingBy(sale -> label(classifier.apply(sale))))
                .entrySet().stream()
                .sorted(comparator)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> metric(entry.getValue()),
                        (left, right) -> left, LinkedHashMap::new));
    }

    private TourismReportResponse.Metric metric(List<Venta> sales) {
        BigDecimal income = sales.stream().map(this::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Venta> salesWithCost = sales.stream().filter(sale -> sale.getCosto() != null).toList();
        BigDecimal cost = salesWithCost.stream().map(this::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal margin = salesWithCost.stream()
                .map(sale -> total(sale).subtract(cost(sale)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int passengers = sales.stream().mapToInt(sale -> sale.getNumeroPasajeros() == null ? 0 : sale.getNumeroPasajeros()).sum();
        return new TourismReportResponse.Metric(sales.size(), passengers, salesWithCost.size(), income, cost, margin);
    }

    private BigDecimal income(List<Venta> sales) {
        return sales.stream().map(this::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal total(Venta sale) {
        if (sale.getTarifa() != null) return sale.getTarifa();
        return sale.getTotal() == null ? BigDecimal.ZERO : sale.getTotal();
    }

    private BigDecimal cost(Venta sale) {
        return sale.getCosto() == null ? BigDecimal.ZERO : sale.getCosto();
    }

    private LocalDate saleDate(Venta sale) {
        if (sale.getFechaVentaBackoffice() != null) return sale.getFechaVentaBackoffice();
        if (sale.getFechaCierre() != null) return sale.getFechaCierre().toLocalDate();
        return sale.getFechaCreacion() == null ? null : sale.getFechaCreacion().toLocalDate();
    }

    private String customerName(Venta sale) {
        if (sale.getCliente() == null) return "Sin cliente";
        String lastName = "-".equals(sale.getCliente().getApellido()) ? "" : sale.getCliente().getApellido();
        String fullName = (sale.getCliente().getNombre() + " " + lastName).trim();
        return fullName.isBlank() ? "Sin cliente" : fullName;
    }

    private String label(String value) {
        return value == null || value.isBlank() ? "Sin especificar" : value.trim();
    }
}
