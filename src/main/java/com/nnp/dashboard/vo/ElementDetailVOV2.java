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
public class ElementDetailVOV2 implements Serializable {
    private String elementDtlId;
//    @JsonIgnore
//    private FeatureElementVO feaElement;

    private String elementId;

    private String elementDtlName;
    private String elementDtlType;
    private String elementDtlHome;
    private String elementDtlDesc;
    private String elementDtlURL;
    private String elementDtlFatNo;
    private String elementDtlSeq;
    private boolean isAssigned = false;
    /*
     * @JsonIgnore private ElementDetailVO prElemDtlId;
     */

    /*
     * @JsonManagedReference
     *
     * @JsonIgnore private List<ElementDetailVO> elemDetailList = new ArrayList<>();
     */
    //private List<CHElementDetailVO> childElementDtls = new ArrayList<>();

}
