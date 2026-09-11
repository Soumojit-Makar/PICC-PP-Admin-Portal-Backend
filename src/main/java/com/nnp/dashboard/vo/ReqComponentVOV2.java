package com.nnp.dashboard.vo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.nnp.dashboard.model.ReqSpec;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReqComponentVOV2 {
	private String reqCompId;
	private Timestamp compComDT;
	private String compLink;
	private Timestamp compReSubDT;
	private String compStatDtl;
	private String compStatus;
	private String compId;
	private List<ReqSpecVOV2> reqSpec = new ArrayList<ReqSpecVOV2>();
}
