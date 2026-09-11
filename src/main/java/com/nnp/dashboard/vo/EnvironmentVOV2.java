package com.nnp.dashboard.vo;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EnvironmentVOV2 implements Serializable {

	private String envId;
	private String envCode;
	private String envName;
	private String envTypeId;
	private String envCustId;
	private String envCustName;
	private String envDesc;
	private String envTenantId;
	private String envFapId;
	private String envFatNo;
	private String envEmail;
//	private String envEmailServerIp;
//	private String envEmailServerPort;
	private String envStatus;
	private String envNamespace;
	private String envDomain;
	private String envRepo;
	private String envIp;
	//private boolean isAssigned = false;
	
	//private List<EnvFeatureVO> envFeatures = new ArrayList<EnvFeatureVO>();
	
	//private Set<UserAccessVO> userList = new HashSet<UserAccessVO>();
	private String usrCommunication;
	private String admCommunication;
	private String admK8SToken;
	private String usrK8SToken;
	
}
