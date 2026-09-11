package com.nnp.dashboard.model;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nnp_account_plan"/* , schema = "portal" */)
@Getter
@Setter
public class NnpAccountPlan implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "acc_plan_id", nullable = false)
    private String accPlanId;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL,optional = false)
    @JoinColumn(name = "acc_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"accBills", "accountPlans", "accSupports"})
    private NnpAccount nnpAccount;

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL,optional=false)
    @JoinColumn(name = "plan_id", nullable = false)
    private HostPlan nnpPlan;

    @Column(name = "base_dcnt", length = 255)
    private BigDecimal baseDcnt;

    @Column(name = "acc_plan_status", length = 255)
    private String accPlanStatus;

    @Column(name = "plan_st_dt")
    private ZonedDateTime  planStDt;

    @Column(name = "plan_en_dt")
    private ZonedDateTime  planEnDt;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "created_on")
    private ZonedDateTime  createdOn;

    @Column(name = "modified_on")
    private ZonedDateTime  modifiedOn;

    @OneToMany(mappedBy = "nnpAccountPlan",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("nnpAccountPlan")
    private List<NnpAccountPlanComp> accountPlanComps=new ArrayList<NnpAccountPlanComp>();
   
}
