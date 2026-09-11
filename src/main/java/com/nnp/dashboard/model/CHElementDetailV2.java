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

@Table(name = "nnp_env_fea_elem_dtl_spec")
@Getter
@Setter
@ToString
public class CHElementDetailV2 implements Serializable {


    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "dtlspec_id", nullable = false)
    private String chElementDtlId;

    @Column(name = "elmdtl_id", nullable = false)
    private String elementDtlId;

    @Column(name = "spec_name", nullable = false)
    private String elementDtlName;

    @Column(name = "spec_shortname")
    private String elementDtlShortName;

    @Column(name = "spec_type")
    private String elementDtlType;

    @Column(name = "spec_home")
    private String elementDtlHome;

    @Column(name = "spec_desc")
    private String elementDtlDesc;

    @Column(name = "spec_url")
    private String elementDtlURL;

    @Column(name = "spec_fatno")
    private String elementDtlFatNo;

    @Column(name = "spec_param1")
    private String demoUrl;

    @Column(name = "spec_param2")
    private String componenetType;

    @Column(name = "env_fea_elem_dtl_seq")
    private String elementDtlSeq;
    @Transient
    private boolean isAssigned = false;


}
