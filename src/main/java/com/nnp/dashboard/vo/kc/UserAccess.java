package com.nnp.dashboard.vo.kc;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccess {

	private boolean manageGroupMembership;
	private boolean view;
	private boolean mapRoles;
	private boolean impersonate;
	private boolean manage;

}
