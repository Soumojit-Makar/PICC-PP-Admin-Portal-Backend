package com.nnp.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "nnp_domain_value")
@Getter
@Setter
public class DomainValueV2 implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "dv_id", nullable = false)
	private String dvId;

	@Column(name = "domain_name")
	private String domainName;
	@Column(name = "domain_value")
	private String domainValue;
}
