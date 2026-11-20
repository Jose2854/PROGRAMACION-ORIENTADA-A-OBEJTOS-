/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.config;

import com.crm.repository.TenantRepository;
import com.crm.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionBootstrapInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final AuthService authService;

    @Value("${app.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.company-name:COLAEREO}")
    private String companyName;

    @Value("${app.bootstrap.admin-name:Administrador COLAEREO}")
    private String adminName;

    @Value("${app.bootstrap.admin-username:JLEGUIZAMON}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-email:admin@colaereo.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        String slug = companyName.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (tenantRepository.existsBySlug(slug)) {
            log.info("Bootstrap omitido: la empresa {} ya existe", companyName);
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()
                || adminPassword.startsWith("CAMBIAR_")) {
            log.warn("Bootstrap omitido: configure APP_BOOTSTRAP_ADMIN_PASSWORD en el Secret");
            return;
        }

        AuthService.TenantRegistrationRequest request = new AuthService.TenantRegistrationRequest();
        request.setCompanyName(companyName);
        request.setAdminName(adminName);
        request.setAdminUsername(adminUsername);
        request.setAdminEmail(adminEmail);
        request.setAdminPassword(adminPassword);
        request.setCountry("CO");
        request.setCurrency("COP");
        request.setTimezone("America/Bogota");
        request.setLocale("es-CO");
        request.setPlanName("AGENCY");
        authService.registerTenant(request);

        log.info("Empresa {} y usuario administrador {} creados", companyName, adminUsername);
    }
}
