package com.crm.controller;

import com.crm.dto.TourismReportResponse;
import com.crm.dto.TourismAiInsightsResponse;
import com.crm.service.TourismAiInsightsService;
import com.crm.service.TourismReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/tourism")
@RequiredArgsConstructor
public class TourismReportController {

    private final TourismReportService tourismReportService;
    private final TourismAiInsightsService tourismAiInsightsService;

    @GetMapping
    public TourismReportResponse getReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        return tourismReportService.report(start, end);
    }

    @GetMapping("/ai-insights")
    public TourismAiInsightsResponse getAiInsights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfYear(1) : from;
        return tourismAiInsightsService.insights(start, end);
    }
}
