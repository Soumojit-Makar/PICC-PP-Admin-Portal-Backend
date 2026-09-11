package com.nnp.dashboard.model;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Entity
@Table(name = "env_proxy_config", schema = "portal")
@Getter
@Setter
@ToString
public class EnvProxyConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "env_config_id", updatable = false, nullable = false)
    private String envConfigId;

    @Column(name = "env_id")
    private String envId;

    @Column(name = "comp_srv_name")
    private String compServName;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "parent_frontend")
    private String parentFrontend;

    @Column(name = "subpath")
    private String subpath;

    @Column(name = "line_index")
    private String lineIndex;
    
    @Column(name = "internal_port")
    private int internalPort;

    @Column(name = "backend_type")
    private String backendType;

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "is_ssl")
    private boolean ssl;

    @Column(name = "path_prefix")
    private String pathPrefix;

    @Column(name = "timeout_server")
    private Long timeoutServer;

    @Column(name = "timeout_tunnel")
    private Long timeoutTunnel;

    @Column(name = "server_address")
    private String serverAddress;

    @Column(name = "server_port")
    private int serverPort;
}