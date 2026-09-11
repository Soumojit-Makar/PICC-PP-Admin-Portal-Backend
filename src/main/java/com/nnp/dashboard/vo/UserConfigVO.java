package com.nnp.dashboard.vo;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserConfigVO extends UserAccessVO implements Serializable {
	private String envId;
	private String envCode;
	private String feaId;
	private String elemId;
	private String elmDetailId;
	private String chElmDetailId;

}
