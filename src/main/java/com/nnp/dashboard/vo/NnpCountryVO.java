package com.nnp.dashboard.vo;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NnpCountryVO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String countryId;

	private String taxCountry;

	private String taxType;

	private String taxPct;

	private String currency;

	private String taxComment;

	private String setupCharge;

	private String countryCode;

	private Boolean active;

	private String createdBy;

	private String modifiedBy;

	private OffsetDateTime createdOn;

	private OffsetDateTime modifiedOn;

	@JsonIgnore
	private List<PlanVOV2> nnpPlans = new ArrayList<PlanVOV2>();

	@JsonIgnore
	private List<NNPAccountVO> nnpAccounts = new ArrayList<NNPAccountVO>();

}
