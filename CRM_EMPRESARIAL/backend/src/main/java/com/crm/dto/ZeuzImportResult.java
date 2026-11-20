package com.crm.dto;

import java.math.BigDecimal;
import java.util.List;

public record ZeuzImportResult(
        int filasLeidas,
        int ventasImportadas,
        int duplicadosOmitidos,
        int filasConError,
        BigDecimal totalImportado,
        List<String> advertencias
) {}
