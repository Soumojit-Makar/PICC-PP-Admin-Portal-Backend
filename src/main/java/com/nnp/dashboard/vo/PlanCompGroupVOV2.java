package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * VO for a component group (nnp_plan_comp_group). Groups are reused while
 * mapping components to a plan.
 *
 * @author AC
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanCompGroupVOV2 {
	private long hostPlanCompGroupId;
	private String compGroupTitle;
	private String compGroupDesc;
	private int itemSeq;
}
