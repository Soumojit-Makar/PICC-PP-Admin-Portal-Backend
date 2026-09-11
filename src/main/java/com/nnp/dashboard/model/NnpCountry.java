package com.nnp.dashboard.model;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nnp_country", schema = "portal")
@Setter
@Getter
public class NnpCountry implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "country_id")
	private String countryId;

	@Column(name = "taxcountry")
	private String taxCountry;

	@Column(name = "taxtype")
	private String taxType;

	@Column(name = "taxpct")
	private String taxPct;

	@Column(name = "currency")
	private String currency;

	@Column(name = "taxcomment")
	private String taxComment;

	@Column(name = "setupcharge")
	private String setupCharge;

	@Column(name = "countrycode")
	private String countryCode;

	@Column(name = "active")
	private Boolean active;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "modified_by")
	private String modifiedBy;

	@Column(name = "created_on")
	private OffsetDateTime createdOn;

	@Column(name = "modified_on")
	private OffsetDateTime modifiedOn;
	
	@OneToMany(mappedBy = "nnpCountry",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("nnpCountry")
	private List<HostPlan> nnpPlans=new ArrayList<HostPlan>();
	
	/*@OneToMany(mappedBy = "nnpCountry",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	private List<NnpAccount> nnpAccounts=new ArrayList<NnpAccount>();*/
	

}
