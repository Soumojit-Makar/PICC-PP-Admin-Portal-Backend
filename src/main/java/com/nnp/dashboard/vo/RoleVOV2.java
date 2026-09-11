package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleVOV2 implements Serializable {

	private String roleId;
	private String roleName;
	private String roleDescription;
	private String roleStatus;
	@Override
	public boolean equals(Object obj) {
		
		return this.roleId.equalsIgnoreCase(((RoleVOV2)obj).roleId);
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return this.roleId.hashCode();
	}
	
	
}
