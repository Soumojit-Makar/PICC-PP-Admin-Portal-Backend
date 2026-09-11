package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProxyConfigWithStatusVO {
    private String envConfigId;
    private String envId;
    private String compServName;
    private String domainName;
    private String parentFrontend;
    private String subpath;
    private String lineIndex;
    private int internalPort;
    private String backendType;
    private String namespace;
    private boolean ssl;
    private String pathPrefix;
    private Long timeoutServer;
    private Long timeoutTunnel;
    private String serverAddress;
    private int serverPort;
    private String status; // "SYNCED", "NOT_SYNCED", "UNKNOWN", "FAILED"
    private String statusMessage;
}