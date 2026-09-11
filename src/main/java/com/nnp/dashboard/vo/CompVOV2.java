package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompVOV2 {
	private String compId;
	private String compName;
	private String compDesc;
	private String compStatus;
    private Short itemSeq;
    private String selectionType;
    private String baseDayPrice;
}
