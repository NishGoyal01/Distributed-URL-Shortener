package com.example.urlshortener.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.repository.ReleasedShortCodeRepository;
import com.example.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("urlshortener")
        .withUsername("urlshortener")
        .withPassword("change-me-locally");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private ReleasedShortCodeRepository releasedShortCodeRepository;

    @BeforeEach
    void clearTestData() {
        urlRepository.deleteAllInBatch();
        releasedShortCodeRepository.deleteAllInBatch();
    }

    @Test
    void shouldServeFrontendAndStaticAssets() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/styles.css"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/app.js"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldServeFrontendRootAndStaticAssets() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/styles.css"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/app.js"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldStartApplicationAndCreateUrlRedirectAndStats() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\":\"https://example.com/integration-test\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shortCode = createJson.get("shortCode").asText();

        mockMvc.perform(get("/" + shortCode))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", containsString("https://example.com/integration-test")));

        mockMvc.perform(get("/api/v1/urls/" + shortCode + "/stats"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldGenerateShortUrlFromForwardedHttpsOrigin() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                .header("X-Forwarded-Host", "links.example.com")
                .header("X-Forwarded-Proto", "https")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\":\"https://example.com/forwarded-test\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.matchesRegex("https://links\\.example\\.com/[0-9A-Za-z]+")));
    }

    @Test
    void shouldReuseShortCodeForSameTrimmedOriginalUrl() throws Exception {
        JsonNode first = createUrl("  https://example.com/reuse?x=1#part  ");
        Long originalId = urlRepository.findByShortCode(first.get("shortCode").asText()).orElseThrow().getId();
        JsonNode second = createUrl("https://example.com/reuse?x=1#part");

        assertEquals(first.get("shortCode").asText(), second.get("shortCode").asText());
        assertEquals(1, urlRepository.countByNormalizedOriginalUrl("https://example.com/reuse?x=1#part"));
        assertEquals(originalId, urlRepository.findByShortCode(first.get("shortCode").asText()).orElseThrow().getId());
        assertCodeFormat(second.get("shortCode").asText());
    }

    @Test
    void shouldGenerateDifferentCodesForDifferentOriginalUrls() throws Exception {
        JsonNode first = createUrl("https://example.com/different-one");
        JsonNode second = createUrl("https://example.com/different-two");

        assertNotEquals(first.get("shortCode").asText(), second.get("shortCode").asText());
        assertEquals(2, urlRepository.count());
    }

    @Test
    void shouldReplaceExpiredMappingAndReuseDeletedCodeOnlyOnce() throws Exception {
        JsonNode expired = createUrl("https://example.com/expired-replace");
        UrlEntity expiredEntity = urlRepository.findByShortCode(expired.get("shortCode").asText()).orElseThrow();
        expiredEntity.setExpiresAt(Instant.now().minusSeconds(1));
        urlRepository.saveAndFlush(expiredEntity);

        JsonNode replacement = createUrl("https://example.com/expired-replace");
        assertEquals(expired.get("shortCode").asText(), replacement.get("shortCode").asText());
        assertEquals(1, urlRepository.countByNormalizedOriginalUrl("https://example.com/expired-replace"));

        JsonNode toDelete = createUrl("https://example.com/released-code");
        String releasedCode = toDelete.get("shortCode").asText();
        mockMvc.perform(delete("/api/v1/urls/{shortCode}", releasedCode))
            .andExpect(status().isNoContent());
        assertEquals(1, releasedShortCodeRepository.count());

        mockMvc.perform(delete("/api/v1/urls/{shortCode}", releasedCode))
            .andExpect(status().isNotFound());
        assertEquals(1, releasedShortCodeRepository.count());

        JsonNode next = createUrl("https://example.com/after-delete");
        assertEquals(releasedCode, next.get("shortCode").asText());
    }

    @Test
    void concurrentDuplicateRequestsReturnOneMappingAndCode() throws Exception {
        int requestCount = 8;
        var executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Callable<String>> requests = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                requests.add(() -> createUrl("https://example.com/concurrent").get("shortCode").asText());
            }

            List<String> codes = executor.invokeAll(requests).stream()
                .map(future -> {
                    try {
                        return future.get(60, TimeUnit.SECONDS);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .toList();

            assertTrue(codes.stream().allMatch(codes.get(0)::equals));
            assertEquals(1, urlRepository.countByNormalizedOriginalUrl("https://example.com/concurrent"));
            assertEquals(1, urlRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRejectInvalidUrlRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\":\"not-a-valid-url\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundForValidButNonexistentShortCode() throws Exception {
        mockMvc.perform(get("/doesNotExist123"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectMalformedShortCode() throws Exception {
        mockMvc.perform(get("/missing-code"))
            .andExpect(status().isBadRequest());
    }

    private JsonNode createUrl(String originalUrl) throws Exception {
        String escapedUrl = objectMapper.writeValueAsString(originalUrl);
        MvcResult result = mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\":" + escapedUrl + "}"))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertCodeFormat(String code) {
        assertTrue(code.length() >= 6);
        assertTrue(code.matches("[0-9A-Za-z]+"));
        assertTrue(code.matches(".*[A-Za-z].*"));
        assertTrue(code.matches(".*[0-9].*"));
    }
}
