package com.nnp.dashboard.vo.kc;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDetails {

	private UUID id;
	private Long createdTimestamp;
	private String username;
	private boolean enabled;
	private boolean toptp;
	private boolean emailVerified;
	private String firstName;
	private String lastName;
	private String email;
	private List<String> disableableCredentialTypes;
	private List<String> requiredActions;
	private Integer notBefore;
	private UserAccess access;
	private UserAttributes attributes;
	
}
