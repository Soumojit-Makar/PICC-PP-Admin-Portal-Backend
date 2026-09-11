package com.nnp.dashboard.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.Type;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The persistent class for the env_req database table.
 * @author AC
 *
 */
@Entity
@Table(name = "env_req")
@Getter @Setter
public class Request implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "env_generic_id_seq")
    @SequenceGenerator(name = "env_generic_id_seq", sequenceName = "env_generic_id_seq", allocationSize = 1)
	@Column(name = "env_reqid")
	
	private long reqId;

	@Column(name = "env_reqcapdt")
	private Timestamp reqCapDT;

	@Column(name = "env_reqcomdt")
	private Timestamp reqComDT;

	@Column(name = "env_reqdtl")
	private String reqDtl;

	/*
	 * @Column(name = "env_reqproj") private String reqProj;
	 */

	@Column(name = "env_reqresubdt")
	private Timestamp reqReSubDT;

	@Column(name = "env_id")
	private String envId;

	@Column(name = "env_reqstatdtl")
	private String reqStatDtl;

	@Column(name = "env_reqstatus")
	private String reqStatus;

	@Column(name = "env_reqtitle")
	private String reqTitle;
	
	@Column(name = "env_hostplanid")
	private long planId;
	@OneToMany(mappedBy = "request", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<ReqComponent> ReqComp = new ArrayList<ReqComponent>();
	@OneToMany(mappedBy = "envReq", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<ReqCommunication> reqCommunication = new ArrayList<ReqCommunication>();

	

	
}