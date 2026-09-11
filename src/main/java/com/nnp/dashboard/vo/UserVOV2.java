package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVOV2 implements Serializable {

	private String userId;
	private String envId;
	private String roleId;
	private String accId;
	private String firstName;
	private String lastName;
	private String emailId;
	@ToString.Exclude
	private String password;
	private String contactNumber;
	private LocalDateTime requestDate;
	private LocalDateTime updateDate;
	private String updateComment;
	private String userType;
	private String userStatus;

	@Override
	public boolean equals(Object obj) {
		
		return this.userId.equalsIgnoreCase(((UserVOV2)obj).userId);
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return this.userId.hashCode();
	}
	
	
}
