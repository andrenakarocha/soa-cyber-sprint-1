package com.henryplatform.telemetry.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HENRY Telemetry Service")
                        .version("1.0.0")
                        .description("""
                                Telemetry microservice for the HENRY Platform (Ford ZeroTouch project).

                                HENRY — Holistic Engagement Navigator for Retention & Yield — is the AI assistant
                                that proactively engages Ford vehicle owners to retain them in the official service network (VIN Share).

                                This service receives OBD-II vehicle data, calculates the HENRY health score,
                                and provides the data foundation for HENRY's proactive scheduling conversations.

                                **Security model:** JWT Bearer tokens with RBAC (CUSTOMER, MECHANIC, DEALER_MANAGER, FORD_ADMIN).
                                Sensitive telemetry fields (engine temperature, oil life) are encrypted at rest (AES).
                                All operations are audit-logged.
                                """))
                // SEC: esquema Bearer declarado no Swagger para que todos os endpoints exijam autenticação explícita
                .schemaRequirement("bearerAuth",
                        new SecurityScheme()
                                .type(Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
    }
}
