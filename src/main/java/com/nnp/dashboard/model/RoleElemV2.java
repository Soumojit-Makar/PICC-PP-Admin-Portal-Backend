package com.nnp.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "nnp_user_role_elem")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class RoleElemV2 implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "role_elemid", nullable = false)
	private String roleElemId;

	@Column(name = "role_id", nullable = false)
	private String roleId;
	@Column(name = "dtlspec_id", nullable = false)
	private String chElmDetailId;
	@Column(name = "env_id", nullable = false)
	private String envId;

}
