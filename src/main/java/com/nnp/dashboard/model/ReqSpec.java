package com.nnp.dashboard.model;

import java.io.Serializable;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The persistent class for the env_reqspec database table.
 * 
 * @author AC
 */
@Entity
@Table(name = "env_reqspec")
@Getter
@Setter
public class ReqSpec implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "env_reqspecid")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "env_generic_id_seq")
    @SequenceGenerator(name = "env_generic_id_seq", sequenceName = "env_generic_id_seq", allocationSize = 1)
	private long reqSpecId;

	@Column(name = "env_specvalue")
	private String specValue;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "env_reqcompid", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private ReqComponent reqComponent;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)

	@JoinColumn(name = "env_specid", nullable = false)

	@LazyToOne(LazyToOneOption.NO_PROXY)
	private BBCompSpec bbCompSpec;

}