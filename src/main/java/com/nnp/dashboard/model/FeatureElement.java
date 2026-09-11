
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
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

@Table(name = "nnp_env_fea_elem")
@Getter @Setter 
public class FeatureElement implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id

	@Column(name = "elem_id", nullable = false)
	private String elementId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "fea_id", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private EnvFeature feature;

	@Column(name = "elem_name", nullable = false)
	private String elementName;

	@Column(name = "elem_type", nullable = false)
	private String elementType;

	@Column(name = "elem_desc")
	private String elementDesc;

	@Column(name = "elem_page")
	private String elementPage;

	@Column(name = "iimp_env_fea_elem_seq")
	private String feaSeq;

	@OneToMany(mappedBy = "feaElement", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<ElementDetail> elementDetails = new ArrayList<ElementDetail>();
	
	@OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name = "elem_id")
	private List<EnvUserAccess> userList = new ArrayList<EnvUserAccess>();
	
	@Transient
	private boolean isAssigned = false;

}
