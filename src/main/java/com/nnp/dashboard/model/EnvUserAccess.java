
package com.nnp.dashboard.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * @author AC
 *
 */

@Entity

@Table(name = "nnp_env_user_access")
@Getter @Setter 
public class EnvUserAccess implements Serializable {
	private static final long serialVersionUID = -7501911745301931226L;

	@Id

	@Column(name = "usracc_id", nullable = false)
	private String userAccountId;

	@Column(name = "user_id", nullable = false)
	private String userId;
}
