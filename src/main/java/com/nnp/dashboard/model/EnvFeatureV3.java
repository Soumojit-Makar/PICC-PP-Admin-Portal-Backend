
package com.nnp.dashboard.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author AC
 *
 */

@Entity

@Table(name = "nnp_env_features")
@Getter @Setter
@ToString
public class EnvFeatureV3 implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "fea_id", nullable = false)
	private String feaId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "env_id", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY) // @JsonIgnoreProperties("envFeatures")
	private EnvironmentV3 env;
	@Column(name = "fea_name", nullable = false)
	private String feaName;

	@Column(name = "fea_type", nullable = false)
	private String feaType;

	@Column(name = "fea_desc", nullable = false)
	private String feaDesc;

	@Column(name = "env_features_seq")
	private String feaSeq;

	@OneToMany(mappedBy = "feature", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<FeatureElementV3> featureElements = new ArrayList<>();

	@Transient
	private boolean isAssigned = false;

}
