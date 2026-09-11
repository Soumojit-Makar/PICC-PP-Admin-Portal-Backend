package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class EnvFeatureVOV2 implements Serializable {
    private String feaId;

    //	@JsonIgnore
//	private EnvironmentVO env;
    private String envId;
    private String feaName;
    private String feaType;
    private String feaDesc;
    private String feaSeq;
    private boolean isAssigned = false;

//	private String envCode;
//	private String envName;
//	private String envType;
//	private String envCustId;
//	private String envCustName;
//	private String envTenantId;
//	private String envFapId;
//	private String envFatNo;
//	private List<FeatureElementVO> featureElements = new ArrayList<FeatureElementVO>();


}
