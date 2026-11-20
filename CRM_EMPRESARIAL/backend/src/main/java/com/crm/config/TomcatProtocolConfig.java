/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
package com.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatProtocolConfig
        implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Value("${server.tomcat.protocol:}")
    private String protocol;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        if (protocol != null && !protocol.isBlank()) {
            factory.setProtocol(protocol);
        }
    }
}
