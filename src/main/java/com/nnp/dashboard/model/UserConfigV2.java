package com.nnp.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "nnp_env_user_access")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserConfigV2 implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "usracc_id", nullable = false)
	private String userAccessId;

	@Column(name = "user_id", nullable = false)
	private String userId;
	@Column(name = "env_id", nullable = false)
	private String envId;
	@Column(name = "dtlspec_id", nullable = false)
	private String chElmDetailId;
}
