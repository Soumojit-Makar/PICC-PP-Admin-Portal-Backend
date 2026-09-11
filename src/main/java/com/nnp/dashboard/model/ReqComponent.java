package com.nnp.dashboard.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

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
 * The persistent class for the env_reqcomp database table.
 * 
 * @author AC
 */
@Entity
@Table(name = "env_reqcomp")
@Getter
@Setter
public class ReqComponent implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "env_generic_id_seq")
    @SequenceGenerator(name = "env_generic_id_seq", sequenceName = "env_generic_id_seq", allocationSize = 1)
	@Column(name = "env_reqcompid")
	private long reqCompId;

	@Column(name = "env_compcomdt")
	private Timestamp compComDT;

	@Column(name = "env_complink")
	private String compLink;

	@Column(name = "env_compresubdt")
	private Timestamp compReSubDT;

	@Column(name = "env_compstatdtl")
	private String compStatDtl;

	@Column(name = "env_compstatus")
	private String compStatus;

	@OneToMany(mappedBy = "reqComponent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<ReqSpec> reqSpec = new ArrayList<ReqSpec>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "env_reqid", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private Request request;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)

	@JoinColumn(name = "env_compid", nullable = false)

	@LazyToOne(LazyToOneOption.NO_PROXY)
	private BBComponent bbComponent;

}