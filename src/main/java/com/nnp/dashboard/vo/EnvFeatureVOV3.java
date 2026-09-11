package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class EnvFeatureVOV3 implements Serializable {
    private String feaId;

    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private EnvironmentVOV3 env;

    private String envId;
    private String feaName;
    private String feaType;
    private String feaDesc;
    private String feaSeq;
    private boolean isAssigned = false;

    // private String envCode;
    // private String envName;
    // private String envType;
    // private String envCustId;
    // private String envCustName;
    // private String envTenantId;
    // private String envFapId;
    // private String envFatNo;
    private List<FeatureElementVOV3> featureElements = new ArrayList<>();

}
