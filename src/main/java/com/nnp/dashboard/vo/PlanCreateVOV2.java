package com.nnp.dashboard.vo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * VO used to create / update a hosting plan (nnp_plan). Optional list of
 * plan -> component mappings can be supplied together with the plan.
 *
 * @author AC
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanCreateVOV2 {
	private long hostPlanid;
	private String hostPlanName;
	private String hostPlanDesc;
	private String hostPlanCatagory;
	private String hostPlanStatus;
	private String hostPlanSpotlight;
	private String hostPlanBasePr;
	private Short hostMaxPod;
	private BigInteger hostMaxBandw;
	private String hostMaxAction;
	private Integer hostMinDuration;
	private Integer hostMaxPCT;
	private String hostNode;
	private String hostCPU;
	private String hostMem;
	private String hostStorage;
	private boolean isActive;
	private Short itemSeq;
	private BigDecimal hostDefaultDct;
	private String countryId;
	private String hostPlanDtlPageLink;
	private List<PlanCompVOV2> planComps;
}
