package com.nnp.dashboard.model;

import java.io.Serializable;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

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

/**
 * BBCompTemplate.java
 *
 * Maps the {@code env_comp_template} table. A component (env_bbcomp) can have
 * multiple YAML templates attached. Each template is rendered by the
 * env-replication-engine at environment creation time.
 *
 * @author AC
 */
@Entity
@Table(name = "env_comp_template")
@Getter
@Setter
public class BBCompTemplate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "template_id")
	private String tmplId;

	@Column(name = "comp_tmplate")
	private String template;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "file_path")
	private String filePath;

	@Column(name = "template_type")
	private String tmplType;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "status")
	private String status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "env_compid", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private BBComponent bbComponent;

}
