package com.nnp.dashboard.model;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
 * @author AC
 */

@Entity
@Table(name = "nnp_plan_comp")
@Getter
@Setter
public class HostPlanComp {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cn_hosting_generic_id_seq")
    @SequenceGenerator(name = "cn_hosting_generic_id_seq", sequenceName = "cn_hosting_generic_id_seq", allocationSize = 1)
    @Column(name = "host_regplid")
    private long hostRegPlanId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "env_compid", nullable = false)
    @LazyToOne(LazyToOneOption.NO_PROXY)
    @JsonBackReference
    private BBComponent envBBComp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "host_plid", nullable = false)
    @LazyToOne(LazyToOneOption.NO_PROXY)
    @JsonBackReference
    private HostPlan hostPlan;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "comp_group_id")
    @LazyToOne(LazyToOneOption.NO_PROXY)
    @JsonBackReference
    private NNPPlanCompGroup planCompGroup;

    @Column(name = "host_plcmptype")
    private String hostPlanCompType;

    @Column(name = "host_basedaypr")
    private String hostBaseMNPr;

    @jakarta.persistence.Transient
    private String hostPlanCompStatus = "Active";

}
