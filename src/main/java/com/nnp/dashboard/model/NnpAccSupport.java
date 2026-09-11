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
@Table(name = "nnp_acc_support", schema = "portal")
@Getter
@Setter
public class NnpAccSupport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "sup_tkt_id", nullable = false)
    private String supTktId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "acc_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"accBills", "accountPlans", "accSupports"})
    private NnpAccount nnpAccount;

    @Column(name = "tkt_dt")
    private ZonedDateTime tktDt;

    @Column(name = "tkt_res_dt")
    private ZonedDateTime tktResDt;

    @Column(name = "tkt_category")
    private String tktCategory;

    @Column(name = "tkt_priority")
    private String tktPriority;

    @Column(name = "tkt_start")
    private ZonedDateTime tktStart;

    @Column(name = "tkt_title")
    private String tktTitle;

}
