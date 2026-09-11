package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HAProxyInDTO {
    private String compName;
    private String parentFE;
    private String backendType;
    private String namespace;
    private int internalPort;
    private String domain;
    private int lineIndex;
    private boolean ssl;
    private String pathPrefix;
    private String pathReplacement;
    private Long timeoutServer;
    private Long timeoutTunnel;
    private String serverAddress;
    private int serverPort;
    private String resolvers;
}