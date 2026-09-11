package com.nnp.dashboard.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

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

@Entity
@Table(name = "nnp_account_plan_comp", schema = "portal")
@Getter
@Setter
public class NnpAccountPlanComp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "acc_plan_comp_id", nullable = false)
    private String accPlanCompId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "plan_comp_id")
    private HostPlanComp nnpPlanComp;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "acc_plan_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("accountPlanComps")
    private NnpAccountPlan nnpAccountPlan;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "created_on")
    private ZonedDateTime createdOn;

    @Column(name = "modified_on")
    private ZonedDateTime modifiedOn;

    
}
