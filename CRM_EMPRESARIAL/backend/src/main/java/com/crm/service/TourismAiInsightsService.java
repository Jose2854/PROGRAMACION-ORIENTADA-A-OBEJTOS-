package com.crm.service;

import com.crm.dto.TourismAiInsightsResponse;
import com.crm.entity.Cliente;
import com.crm.entity.Venta;
import com.crm.repository.VentaRepository;
import com.crm.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourismAiInsightsService {

    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public TourismAiInsightsResponse insights(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");

        List<Venta> availableSales = ventaRepository.findByTenantIdAndEstado(
                        TenantContext.requireCurrentTenant(), Venta.EstadoVenta.CERRADA).stream()
                .filter(sale -> saleDate(sale) != null)
                .toList();
        LocalDate availableFrom = availableSales.stream().map(this::saleDate).min(LocalDate::compareTo).orElse(null);
        LocalDate availableTo = availableSales.stream().map(this::saleDate).max(LocalDate::compareTo).orElse(null);
        List<Venta> sales = availableSales.stream()
                .filter(sale -> !saleDate(sale).isBefore(from) && !saleDate(sale).isAfter(to))
                .toList();

        Map<Long, List<Venta>> byClient = sales.stream()
                .filter(sale -> sale.getCliente() != null)
                .collect(Collectors.groupingBy(sale -> sale.getCliente().getId()));
        BigDecimal maxRevenue = byClient.values().stream().map(this::revenue).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        long maxSales = byClient.values().stream().mapToLong(List::size).max().orElse(0);
        int maxAirlines = byClient.values().stream().mapToInt(this::airlineCount).max().orElse(0);

        List<TourismAiInsightsResponse.ClientInsight> allClients = byClient.entrySet().stream()
                .map(entry -> clientInsight(entry.getValue().get(0).getCliente(), entry.getValue(), to,
                        maxRevenue, maxSales, maxAirlines))
                .sorted(Comparator.comparingInt(TourismAiInsightsResponse.ClientInsight::potentialScore).reversed()
                        .thenComparing(TourismAiInsightsResponse.ClientInsight::revenue, Comparator.reverseOrder()))
                .toList();
        List<TourismAiInsightsResponse.ClientInsight> clients = allClients.stream().limit(20).toList();

        BigDecimal totalRevenue = revenue(sales);
        List<TourismAiInsightsResponse.AirlineInsight> airlines = sales.stream()
                .collect(Collectors.groupingBy(sale -> label(sale.getAerolinea())))
                .entrySet().stream()
                .map(entry -> airlineInsight(entry.getKey(), entry.getValue(), totalRevenue))
                .sorted(Comparator.comparing(TourismAiInsightsResponse.AirlineInsight::revenue).reversed())
                .limit(15)
                .toList();

        TourismAiInsightsResponse.ClientInsight topRevenueClient = allClients.stream()
                .max(Comparator.comparing(TourismAiInsightsResponse.ClientInsight::revenue)).orElse(null);
        TourismAiInsightsResponse.ClientInsight topPotential = allClients.isEmpty() ? null : allClients.get(0);
        TourismAiInsightsResponse.AirlineInsight topAirline = airlines.isEmpty() ? null : airlines.get(0);

        return new TourismAiInsightsResponse(
                from, to, availableFrom, availableTo, LocalDateTime.now(),
                "Puntaje explicable: 45% ingreso por tarifa, 30% frecuencia, 15% recencia y 10% diversidad de aerolíneas.",
                new TourismAiInsightsResponse.Summary(
                        topRevenueClient == null ? "Sin datos" : topRevenueClient.name(),
                        topRevenueClient == null ? BigDecimal.ZERO : topRevenueClient.revenue(),
                        topAirline == null ? "Sin datos" : topAirline.name(),
                        topAirline == null ? BigDecimal.ZERO : topAirline.revenue(),
                        topPotential == null ? "Sin datos" : topPotential.name(),
                        topPotential == null ? 0 : topPotential.potentialScore(),
                        allClients.stream().filter(client -> client.potentialScore() >= 60).count(),
                        allClients.stream().filter(client -> client.churnRiskScore() >= 60).count(),
                        topAirline == null ? 0 : topAirline.revenueShare()),
                clients, airlines);
    }

    private TourismAiInsightsResponse.ClientInsight clientInsight(
            Cliente client, List<Venta> sales, LocalDate referenceDate,
            BigDecimal maxRevenue, long maxSales, int maxAirlines) {
        BigDecimal income = revenue(sales);
        LocalDate lastSale = sales.stream().map(this::saleDate).max(LocalDate::compareTo).orElse(referenceDate);
        long days = Math.max(0, ChronoUnit.DAYS.between(lastSale, referenceDate));
        int airlines = airlineCount(sales);
        double revenueRatio = ratio(income, maxRevenue);
        double frequencyRatio = maxSales == 0 ? 0 : (double) sales.size() / maxSales;
        double recencyRatio = Math.max(0, 1 - (double) days / 180);
        double diversityRatio = maxAirlines == 0 ? 0 : (double) airlines / maxAirlines;
        int potential = clamp((int) Math.round(100 * (0.45 * revenueRatio + 0.30 * frequencyRatio
                + 0.15 * recencyRatio + 0.10 * diversityRatio)));
        int interaction = clamp((int) Math.round(100 * (0.70 * frequencyRatio + 0.30 * recencyRatio)));
        int churnRisk = clamp((int) Math.round(100 * (0.75 * (1 - recencyRatio) + 0.25 * revenueRatio)));
        String segment = potential >= 80 ? "VIP" : days > 60 && revenueRatio >= 0.20 ? "RECUPERAR"
                : frequencyRatio >= 0.45 ? "FRECUENTE" : potential >= 45 ? "CRECIMIENTO" : "NUTRIR";
        String level = potential >= 80 ? "MUY_ALTO" : potential >= 60 ? "ALTO" : potential >= 40 ? "MEDIO" : "POR_ACTIVAR";
        String recommendation = days > 90
                ? "Reactivar: fue un cliente valioso, pero lleva más de 90 días sin comprar."
                : potential >= 80
                ? "Prioridad VIP: ofrecer beneficios y anticipar su próxima compra."
                : airlines > 1
                ? "Oportunidad de fidelización: compra con varias aerolíneas."
                : "Nutrir con ofertas afines a su historial de compra.";
        return new TourismAiInsightsResponse.ClientInsight(
                client.getId(), customerName(client), client.getEmail(), sales.size(), income,
                lastSale, days, airlines, preferredAirline(sales),
                sales.isEmpty() ? BigDecimal.ZERO : income.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP),
                potential, interaction, churnRisk, segment, level, recommendation);
    }

    private TourismAiInsightsResponse.AirlineInsight airlineInsight(String name, List<Venta> sales, BigDecimal totalRevenue) {
        BigDecimal income = revenue(sales);
        long clients = sales.stream().map(Venta::getCliente).filter(c -> c != null).map(Cliente::getId).distinct().count();
        double share = totalRevenue.signum() == 0 ? 0 : income.multiply(BigDecimal.valueOf(100))
                .divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue();
        return new TourismAiInsightsResponse.AirlineInsight(name, sales.size(), income, clients, share);
    }

    private BigDecimal revenue(List<Venta> sales) {
        return sales.stream().map(this::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal total(Venta sale) {
        if (sale.getTarifa() != null) return sale.getTarifa();
        return sale.getTotal() == null ? BigDecimal.ZERO : sale.getTotal();
    }

    private LocalDate saleDate(Venta sale) {
        if (sale.getFechaVentaBackoffice() != null) return sale.getFechaVentaBackoffice();
        if (sale.getFechaCierre() != null) return sale.getFechaCierre().toLocalDate();
        return sale.getFechaCreacion() == null ? null : sale.getFechaCreacion().toLocalDate();
    }

    private int airlineCount(List<Venta> sales) {
        Set<String> airlines = sales.stream().map(Venta::getAerolinea)
                .filter(value -> value != null && !value.isBlank()).map(String::trim).collect(Collectors.toSet());
        return airlines.size();
    }

    private String preferredAirline(List<Venta> sales) {
        return sales.stream().map(Venta::getAerolinea)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(String::trim, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("Sin especificar");
    }

    private String customerName(Cliente client) {
        if (client.getEmpresa() != null && !client.getEmpresa().isBlank()) return client.getEmpresa().trim();
        String lastName = client.getApellido() == null || "-".equals(client.getApellido()) ? "" : client.getApellido();
        String name = ((client.getNombre() == null ? "" : client.getNombre()) + " " + lastName).trim();
        return name.isBlank() ? "Cliente " + client.getId() : name;
    }

    private String label(String value) {
        return value == null || value.isBlank() ? "Sin especificar" : value.trim();
    }

    private double ratio(BigDecimal value, BigDecimal maximum) {
        return maximum.signum() == 0 ? 0 : value.divide(maximum, 6, RoundingMode.HALF_UP).doubleValue();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
