package com.nnp.dashboard.client;

import java.time.Duration;
import java.util.Map;

import com.nnp.dashboard.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nnp.dashboard.vo.HAProxyInDTO;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HAProxyIntegrationClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HAProxyIntegrationClient(
            @Value("${haproxy.service.baseurl:${haproxy.integration.url:http://localhost:8081}}") String baseUrl,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("Initializing HAProxyIntegrationClient with baseUrl: {}", Utils.sanitizeForLog(baseUrl));
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean register(HAProxyInDTO dto) {
        try {
            log.info("Calling HAProxy register for comp: {}", Utils.sanitizeForLog(dto.getCompName()));
            ResponseEntity<String> response = webClient.post()
                    .uri("/register")
                    .bodyValue(dto)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            return response != null && response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error in HAProxy register for {}: {}",Utils.sanitizeForLog( dto.getCompName()),Utils.sanitizeForLog( e.getMessage()));
            throw new RuntimeException("Failed to call HAProxy register: " + e.getMessage(), e);
        }
    }

    public boolean deregister(HAProxyInDTO dto) {
        try {
            log.info("Calling HAProxy deregister for comp: {}", Utils.sanitizeForLog(dto.getCompName()));
            ResponseEntity<String> response = webClient.post()
                    .uri("/deregister")
                    .bodyValue(dto)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            return response != null && response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error in HAProxy deregister for {}: {}", Utils.sanitizeForLog(dto.getCompName()),Utils.sanitizeForLog( e.getMessage()));
            throw new RuntimeException("Failed to call HAProxy deregister: " + e.getMessage(), e);
        }
    }

    public String getRegisterStatus(String compName) {
        try {
            String resp = webClient.get()
                    .uri("/register-status/{compName}", compName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(e -> Mono.just("{}"))
                    .block();

            if (resp != null && !resp.isBlank()) {
                JsonNode node = objectMapper.readTree(resp);
                if (node.has("status")) {
                    return node.get("status").asText();
                }
            }
            return "UNKNOWN";
        } catch (Exception e) {
            log.warn("Could not get register-status for {}: {}", Utils.sanitizeForLog(compName), Utils.sanitizeForLog(e.getMessage()));
            return "UNKNOWN";
        }
    }

    public JsonNode getInventory() {
        try {
            String resp = webClient.get()
                    .uri("/inventory")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (resp != null && !resp.isBlank()) {
                return objectMapper.readTree(resp);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch HAProxy inventory: {}", Utils.sanitizeForLog(e.getMessage()));
            throw new RuntimeException("Failed to fetch HAProxy inventory: " + e.getMessage(), e);
        }
    }

    public boolean triggerReload() {
        try {
            ResponseEntity<String> response = webClient.post()
                    .uri("/reload")
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(35))
                    .block();
            return response != null && response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error triggering HAProxy reload: {}",Utils.sanitizeForLog( e.getMessage()));
            return false;
        }
    }
}