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
public class FeatureElementVOV3 implements Serializable {
    private String elementId;
	@JsonIgnore
	private EnvFeatureVOV3 feature;

    private String featureId;
    private String elementName;
    private String elementType;
    private String elementDesc;
    private String elementPage;
    private String feaSeq;
    private boolean isAssigned = false;
    private List<ElementDetailVOV3> elementDetails = new ArrayList<>();

}
