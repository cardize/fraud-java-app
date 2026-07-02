package com.fraud.integration;

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
 * End-to-end integration test: real Spring context + H2 + Flyway + seeder + security.
 *
 * While running: Flyway creates the schema, ScenarioSeeder loads sample scenarios, JWT security is active.
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
                        .content("{\"username\":\"admin\",\"password\":\"fraud123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        String token = login();
        org.junit.jupiter.api.Assertions.assertFalse(token.isBlank());
    }

    @Test
    void fraudWithoutTokenReturns4xx() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(6000)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void highAmountFraudReturnsReject() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardRequest(6000)))   // > 5000 threshold -> "High Amount" scenario (REJECT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fraudResponseCode").value("REJECT"));
    }

    @Test
    void aiCheckTransactionReturnsAnomalyWithInsufficientHistory() throws Exception {
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

    @Test
    void fraudWithInvalidBodyReturns400() throws Exception {
        String token = login();
        // blank shadowCardNo (@NotBlank) + negative amount (@DecimalMin) -> validation error
        String invalid = "{\"module\":1,\"transactionMessageId\":1001,\"shadowCardNo\":\"\","
                + "\"amount\":-5,\"merchantId\":\"M1\",\"transactionDate\":\"2026-01-01T03:00:00Z\"}";
        mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tokenIsRejectedAfterLogout() throws Exception {
        String token = login();

        // the token is valid before logout
        mockMvc.perform(post("/api/v1/ai/check-transaction")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkTransactionRequest()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // the same token is now rejected (blacklisted)
        mockMvc.perform(post("/api/v1/ai/check-transaction")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkTransactionRequest()))
                .andExpect(status().is4xxClientError());
    }

    private String checkTransactionRequest() {
        return "{\"transactionId\":\"33333333-3333-3333-3333-333333333333\","
                + "\"shadowCardNo\":\"LOGOUTCARD\",\"amount\":100,\"merchantId\":\"M1\","
                + "\"transactionDate\":\"2026-01-01T03:00:00Z\"}";
    }

    private String cardRequest(int amount) {
        return "{\"module\":1,\"transactionMessageId\":1001,\"shadowCardNo\":\"CARD123\",\"amount\":" + amount
                + ",\"merchantId\":\"MERCH1\",\"transactionDate\":\"2026-01-01T03:00:00Z\"}";
    }
}
