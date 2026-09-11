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

@Table(name = "nnp_env_fea_elem_dtl")
@Getter
@Setter
@ToString
public class ElementDetailV3 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id

    @Column(name = "elmdtl_id", nullable = false)
    private String elementDtlId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "elem_id", nullable = false)
    @LazyToOne(LazyToOneOption.NO_PROXY)
    private FeatureElementV3 feaElement;
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
    private List<CHElementDetailV3> childElementDtls = new ArrayList<>();



    @Transient
    private boolean isAssigned = false;

}
