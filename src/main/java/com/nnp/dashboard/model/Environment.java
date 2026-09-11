
package com.nnp.dashboard.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author AC
 *
 */

@Entity

@Table(name = "nnp_env")

@Getter
@Setter
// @ToString
public class Environment implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "env_id", nullable = false)
	private String envId;

	@Column(name = "env_code", nullable = false)
	private String envCode;

	@Column(name = "env_name", nullable = false)
	private String envName;

	@Column(name = "env_typeid")
	private String envType;

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
	@Column(name = "user_comm")
	private String userComm;
	@Column(name = "env_param1")
	private String envParam1;
	@Column(name = "env_param2")
	private String envParam2;
	@Column(name = "env_param3")
	private String envParam3;
	@Column(name = "env_param4")
	private String envParam4;
	@Column(name = "env_namespace")
	private String envNamespace;
	@Column(name = "env_domain")
	private String envDomain;
	@Column(name = "env_repo")
	private String envRepo;
	@Column(name = "env_ip")
	private String envIp;
	@Column(name = "admin_comm")
	private String adminComm;
	@Column(name = "admin_k8s_ns_token")
	private String adminK8sNsToken;
	@Column(name = "usr_k8s_ns_token")
	private String userK8sNsToken;
	@OneToMany(mappedBy = "env", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<EnvFeature> envFeatures = new ArrayList<EnvFeature>();

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "env_id")
	private List<EnvUserAccess> userList = new ArrayList<EnvUserAccess>();

}
