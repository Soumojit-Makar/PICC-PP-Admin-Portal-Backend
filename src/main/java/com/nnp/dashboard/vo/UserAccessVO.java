package com.nnp.dashboard.vo;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserAccessVO implements Serializable {
	private String userAccountId;
	private String userId;
	private String userEmail;
	private String contactNo;
	private String password;
	private String firstName;
	private String lastName;
	
	/*
	 * private List<EnvironmentVO> envList = new ArrayList<>(); private
	 * List<EnvFeatureVO> featureList = new ArrayList<>(); private
	 * List<FeatureElementVO> feaElemList = new ArrayList<>(); private
	 * List<ElementDetailVO> elemDeatilId = new ArrayList<>();
	 */
	@Override
	public boolean equals(Object obj) {
		
		return this.userId.equalsIgnoreCase(((UserAccessVO)obj).userId);
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return this.userId.hashCode();
	}
	
	
}
