
package com.nnp.dashboard.model;

import jakarta.persistence.*;
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
public class EnvFeatureV2 implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "fea_id", nullable = false)
	private String feaId;


	@Column(name = "env_id", nullable = false)
	private String envId;

	@Column(name = "fea_name", nullable = false)
	private String feaName;

	@Column(name = "fea_type", nullable = false)
	private String feaType;

	@Column(name = "fea_desc", nullable = false)
	private String feaDesc;

	@Column(name = "env_features_seq")
	private String feaSeq;

	@Transient
	private boolean isAssigned = false;

}
