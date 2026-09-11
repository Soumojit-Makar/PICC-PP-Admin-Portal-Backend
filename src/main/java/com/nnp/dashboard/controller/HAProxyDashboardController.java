package com.nnp.dashboard.controller;

import java.util.List;

import com.nnp.dashboard.utils.Utils;
import org.jgroups.tests.rt.transports.UdpTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nnp.dashboard.service.HAProxyDashboardService;
import com.nnp.dashboard.vo.EnvProxyConfigVO;
import com.nnp.dashboard.vo.ImportResultVO;
import com.nnp.dashboard.vo.ProxyConfigWithStatusVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping({"/env/haproxy", "/haproxy"})
@Slf4j
@Tag(name = "HAProxy Management", description = "Endpoints for managing HAProxy proxy configurations and synchronizing with HAProxy service")
public class HAProxyDashboardController {

    @Autowired
    private HAProxyDashboardService dashboardService;

    @GetMapping("/configs")
    @Operation(summary = "Get all proxy configs with HAProxy live status")
    public ResponseEntity<List<ProxyConfigWithStatusVO>> getAllConfigs() {
        log.info("Fetching all proxy configs with status");
        return ResponseEntity.ok(dashboardService.getAllConfigsWithStatus());
    }

    @PostMapping("/sync/{configId}")
    @Operation(summary = "Synchronize / register a proxy config to HAProxy")
    public ResponseEntity<Void> syncConfig(@PathVariable String configId) {
        log.info("Syncing proxy config: {}", Utils.sanitizeForLog(configId));
        dashboardService.syncConfig(configId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/{configId}")
    @Operation(summary = "Explicitly register a proxy config to HAProxy")
    public ResponseEntity<Void> registerConfig(@PathVariable String configId) {
        log.info("Registering proxy config: {}", Utils.sanitizeForLog(configId));
        dashboardService.registerConfig(configId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deregister/{configId}")
    @Operation(summary = "Deregister a proxy config from HAProxy")
    public ResponseEntity<Void> deregisterConfig(@PathVariable String configId) {
        log.info("Deregistering proxy config: {}", Utils.sanitizeForLog(configId));
        dashboardService.deregisterConfig(configId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import")
    @Operation(summary = "Import missing records from live HAProxy configuration into DB")
    public ResponseEntity<ImportResultVO> importFromHAProxy(
            @RequestParam(defaultValue = "http_front", required = false) String parentFE) {
        log.info("Importing records from HAProxy frontend: {}", Utils.sanitizeForLog(parentFE));
        ImportResultVO result = dashboardService.importFromHAProxy(parentFE);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/configs")
    @Operation(summary = "Create a new proxy config in DB manually")
    public ResponseEntity<EnvProxyConfigVO> createConfig(@RequestBody EnvProxyConfigVO payload) {
        log.info("Creating new proxy config: {}", Utils.sanitizeForLog(payload));
        EnvProxyConfigVO created = dashboardService.createConfig(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/configs/{configId}")
    @Operation(summary = "Update an existing proxy config")
    public ResponseEntity<EnvProxyConfigVO> updateConfig(
            @PathVariable String configId,
            @RequestBody EnvProxyConfigVO payload) {
        log.info("Updating proxy config {}: {}", Utils.sanitizeForLog(configId), Utils.sanitizeForLog(payload));
        EnvProxyConfigVO updated = dashboardService.updateConfig(configId, payload);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/configs/{configId}")
    @Operation(summary = "Delete a proxy config from DB")
    public ResponseEntity<Void> deleteConfig(@PathVariable String configId) {
        log.info("Deleting proxy config: {}", Utils.sanitizeForLog(configId));
        dashboardService.deleteConfig(configId);
        return ResponseEntity.noContent().build();
    }
}