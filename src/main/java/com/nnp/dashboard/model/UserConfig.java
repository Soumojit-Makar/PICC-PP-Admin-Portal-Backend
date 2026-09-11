package com.nnp.dashboard.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nnp_env_user_access")
@Getter @Setter
public class UserConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "usracc_id", nullable = false)
	private String userAccountId;

	@Column(name = "user_id", nullable = false)
	private String userId;
	@Column(name = "env_id")
	private String envId;
	@Column(name = "fea_id")
	private String feaId;
	@Column(name = "elem_id")
	private String elemId;
	@Column(name = "elmdtl_id")
	private String elmDetailId;
	@Column(name = "chelmdtl_id")
	private String chElmDetailId;
}
