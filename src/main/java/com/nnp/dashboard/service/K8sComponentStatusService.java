package com.nnp.dashboard.service;

import com.nnp.dashboard.dto.K8sPodListResponse;
import com.nnp.dashboard.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class K8sComponentStatusService {

    private final WebClient k8sWebClient;

    public K8sComponentStatusService(@Qualifier("webClientK8sApi") WebClient k8sWebClient) {
        this.k8sWebClient = k8sWebClient;
    }

    /**
     * Calls Kubernetes REST API to fetch all running pod names in the given namespace.
     *
     * @param namespace K8s namespace
     * @param bearerToken K8s bearer token for authentication
     * @return Set of running pod names in the namespace
     */
    public Set<String> getRunningPodNames(String namespace, String bearerToken) {
        if (namespace == null || namespace.isBlank()) {
            log.warn("Namespace is blank, returning empty running pods set");
            return Collections.emptySet();
        }

        try {
            WebClient.RequestHeadersSpec<?> reqSpec = k8sWebClient.get()
                    .uri("/api/v1/namespaces/{namespace}/pods", namespace);

            if (bearerToken != null && !bearerToken.isBlank()) {
                reqSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken.trim());
            }

            K8sPodListResponse response = reqSpec.retrieve()
                    .bodyToMono(K8sPodListResponse.class)
                    .block();

            if (response != null && response.getItems() != null) {
                Set<String> runningPods = response.getItems().stream()
                        .filter(item -> item.getStatus() != null && "Running".equalsIgnoreCase(item.getStatus().getPhase()))
                        .filter(item -> item.getMetadata() != null && item.getMetadata().getName() != null)
                        .map(item -> item.getMetadata().getName())
                        .collect(Collectors.toSet());

                log.info("Fetched {} running pods for namespace {}", Utils.sanitizeForLog(runningPods.size()), Utils.sanitizeForLog(namespace));
                return runningPods;
            }
        } catch (Exception e) {
            log.error("Failed to fetch k8s pod status for namespace {}: {}", Utils.sanitizeForLog(namespace), Utils.sanitizeForLog(e.getMessage()));
        }

        return Collections.emptySet();
    }
}
