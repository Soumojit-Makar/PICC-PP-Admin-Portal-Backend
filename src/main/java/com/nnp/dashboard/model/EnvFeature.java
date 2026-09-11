
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

@Table(name = "nnp_env_features")
@Getter @Setter
//@ToString
public class EnvFeature implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "fea_id", nullable = false)
	private String feaId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "env_id", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY) // @JsonIgnoreProperties("envFeatures")
	private Environment env;

	@Column(name = "fea_name", nullable = false)
	private String feaName;

	@Column(name = "fea_type", nullable = false)
	private String feaType;

	@Column(name = "fea_desc", nullable = false)
	private String feaDesc;

	@Column(name = "env_features_seq")
	private String feaSeq;

	@OneToMany(mappedBy = "feature", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<FeatureElement> featureElements = new ArrayList<FeatureElement>();
	
	@OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name = "fea_id")
	private List<EnvUserAccess> userList = new ArrayList<EnvUserAccess>();
	
	@Transient
	private boolean isAssigned = false;

}
