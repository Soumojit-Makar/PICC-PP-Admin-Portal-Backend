package com.nnp.dashboard.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nnp_acc_bill_ln", schema = "portal")
@Getter
@Setter
public class NnpAccBillLn implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "acc_bill_ln_id", nullable = false)
	private String accBillLnId;

	@Column(name = "acc_bill_ln_amount")
	private float accBillLnAmount;

	//@Column(name = "acc_call")
	//private int accCall;

	//@Column(name = "acc_vol")
	//private int accVol;

	@Column(name = "acc_bill_ln_dt")
	private LocalDateTime accBillLnDt;

	@Column(name = "token_cost", precision = 10, scale = 4)
	private java.math.BigDecimal tokenCost;

	@Column(name = "comp_cost", precision = 10, scale = 4)
	private java.math.BigDecimal compCost;

	@Column(name = "acc_comp_charge")
	private String accCompCharge;

	@Column(name = "active_comp_count")
	private Integer activeCompCount;

	@ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL,optional = false)
	@JoinColumn(name = "acc_bill_id")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"accBillLns", "accBillCols"})
	private NnpAccBill accBill;
	
	//@OneToOne(cascade = CascadeType.ALL)
	//@JoinColumn(name = "acc_plan_comp_id")
	//private NnpAccountPlanComp nnpAccPlComp;
		
}
