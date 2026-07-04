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
        return login("admin", "fraud123");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
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
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    // RBAC: cache management requires the ADMIN role (SecurityConfig).
    @Test
    void adminCanEvictScenarioCache() throws Exception {
        String token = login(); // admin has roles ADMIN,USER
        mockMvc.perform(post("/api/v1/cache/evict-scenarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // RBAC: the analyst user only has USER — the same endpoint must be forbidden.
    @Test
    void analystWithoutAdminRoleCannotEvictScenarioCache() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(post("/api/v1/cache/evict-scenarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // Scenario management: an admin-created scenario must affect decisions IMMEDIATELY
    // (write-time SpEL validation passes, the scenario cache is evicted on create).
    @Test
    void adminCreatedScenarioAffectsNextDecision() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/v1/scenarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Blocked Merchant\",\"productType\":\"CARD\",\"module\":1,"
                                + "\"priority\":0,\"fraudResponseCode\":\"REVIEW\",\"rules\":["
                                + "{\"name\":\"blocked merchant\",\"expression\":\"merchantId == 'BLOCKME'\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber());

        // small amount, but the new scenario matches the merchant -> REVIEW
        mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"module\":1,\"transactionMessageId\":9001,\"shadowCardNo\":\"SCNCARD\","
                                + "\"amount\":10,\"merchantId\":\"BLOCKME\",\"transactionDate\":\"2026-01-01T12:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fraudResponseCode").value("REVIEW"));
    }

    @Test
    void scenarioWithUnsafeExpressionIsRejected() throws Exception {
        String token = login();
        // A type-reference/method-call expression (injection shape) must be rejected at write time.
        mockMvc.perform(post("/api/v1/scenarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Evil\",\"productType\":\"CARD\",\"module\":1,"
                                + "\"priority\":0,\"fraudResponseCode\":\"REJECT\",\"rules\":["
                                + "{\"name\":\"rce\",\"expression\":\"T(java.lang.Runtime).getRuntime().exec('x') != null\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void analystCannotCreateScenario() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(post("/api/v1/scenarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"productType\":\"CARD\",\"module\":1,"
                                + "\"priority\":0,\"fraudResponseCode\":\"REJECT\",\"rules\":["
                                + "{\"name\":\"r\",\"expression\":\"amountValue > 1\"}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyAuthenticatedUserCanListScenarios() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/scenarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
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
