package com.nnp.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sPodListResponse {
    private List<PodItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PodItem {
        private PodMetadata metadata;
        private PodStatus status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PodMetadata {
        private String name;
        private String namespace;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PodStatus {
        private String phase;
    }
}
