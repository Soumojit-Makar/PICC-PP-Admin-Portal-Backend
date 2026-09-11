package com.nnp.dashboard.vo;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NNPAccountVO {
	private String accId;
	private String accName;
	private UserVOV2 user;
	private String countryCode;
	private String orgName;
	private String orgCategory;
	private String platformPurpose;
	private PlanVOV2 selectedPlan;
	private ZonedDateTime createdDate;
	private ZonedDateTime updatedDate;
	private String organization;
	private String description;
	private String totalMonthlyCost;
	
}
