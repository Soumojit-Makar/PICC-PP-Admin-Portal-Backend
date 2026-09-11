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
@Table(name = "nnp_env_activity_log")
@Getter
@Setter
public class EnvActivityLogV2 implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "act_id", nullable = false)
	private String actId;

	@Column(name = "env_id", nullable = false)
	private String envId;
	@Column(name = "act_date")
	private LocalDateTime actDate;
	@Column(name = "act_desc")
	private String actDesc;
	@Column(name = "act_status")
	private String actStatus;
	@Column(name = "act_note")
	private String actNote;
	@Column(name = "user_id")
	private String userId;

}
