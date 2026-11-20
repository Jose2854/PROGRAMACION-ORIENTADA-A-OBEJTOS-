package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TourismAiInsightsResponse(
        LocalDate from,
        LocalDate to,
        LocalDate availableFrom,
        LocalDate availableTo,
        LocalDateTime generatedAt,
        String methodology,
        Summary summary,
        List<ClientInsight> clients,
        List<AirlineInsight> airlines
) {
    public record Summary(
            String topClient,
            BigDecimal topClientRevenue,
            String topAirline,
            BigDecimal topAirlineRevenue,
            String highestPotentialClient,
            int highestPotentialScore,
            long highPotentialClients,
            long atRiskClients,
            double topAirlineConcentration
    ) {}

    public record ClientInsight(
            Long clientId,
            String name,
            String email,
            long sales,
            BigDecimal revenue,
            LocalDate lastSale,
            long daysSinceLastSale,
            int airlinesUsed,
            String preferredAirline,
            BigDecimal averageTicket,
            int potentialScore,
            int interactionScore,
            int churnRiskScore,
            String segment,
            String level,
            String recommendation
    ) {}

    public record AirlineInsight(
            String name,
            long sales,
            BigDecimal revenue,
            long clients,
            double revenueShare
    ) {}
}
