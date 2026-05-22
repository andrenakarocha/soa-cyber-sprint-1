package com.henryplatform.telemetry.config;

import com.henryplatform.telemetry.security.JwtAuthFilter;
import com.henryplatform.telemetry.security.PayloadSignatureFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final PayloadSignatureFilter payloadSignatureFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // SEC: CSRF desabilitado pois a API é stateless (JWT) — CSRF só é relevante para sessões com cookies
            .csrf(AbstractHttpConfigurer::disable)

            // SEC: política stateless — nenhuma sessão criada no servidor, escala horizontal sem estado
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos: documentação e autenticação
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/api/v1/auth/**").permitAll()

                // SEC: admin de retenção restrito a FORD_ADMIN — operação destrutiva irreversível
                .requestMatchers("/api/v1/admin/**").hasRole("FORD_ADMIN")

                // SEC: DELETE restrito a FORD_ADMIN — dado de telemetria é crítico, não pode ser apagado por qualquer role
                .requestMatchers(HttpMethod.DELETE, "/api/v1/vehicles/**").hasRole("FORD_ADMIN")

                // SEC: escrita (POST/PATCH) exige ao menos role MECHANIC — leituras brutas não podem ser criadas por clientes
                .requestMatchers(HttpMethod.POST, "/api/v1/vehicles/**").hasAnyRole("MECHANIC", "DEALER_MANAGER", "FORD_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/vehicles/**").hasAnyRole("MECHANIC", "DEALER_MANAGER", "FORD_ADMIN")

                // Leitura autenticada para qualquer role válida
                .anyRequest().authenticated()
            )

            // SEC: PayloadSignatureFilter executa antes do JWT — garante integridade do corpo antes de autenticar
            .addFilterBefore(payloadSignatureFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
