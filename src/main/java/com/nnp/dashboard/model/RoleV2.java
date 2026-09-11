package com.nnp.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "nnp_user_role")
@Getter
@Setter
public class RoleV2 implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "role_id", nullable = false)
	private String roleId;

	@Column(name = "role_name")
	private String roleName;
	@Column(name = "role_desc")
	private String roleDescription;
	@Column(name = "role_stat")
	private String roleStatus;

}
