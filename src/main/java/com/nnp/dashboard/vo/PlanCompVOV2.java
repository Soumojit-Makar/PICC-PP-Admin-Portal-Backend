package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * VO for a plan -> component mapping row (nnp_plan_comp). The component group
 * is a reference to the nnp_plan_comp_group master.
 *
 * @author AC
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanCompVOV2 {
	private long hostRegPlanId;
	private String compId;
	private String compName;
	private Long compGroupId;
	private String compGroupTitle;
	private String hostPlanCompType;
	private String hostBaseMNPr;
	private String hostPlanCompStatus;
}
