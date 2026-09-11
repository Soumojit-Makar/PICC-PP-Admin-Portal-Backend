package com.nnp.dashboard.vo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EnvRequestVOV2 {
	private String reqId;
	private Timestamp reqCapDT;
	private Timestamp reqComDT;
	private String reqDtl;
	private Timestamp reqReSubDT;
	private String envId;
	private String reqStatDtl;
	private String reqStatus;
	private String reqTitle;
	private long planId;
	private List<ReqComponentVOV2> reqCompVOV2 = new ArrayList<ReqComponentVOV2>();
}
