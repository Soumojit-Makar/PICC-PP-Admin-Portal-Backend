package com.nnp.dashboard.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nnp_account", schema = "portal")
@Getter
@Setter
public class NnpAccount implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "acc_id")
	private String accId;

	@SuppressWarnings("deprecation")
	@ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL,optional = false)
	@JoinColumn(name = "country_id")
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private NnpCountry nnpCountry;
	@Column(name = "env_id")
	private String env;
	@Column(name = "acc_name")
	private String accName;

	@Column(name = "acc_category", length = 255)
	private String accCategory;

	@Column(name = "acc_status", length = 255)
	private String accStatus;
	@Column(name = "created_by", length = 255)
	private String createdBy;

	@Column(name = "modified_by", length = 255)
	private String modifiedBy;

	@Column(name = "created_on")
	private ZonedDateTime createdOn;

	@Column(name = "modified_on")
	private ZonedDateTime modifiedOn;
	
	@Column(name = "organization")
	private String organization;
	
	@Column(name = "description")
	private String description;
	@OneToMany(mappedBy = "nnpAccount",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("nnpAccount")
	private List<NnpAccBill> accBills=new ArrayList<NnpAccBill>();
	
	@OneToMany(mappedBy = "nnpAccount",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("nnpAccount")
	private List<NnpAccountPlan> accountPlans=new ArrayList<NnpAccountPlan>();

	@OneToMany(mappedBy = "nnpAccount",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("nnpAccount")
	private List<NnpAccSupport> accSupports=new ArrayList<NnpAccSupport>();

}
