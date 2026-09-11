
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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author AC
 *
 */

@Entity

@Table(name = "nnp_env_fea_elem_dtl")
@Getter @Setter 
public class ElementDetail implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "elmdtl_id", nullable = false)
	private String elementDtlId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "elem_id", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private FeatureElement feaElement;

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

	@Column(name = "env_fea_elem_dtl_seq")
	private String elementDtlSeq;

	
	@OneToMany(mappedBy = "prElemDtl", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<CHElementDetail> childElementDtls = new ArrayList<>();
	
	@OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name = "elmdtl_id")
	private List<EnvUserAccess> userList = new ArrayList<EnvUserAccess>();
	
	@Transient
	private boolean isAssigned = false;

}
