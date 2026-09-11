package com.nnp.dashboard.vo;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanVOV2 {
	private long hostPlanid;
	private String hostPlanName;
	private String hostNode;
	private String hostCPU;
	private String hostMem;
	private String hostStorage;
	private String hostPlanDtlPageLink;
	private List<CompVOV2> CompVOV2;
	private BigDecimal hostDefaultDct;
	private String hostPlanBasePr;
}
