package com.nnp.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
//@ToString
public class EnvironmentVOV3 implements Serializable {

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
	
	private List<EnvFeatureVOV3> envFeatures = new ArrayList<>();
	
	//private Set<UserAccessVO> userList = new HashSet<UserAccessVO>();
	
}
