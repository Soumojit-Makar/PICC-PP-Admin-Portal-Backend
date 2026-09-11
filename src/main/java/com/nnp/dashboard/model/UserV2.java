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
@Table(name = "nnp_user")
@Getter
@Setter
public class UserV2 implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "user_id", nullable = false)
	private String userId;

	@Column(name = "env_id", nullable = false)
	private String envId;
	@Column(name = "role_id", nullable = false)
	private String roleId;
	@Column(name = "acc_id", nullable = false)
	private String accId;
	@Column(name = "f_name")
	private String firstName;
	@Column(name = "l_name")
	private String lastName;
	@Column(name = "email")
	private String emailId;
	@Column(name = "contact")
	private String contactNumber;
	@Column(name = "req_dt")
	private LocalDateTime requestDate;
	@Column(name = "udp_dt")
	private LocalDateTime updateDate;
	@Column(name = "udp_comm")
	private String updateComment;
	@Column(name = "user_type")
	private String userType;
	@Column(name = "user_status")
	private String userStatus;
	@Column(name = "env_param4")//temporary password capture
	private String env_param4;
	@Column(name = "address")
	private String address;

}
