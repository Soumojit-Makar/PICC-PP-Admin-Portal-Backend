package com.nnp.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nnp.dashboard.config.OpenApiConfig;
import com.nnp.dashboard.utils.Utils;
import com.nnp.dashboard.vo.HAProxyInDTO;
import com.nnp.dashboard.vo.ImportResultVO;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * Unit tests verifying core utility functions, OpenAPI specifications, and DTO structures.
 */
class EnvDashboardConfiguratorApplicationTests {

    @Test
    @DisplayName("Utils.sanitizeForLog should sanitize newlines and handle null gracefully")
    void testSanitizeForLog() {
        assertEquals("null", Utils.sanitizeForLog(null));
        assertEquals("CleanString", Utils.sanitizeForLog("CleanString"));
        assertEquals("Line1Line2", Utils.sanitizeForLog("Line1\nLine2"));
        assertEquals("Line1Line2", Utils.sanitizeForLog("Line1\r\nLine2"));
    }

    @Test
    @DisplayName("OpenApiConfig should generate valid OpenAPI 3.0 document with required metadata")
    void testOpenApiConfig() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI, "OpenAPI bean must not be null");
        assertNotNull(openAPI.getInfo(), "Info section must be populated");
        assertEquals("PICC-PP-Admin-Portal-Backend REST API", openAPI.getInfo().getTitle());
        assertEquals("0.0.1-SNAPSHOT", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getInfo().getLicense(), "License must be configured");
        assertEquals("Apache License 2.0", openAPI.getInfo().getLicense().getName());
        assertNotNull(openAPI.getInfo().getContact(), "Contact information must be present");
        assertEquals("contribution@nubons.com", openAPI.getInfo().getContact().getEmail());

        assertNotNull(openAPI.getServers(), "Server list must not be empty");
        assertTrue(openAPI.getServers().size() >= 2, "Should include default and local servers");

        assertNotNull(openAPI.getTags(), "Tags list must not be empty");
        assertTrue(openAPI.getTags().stream().anyMatch(t -> t.getName().equals("Environment Management")));
        assertTrue(openAPI.getTags().stream().anyMatch(t -> t.getName().equals("User Access & RBAC")));
    }

    @Test
    @DisplayName("ImportResultVO should properly store and return statistics")
    void testImportResultVO() {
        ImportResultVO result = new ImportResultVO(5, 2, 7, "Import completed successfully");
        assertEquals(5, result.getImported());
        assertEquals(2, result.getSkipped());
        assertEquals(7, result.getTotal());
        assertEquals("Import completed successfully", result.getMessage());
    }

    @Test
    @DisplayName("HAProxyInDTO should hold component and backend routing configuration")
    void testHAProxyInDTO() {
        HAProxyInDTO dto = new HAProxyInDTO();
        dto.setCompName("test-service");
        dto.setNamespace("default");
        dto.setInternalPort(8080);
        dto.setBackendType("K8S_DNS");
        dto.setDomain("test-service-default.nnp.example.com");

        assertEquals("test-service", dto.getCompName());
        assertEquals("default", dto.getNamespace());
        assertEquals(8080, dto.getInternalPort());
        assertEquals("K8S_DNS", dto.getBackendType());
        assertEquals("test-service-default.nnp.example.com", dto.getDomain());
    }
}
