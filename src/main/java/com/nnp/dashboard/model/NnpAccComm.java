package com.nnp.dashboard.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nnp_acc_comm", schema = "portal")
@Getter
@Setter
public class NnpAccComm implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "acc_comm_id")
	private String accCommId;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "acc_id")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"accBills", "accountPlans", "accSupports"})
	private NnpAccount nnpAccount;

	@Column(name = "comm_type")
	private String commType;

	@Column(name = "comm_date")
	private ZonedDateTime commDate;

	@Column(name = "comm_category")
	private String commCategory;

	@Column(name = "comm_title")
	private String commTitle;

	@Column(name = "comm_description")
	private String commDescription;

	@Column(name = "comm_file_links")
	private String commFileLinks;

	@Column(name = "comm_commnets")
	private String commComments;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "modified_by")
	private String modifiedBy;

	@Column(name = "created_on")
	private ZonedDateTime createdOn;

	@Column(name = "modified_on")
	private ZonedDateTime modifiedOn;

}
