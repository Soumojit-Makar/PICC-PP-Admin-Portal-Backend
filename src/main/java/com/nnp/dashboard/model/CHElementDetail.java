package com.nnp.dashboard.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import lombok.Getter;
import lombok.Setter;

/**
 * @author AC
 *
 */

@Entity

@Table(name = "IIMP_ENV_FEA_CH_ELEM_DTL")
@Getter @Setter 
public class CHElementDetail implements Serializable{
	

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "CHELMDTL_ID", nullable = false)
	private String chElementDtlId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "ELMDTL_ID", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private ElementDetail prElemDtl;
	
	@Column(name = "elmdtl_name", nullable = false)
	private String elementDtlName;

	@Column(name = "elmdtl_type", nullable = false)
	private String elementDtlType;

	@Column(name = "elmdtl_home", nullable = false)
	private String elementDtlHome;

	@Column(name = "elmdtl_desc")
	private String elementDtlDesc;

	@Column(name = "elmdtl_url")
	private String elementDtlURL;

	@Column(name = "elmdtl_fatno")
	private String elementDtlFatNo;
	
	@Column(name = "elem_dtl_param1")
	private String demoUrl;
	
	@Column(name = "elem_dtl_param2")
	private String homeIcon;

	@Column(name = "env_fea_elem_dtl_seq")
	private String elementDtlSeq;
	
	@OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name = "CHELMDTL_ID")
	private List<EnvUserAccess> userList = new ArrayList<EnvUserAccess>();
	
	@Transient
	private boolean isAssigned = false;


}
