package com.henryplatform.telemetry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        var config = new CorsConfiguration();

        // SEC: origins explícitas em vez de "*" — wildcard exporia a API a qualquer domínio
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",   // dashboard local (React)
            "http://localhost:19006"   // app cliente local (Expo)
        ));

        // SEC: apenas métodos necessários — OPTIONS é obrigatório para preflight CORS
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));

        // SEC: headers permitidos declarados explicitamente — evita aceitar headers arbitrários
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // SEC: credentials habilitadas apenas com origins explícitas — nunca com allowedOrigins("*")
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
