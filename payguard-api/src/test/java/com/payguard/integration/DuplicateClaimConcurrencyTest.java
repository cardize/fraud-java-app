package com.payguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Aynı transactionMessageId ile gelen eşzamanlı isteklerin TAM OLARAK BİRİNİN normal işlendiğini,
 * geri kalanlarının DUPLICATE döndüğünü kilitler.
 *
 * Önceki "SELECT ile var mı bak, sonra INSERT et" deseni bunu garanti edemiyordu: yeterince
 * yakın zamanlı eşzamanlı istekler hepsi "yok" görüp hepsi NORMAL işlenebiliyordu (fraud
 * senaryosu N kez koşar, N kez outbox'a yazılırdı). claimMessage()'ın atomik mutex davranışı
 * bunu DB seviyesinde imkânsız kılar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DuplicateClaimConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 8;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ayni_messageId_ile_esZamanli_istekler_sadece_bir_kez_normal_islenir() throws Exception {
        String token = login();
        long sharedMessageId = 987_654_321L;

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(pool.submit(() -> {
                    MvcResult result = mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(cardRequest(sharedMessageId)))
                            .andReturn();
                    return objectMapper.readTree(result.getResponse().getContentAsString())
                            .path("data").path("fraudResponseCode").asText();
                }));
            }

            AtomicInteger normalCount = new AtomicInteger();
            AtomicInteger duplicateCount = new AtomicInteger();
            for (Future<String> f : futures) {
                String code = f.get(15, TimeUnit.SECONDS);
                if ("DUPLICATE".equals(code)) {
                    duplicateCount.incrementAndGet();
                } else {
                    normalCount.incrementAndGet();
                }
            }

            assertEquals(1, normalCount.get(), "Tam olarak bir istek NORMAL (fraud senaryosu çalıştırılmış) olmalıydı");
            assertEquals(CONCURRENT_REQUESTS - 1, duplicateCount.get(), "Geri kalan tüm istekler DUPLICATE olmalıydı");
        } finally {
            pool.shutdown();
        }
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"payguard123\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private String cardRequest(long messageId) {
        return "{\"module\":1,\"transactionMessageId\":" + messageId + ",\"shadowCardNo\":\"CARD123\",\"amount\":100"
                + ",\"merchantId\":\"MERCH1\",\"transactionDate\":\"2026-01-01T03:00:00Z\"}";
    }
}
