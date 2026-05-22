package com.henryplatform.telemetry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.henryplatform.telemetry.dto.TelemetryReadingRequest;
import com.henryplatform.telemetry.dto.TelemetryReadingResponse;
import com.henryplatform.telemetry.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @MockBean TelemetryService telemetryService;

    private static final String VIN = "1FTFW1ET5EKE07497";

    // SEC: testa que requisição sem token retorna 401 — endpoint não está aberto ao público
    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/{vin}/readings", VIN))
                .andExpect(status().isUnauthorized());
    }

    // SEC: testa que CUSTOMER não consegue criar leitura — RBAC funcionando corretamente
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403WhenCustomerTriesToCreateReading() throws Exception {
        var req = validRequest();
        mockMvc.perform(post("/api/v1/vehicles/{vin}/readings", VIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MECHANIC")
    void shouldCreateReadingSuccessfullyAsMechanic() throws Exception {
        var req = validRequest();
        var response = TelemetryReadingResponse.builder()
                .id(1L).vin(VIN).engineTempCelsius(92.0).oilLifePercent(45)
                .healthScore(85).processed(false).recordedAt(LocalDateTime.now()).build();

        when(telemetryService.registerReading(eq(VIN), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/vehicles/{vin}/readings", VIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.healthScore").value(85));
    }

    // SEC: testa que input inválido é rejeitado com 400 antes de chegar ao service
    @Test
    @WithMockUser(roles = "MECHANIC")
    void shouldReturn400ForInvalidVinInBody() throws Exception {
        var req = validRequest();
        req.setVin("INVALID_VIN");

        mockMvc.perform(post("/api/v1/vehicles/{vin}/readings", VIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // SEC: testa que DELETE sem role FORD_ADMIN retorna 403
    @Test
    @WithMockUser(roles = "MECHANIC")
    void shouldReturn403WhenMechanicTriesToDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/vehicles/{vin}/readings/1", VIN))
                .andExpect(status().isForbidden());
    }

    private TelemetryReadingRequest validRequest() {
        var req = new TelemetryReadingRequest();
        req.setVin(VIN);
        req.setEngineTempCelsius(92.0);
        req.setOilLifePercent(45);
        req.setRpm(2500);
        req.setFuelLevelPercent(70);
        return req;
    }
}
