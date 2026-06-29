package com.payguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uçtan uca entegrasyon testi: gerçek Spring context + H2 + Flyway + seeder + güvenlik.
 *
 * .NET karşılığı: WebApplicationFactory ile yapılan integration test'ler.
 * Çalışırken: Flyway şemayı kurar, ScenarioSeeder örnek senaryoları yükler, JWT güvenliği aktiftir.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FraudFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"payguard123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    void login_gecerli_kimlikle_token_doner() throws Exception {
        String token = login();
        org.junit.jupiter.api.Assertions.assertFalse(token.isBlank());
    }

    @Test
    void fraud_token_olmadan_401_403_doner() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(6000)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void fraud_yuksek_tutar_REJECT_doner() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(6000)))   // > 5000 eşik -> "Yüksek Tutar" senaryosu (REJECT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fraudResponseCode").value("REJECT"));
    }

    @Test
    void ai_check_transaction_yetersiz_gecmiste_anomaly_doner() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/v1/ai/check-transaction")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":\"22222222-2222-2222-2222-222222222222\","
                                + "\"shadowCardNo\":\"NEWCARD\",\"amount\":50000,\"merchantId\":\"M1\","
                                + "\"transactionDate\":\"2026-01-01T03:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anomaly").value(true));
    }

    private String cardRequest(int amount) {
        return "{\"module\":1,\"transactionMessageId\":1001,\"shadowCardNo\":\"CARD123\",\"amount\":" + amount
                + ",\"merchantId\":\"MERCH1\",\"transactionDate\":\"2026-01-01T03:00:00Z\"}";
    }
}
