
package com.nnp.dashboard.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author AC
 *
 */

@Entity

@Table(name = "nnp_env")

@Getter @Setter
//@ToString
public class EnvironmentV2 implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "env_id", nullable = false)
	private String envId;

	@Column(name = "env_code", nullable = false, unique = true)
	private String envCode;

	@Column(name = "env_name", nullable = false)
	private String envName;

	@Column(name = "env_typeid", nullable = false)
	private String envTypeId;

	@Column(name = "env_custid", nullable = false)
	private String envCustId;

	@Column(name = "env_custname")
	private String envCustName;

	@Column(name = "env_desc")
	private String envDesc;

	@Column(name = "env_tenant", nullable = false)
	private String envTenantId;

	@Column(name = "env_fapid", nullable = false)
	private String envFapId;

	@Column(name = "env_fatno", nullable = false)
	private String envFatNo;

	@Column(name = "env_email")
	private String envEmail;
	
	@Column(name = "env_status")
	private String envStatus;

	@Column(name = "env_namespace")
	private String envNamespace;

	@Column(name = "env_domain")
	private String envDomain;

	@Column(name = "env_repo")
	private String envRepo;

	@Column(name = "env_ip")
	private String envIp;

	@Column(name = "user_comm")
	private String usrCommunication;
	
	@Column(name = "admin_comm")
	private String admCommunication;
	
	@Column(name = "admin_k8s_ns_token")
	private String admK8SToken;
	
	@Column(name = "usr_k8s_ns_token")
	private String usrK8SToken;

}
