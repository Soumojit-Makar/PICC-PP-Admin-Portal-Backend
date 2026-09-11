package com.nnp.dashboard.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nnp.dashboard.utils.Utils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nnp.dashboard.client.HAProxyIntegrationClient;
import com.nnp.dashboard.model.EnvProxyConfig;
import com.nnp.dashboard.model.EnvironmentV2;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.repo.EnvProxyConfigRepo;
import com.nnp.dashboard.repo.EnvironmentRepoV2;
import com.nnp.dashboard.vo.EnvProxyConfigVO;
import com.nnp.dashboard.vo.HAProxyInDTO;
import com.nnp.dashboard.vo.ImportResultVO;
import com.nnp.dashboard.vo.ProxyConfigWithStatusVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HAProxyDashboardService {

    private static final Set<String> KNOWN_NAMESPACES = Set.of(
        "itc", "somnetics", "iitkgp", "bws", "playground", "dev", "uat", "prod", "qa", "stage", "test",
        "core", "falco", "signoz", "openbao", "log-monitoring", "kubernetes-dashboard", "kube-system",
        "appsmith-ee", "nnp-devsecops", "nnp-core-components", "nnp-pg", "nnp-api-ecosystem"
    );

    @Autowired
    private EnvProxyConfigRepo proxyConfigRepo;

    @Autowired
    private EnvironmentRepoV2 envRepositoryV2;

    @Autowired
    private HAProxyIntegrationClient haproxyClient;

    @Autowired
    private ModelMapper modelMapper;

    @org.springframework.beans.factory.annotation.Value("${haproxy.base.domain:nnp.example.com}")
    private String haproxyBaseDomain;

    /**
     * Lists all proxy configs with live HAProxy DataPlane status.
     * Efficiently fetches live inventory in a single call to verify SYNCED vs NOT_SYNCED.
     */
    public List<ProxyConfigWithStatusVO> getAllConfigsWithStatus() {
        List<EnvProxyConfig> entities = proxyConfigRepo.findAll();
        List<ProxyConfigWithStatusVO> result = new ArrayList<>();

        Set<String> activeBackends = new HashSet<>();
        boolean inventoryAvailable = false;
        try {
            JsonNode inventory = haproxyClient.getInventory();
            if (inventory != null && inventory.has("backends")) {
                inventoryAvailable = true;
                for (JsonNode be : inventory.get("backends")) {
                    if (be.has("name")) {
                        activeBackends.add(be.get("name").asText().toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch live HAProxy inventory for status check: {}", Utils.sanitizeForLog(e.getMessage()));
        }

        for (EnvProxyConfig entity : entities) {
            ProxyConfigWithStatusVO vo = modelMapper.map(entity, ProxyConfigWithStatusVO.class);
            String compName = entity.getCompServName() != null ? entity.getCompServName().toLowerCase() : "";
            if (!inventoryAvailable) {
                vo.setStatus("UNKNOWN");
            } else if (activeBackends.contains(compName)) {
                vo.setStatus("SYNCED");
            } else {
                vo.setStatus("NOT_SYNCED");
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * Lists configs for a specific environment with live status.
     */
    public List<ProxyConfigWithStatusVO> getConfigsByEnvWithStatus(String envId) {
        List<EnvProxyConfig> entities = proxyConfigRepo.findByEnvId(envId);
        List<ProxyConfigWithStatusVO> result = new ArrayList<>();

        Set<String> activeBackends = new HashSet<>();
        boolean inventoryAvailable = false;
        try {
            JsonNode inventory = haproxyClient.getInventory();
            if (inventory != null && inventory.has("backends")) {
                inventoryAvailable = true;
                for (JsonNode be : inventory.get("backends")) {
                    if (be.has("name")) {
                        activeBackends.add(be.get("name").asText().toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch live HAProxy inventory for status check: {}", Utils.sanitizeForLog(e.getMessage()));
        }

        for (EnvProxyConfig entity : entities) {
            ProxyConfigWithStatusVO vo = modelMapper.map(entity, ProxyConfigWithStatusVO.class);
            String compName = entity.getCompServName() != null ? entity.getCompServName().toLowerCase() : "";
            if (!inventoryAvailable) {
                vo.setStatus("UNKNOWN");
            } else if (activeBackends.contains(compName)) {
                vo.setStatus("SYNCED");
            } else {
                vo.setStatus("NOT_SYNCED");
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * Synchronizes a proxy config to HAProxy (alias to registerConfig).
     */
    public boolean syncConfig(String configId) {
        return registerConfig(configId);
    }

    /**
     * Triggers register for a single config against HAProxy DataPlane API.
     */
    public boolean registerConfig(String configId) {
        EnvProxyConfig config = proxyConfigRepo.findById(configId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Proxy config not found with id: " + configId)));

        HAProxyInDTO dto = buildHAProxyIn(config);
        log.info("Triggering register for comp: {}, env: {}",Utils.sanitizeForLog( config.getCompServName()), Utils.sanitizeForLog(config.getEnvId()));
        return haproxyClient.register(dto);
    }

    /**
     * Triggers deregister for a single config.
     */
    public boolean deregisterConfig(String configId) {
        EnvProxyConfig config = proxyConfigRepo.findById(configId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Proxy config not found with id: " + configId)));

        HAProxyInDTO dto = buildHAProxyIn(config);
        log.info("Triggering deregister for comp: {}, env: {}", Utils.sanitizeForLog(config.getCompServName()), Utils.sanitizeForLog(config.getEnvId()));
        return haproxyClient.deregister(dto);
    }

    /**
     * Scans HAProxy live inventory and imports missing configs into DB.
     */
    public ImportResultVO importFromHAProxy(String parentFrontend) {
        String parentFE = (parentFrontend != null && !parentFrontend.isBlank()) ? parentFrontend : "http_front";
        log.info("Starting HAProxy import for parentFrontend: {}", Utils.sanitizeForLog(parentFE));

        JsonNode inventory = haproxyClient.getInventory();
        if (inventory == null) {
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("500", "Failed to retrieve inventory from HAProxy"));
        }

        // Map backendName -> condTest from switching rules
        Map<String, String> backendToAclMap = new HashMap<>();
        if (inventory.has("switchingRules")) {
            for (JsonNode rule : inventory.get("switchingRules")) {
                String fe = rule.has("frontend") ? rule.get("frontend").asText() : "";
                String be = rule.has("backend") ? rule.get("backend").asText() : "";
                String condTest = rule.has("condTest") ? rule.get("condTest").asText() : "";
                if (fe.equalsIgnoreCase(parentFE) && !be.isBlank() && !condTest.isBlank()) {
                    backendToAclMap.put(be, condTest);
                }
            }
        }

        // Map aclName -> domain value, and aclName -> path value
        Map<String, String> aclValueMap = new HashMap<>();
        Map<String, String> aclPathMap = new HashMap<>();
        if (inventory.has("acls")) {
            for (JsonNode acl : inventory.get("acls")) {
                String fe = acl.has("frontend") ? acl.get("frontend").asText() : "";
                String aclName = acl.has("aclName") ? acl.get("aclName").asText() : "";
                String criterion = acl.has("criterion") ? acl.get("criterion").asText() : "";
                String val = acl.has("value") ? acl.get("value").asText() : "";
                if (fe.equalsIgnoreCase(parentFE) && !aclName.isBlank() && !val.isBlank()) {
                    if (criterion.toLowerCase().startsWith("path")) {
                        aclPathMap.put(aclName, cleanPathValue(val));
                    } else {
                        aclValueMap.put(aclName, cleanDomainValue(val));
                    }
                }
            }
        }

        int imported = 0;
        int skipped = 0;
        int total = 0;

        if (inventory.has("backends")) {
            for (JsonNode be : inventory.get("backends")) {
                total++;
                String beName = be.has("name") ? be.get("name").asText() : "";
                if (beName.isBlank()) continue;

                // Check if already in DB
                Optional<EnvProxyConfig> existing = proxyConfigRepo.findByCompServName(beName);
                EnvProxyConfig entity;
                boolean isNew = false;
                if (existing.isPresent()) {
                    entity = existing.get();
                    skipped++;
                } else {
                    entity = new EnvProxyConfig();
                    entity.setCompServName(beName);
                    entity.setParentFrontend(parentFE);
                    entity.setLineIndex("0");
                    entity.setBackendType("K8S_DNS");
                    isNew = true;
                }

                // Servers info
                if (be.has("servers") && be.get("servers").isArray() && be.get("servers").size() > 0) {
                    JsonNode srv = be.get("servers").get(0);
                    int port = srv.has("port") ? srv.get("port").asInt() : 0;
                    String addr = srv.has("address") ? srv.get("address").asText() : "";
                    boolean isSsl = srv.has("ssl") && srv.get("ssl").asBoolean();

                    entity.setInternalPort(port);
                    entity.setSsl(isSsl);

                    // Check if k8s DNS or external IP
                    if (addr.contains(".svc.cluster.local")) {
                        String[] parts = addr.split("\\.");
                        if (parts.length >= 2) {
                            entity.setNamespace(parts[1]);
                        }
                    } else if (addr.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        entity.setBackendType("EXTERNAL_IP");
                        entity.setServerAddress(addr);
                        entity.setServerPort(port);
                    } else if (addr.contains(":") || addr.contains(".")) {
                        entity.setServerAddress(addr);
                        entity.setServerPort(port);
                    }
                }

                // Timeouts
                if (be.has("timeoutServer") && !be.get("timeoutServer").isNull()) {
                    entity.setTimeoutServer(be.get("timeoutServer").asLong());
                }
                if (be.has("timeoutTunnel") && !be.get("timeoutTunnel").isNull()) {
                    entity.setTimeoutTunnel(be.get("timeoutTunnel").asLong());
                }

                // Replace path rules from backend definition
                if (be.has("replacePathRules") && be.get("replacePathRules").isArray() && be.get("replacePathRules").size() > 0) {
                    JsonNode rpr = be.get("replacePathRules").get(0);
                    if (rpr.has("match")) {
                        entity.setPathPrefix(rpr.get("match").asText());
                        entity.setBackendType("PATH_REWRITE");
                    }
                }

                // Extract domain and path from frontend switching rules and ACLs
                String condTest = backendToAclMap.get(beName);
                extractRoutingInfo(entity, condTest, aclValueMap, aclPathMap);

                // Sanitize domainName: ensure no raw syntax exists
                if (entity.getDomainName() != null) {
                    String d = entity.getDomainName().trim();
                    if (d.startsWith("{") || d.startsWith("path") || !d.contains(".")) {
                        entity.setDomainName(null);
                    }
                }

                // Infer namespace cleanly
                String ns = entity.getNamespace();
                if (ns == null || ns.isBlank() || ns.startsWith("i ") || ns.contains(" ")) {
                    ns = null;
                }
                if (ns == null && entity.getDomainName() != null && entity.getDomainName().contains(".")) {
                    String dName = entity.getDomainName().trim();
                    String[] dParts = dName.split("\\.");
                    if (dParts.length >= 4 && isKnownNamespace(dParts[1])) {
                        ns = dParts[1].toLowerCase();
                    } else if (dParts[0].contains("-")) {
                        String[] subParts = dParts[0].split("-");
                        for (String sub : subParts) {
                            if (isKnownNamespace(sub)) {
                                ns = sub.toLowerCase();
                                break;
                            }
                        }
                        if (ns == null) {
                            String last = subParts[subParts.length - 1];
                            if (!last.equalsIgnoreCase("play") && !last.equalsIgnoreCase("backend") && !last.equalsIgnoreCase("service")) {
                                ns = last.toLowerCase();
                            }
                        }
                    }
                }
                if (ns == null) {
                    String[] bParts = beName.split("[-_]");
                    for (String part : bParts) {
                        if (isKnownNamespace(part)) {
                            ns = part.toLowerCase();
                            break;
                        }
                    }
                }
                if (ns == null || ns.isBlank()) {
                    ns = "default";
                }
                entity.setNamespace(ns);

                // Resolve valid env_id
                String actualEnvId = ns;
                if (ns != null && !ns.isBlank() && !"default".equalsIgnoreCase(ns)) {
                    Optional<EnvironmentV2> envOpt = envRepositoryV2.findByEnvCode(ns);
                    if (envOpt.isPresent()) {
                        actualEnvId = envOpt.get().getEnvCode();
                    } else {
                        Optional<EnvironmentV2> envById = envRepositoryV2.findById(ns);
                        if (envById.isPresent()) {
                            actualEnvId = envById.get().getEnvCode();
                        }
                    }
                }
                entity.setEnvId(actualEnvId);

                // Ensure clean domainName if still missing
                if (entity.getDomainName() == null || entity.getDomainName().isBlank() || entity.getDomainName().contains("_backend-")) {
                    if ("PATH_REWRITE".equalsIgnoreCase(entity.getBackendType()) && entity.getPathPrefix() != null && !entity.getPathPrefix().isBlank()) {
                        entity.setDomainName("");
                    } else {
                        String cleanCompName = beName.replaceAll("(?i)_backend$", "").replaceAll("(?i)-backend$", "");
                        if (!"default".equalsIgnoreCase(entity.getNamespace()) && cleanCompName.endsWith("_" + entity.getNamespace())) {
                            cleanCompName = cleanCompName.substring(0, cleanCompName.length() - ("_" + entity.getNamespace()).length());
                        }
                        entity.setDomainName(cleanCompName + "-" + entity.getNamespace() + "." + haproxyBaseDomain);
                    }
                }

                proxyConfigRepo.save(entity);
                if (isNew) {
                    imported++;
                }
            }
        }

        return new ImportResultVO(imported, skipped, total,
                String.format("Import completed: %d imported, %d updated/existed, %d total backends scanned", imported, skipped, total));
    }

    /**
     * Creates a new proxy config in DB manually.
     */
    public EnvProxyConfigVO createConfig(EnvProxyConfigVO vo) {
        EnvProxyConfig entity = modelMapper.map(vo, EnvProxyConfig.class);
        if (entity.getEnvId() != null && !entity.getEnvId().isBlank()) {
            Optional<EnvironmentV2> envOpt = envRepositoryV2.findByEnvCode(entity.getEnvId());
            if (envOpt.isPresent()) {
                entity.setEnvId(envOpt.get().getEnvId());
            }
        } else {
            entity.setEnvId(null);
        }
        if (entity.getParentFrontend() == null || entity.getParentFrontend().isBlank()) {
            entity.setParentFrontend("http_front");
        }
        if (entity.getBackendType() == null || entity.getBackendType().isBlank()) {
            entity.setBackendType("K8S_DNS");
        }
        EnvProxyConfig saved = proxyConfigRepo.save(entity);
        return modelMapper.map(saved, EnvProxyConfigVO.class);
    }

    /**
     * Updates an existing proxy config.
     */
    public EnvProxyConfigVO updateConfig(String configId, EnvProxyConfigVO vo) {
        EnvProxyConfig existing = proxyConfigRepo.findById(configId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Proxy config not found with id: " + configId)));

        if (vo.getEnvId() != null) existing.setEnvId(vo.getEnvId());
        if (vo.getCompServName() != null) existing.setCompServName(vo.getCompServName());
        if (vo.getDomainName() != null) existing.setDomainName(vo.getDomainName());
        if (vo.getParentFrontend() != null) existing.setParentFrontend(vo.getParentFrontend());
        if (vo.getSubpath() != null) existing.setSubpath(vo.getSubpath());
        if (vo.getLineIndex() != null) existing.setLineIndex(vo.getLineIndex());
        existing.setInternalPort(vo.getInternalPort());
        if (vo.getBackendType() != null) existing.setBackendType(vo.getBackendType());
        if (vo.getNamespace() != null) existing.setNamespace(vo.getNamespace());
        existing.setSsl(vo.isSsl());
        existing.setPathPrefix(vo.getPathPrefix());
        existing.setTimeoutServer(vo.getTimeoutServer());
        existing.setTimeoutTunnel(vo.getTimeoutTunnel());
        existing.setServerAddress(vo.getServerAddress());
        existing.setServerPort(vo.getServerPort());

        EnvProxyConfig saved = proxyConfigRepo.save(existing);
        return modelMapper.map(saved, EnvProxyConfigVO.class);
    }

    /**
     * Deletes a proxy config from DB.
     */
    public void deleteConfig(String configId) {
        EnvProxyConfig existing = proxyConfigRepo.findById(configId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Proxy config not found with id: " + configId)));
        proxyConfigRepo.delete(existing);
    }

    private HAProxyInDTO buildHAProxyIn(EnvProxyConfig config) {
        HAProxyInDTO dto = new HAProxyInDTO();
        dto.setCompName(config.getCompServName());
        dto.setParentFE(config.getParentFrontend() != null ? config.getParentFrontend() : "http_front");
        dto.setBackendType(config.getBackendType() != null ? config.getBackendType() : "K8S_DNS");
        dto.setNamespace(config.getNamespace() != null ? config.getNamespace() : config.getEnvId());
        dto.setInternalPort(config.getInternalPort());
        dto.setDomain(config.getDomainName());
        dto.setSsl(config.isSsl());
        dto.setPathPrefix(config.getPathPrefix());
        dto.setTimeoutServer(config.getTimeoutServer());
        dto.setTimeoutTunnel(config.getTimeoutTunnel());
        dto.setServerAddress(config.getServerAddress());
        dto.setServerPort(config.getServerPort());

        int lineIndex = 0;
        if (config.getLineIndex() != null) {
            try {
                lineIndex = Integer.parseInt(config.getLineIndex());
            } catch (Exception ignored) {}
        }
        dto.setLineIndex(lineIndex);

        return dto;
    }
    private static final Pattern HOST_PATTERN = Pattern.compile(
            "(?i)(?:req\\.)?hdr(?:_[a-z]+)?\\(host\\)\\s+(?:-i\\s+)?([a-zA-Z0-9_.-]+)"
    );
    private static final Pattern PATH_PATTERN = Pattern.compile(
            "(?i)path(?:_[a-z]++)?\\s++([/a-zA-Z0-9_.-]++)"
    );
    private void extractRoutingInfo(EnvProxyConfig entity, String condTest, Map<String, String> aclValueMap, Map<String, String> aclPathMap) {
        if (condTest == null || condTest.isBlank()) return;

        Matcher hostMatcher = HOST_PATTERN.matcher(condTest);
        if (hostMatcher.find()) {
            entity.setDomainName(hostMatcher.group(hostMatcher.groupCount()).trim());
        }

        // 2. Inline path check: e.g. { path /mgw } or { path_beg /mgw/ }
        Matcher pathMatcher = PATH_PATTERN.matcher(condTest);
        if (pathMatcher.find()) {
            entity.setPathPrefix(pathMatcher.group(1).trim());
            entity.setBackendType("PATH_REWRITE");
        }

        // 3. Named ACL tokens: e.g. is_redis-clnt, host_itc_kafka, or is_eoffice_host is_eoffice_user
        String[] tokens = condTest.split("\\s+");
        for (String token : tokens) {
            String clean = token.replaceAll("[{}()]", "").trim();
            if (aclValueMap.containsKey(clean)) {
                entity.setDomainName(aclValueMap.get(clean));
            }
            if (aclPathMap.containsKey(clean)) {
                entity.setPathPrefix(aclPathMap.get(clean));
                entity.setBackendType("PATH_REWRITE");
            }
        }
    }

    private boolean isKnownNamespace(String s) {
        return s != null && KNOWN_NAMESPACES.contains(s.trim().toLowerCase());
    }

    private String cleanDomainValue(String val) {
        if (val == null) return "";
        String v = val.trim();
        v = v.replaceAll("(?i)^.*?-i\\s+", "").trim();
        v = v.replaceAll("(?i)^(-m\\s+\\S+|-n)\\s+", "").trim();
        v = v.replaceAll("^[\"\'{}\s]+|[\"\'{}\s]+$", "").trim();
        return v;
    }

    private String cleanPathValue(String val) {
        if (val == null) return "";
        String v = val.trim();
        v = v.replaceAll("(?i)^.*?-i\\s+", "").trim();
        v = v.replaceAll("^[\"\'{}\s]+|[\"\'{}\s]+$", "").trim();
        return v;
    }
}