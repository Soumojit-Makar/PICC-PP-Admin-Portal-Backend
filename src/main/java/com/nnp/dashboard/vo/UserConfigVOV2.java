package com.nnp.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
public class UserConfigVOV2 implements Serializable {
	private String userAccessId;
	private String userId;
	private String envId;
	private String chElmDetailId;

}
