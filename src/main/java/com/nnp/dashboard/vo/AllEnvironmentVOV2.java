package com.nnp.dashboard.vo;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AllEnvironmentVOV2 implements Serializable {

	private EnvironmentVOV2 environment;
	private UserVOV2 user;
	private EnvRequestVOV2 envReq;
	@Override
	public boolean equals(Object obj) {
		//return this.userId.equalsIgnoreCase(((UserVOV2)obj).userId);
		return (this.user.getUserId().equalsIgnoreCase(((AllEnvironmentVOV2)obj).user.getUserId()))
				&& (this.environment.getEnvId().equalsIgnoreCase(((AllEnvironmentVOV2)obj).environment.getEnvId()))
				&& (this.envReq.getReqId().equalsIgnoreCase(((AllEnvironmentVOV2)obj).envReq.getReqId()));
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		//return this.userId.hashCode();
		return (this.user.getUserId() + this.environment.getEnvId()).hashCode();
	}
	
}
