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
 */

@Entity

@Table(name = "nnp_env_fea_elem")
@Getter
@Setter
@ToString
public class FeatureElementV3 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id

    @Column(name = "elem_id", nullable = false)
    private String elementId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "fea_id", nullable = false)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	private EnvFeatureV3 feature;

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
	private List<ElementDetailV3> elementDetails = new ArrayList<>();

    @Transient
    private boolean isAssigned = false;

}
