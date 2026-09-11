package com.nnp.dashboard.model;

import java.io.Serializable;
import java.time.LocalDateTime;
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
@Table(name = "nnp_acc_bill")
@Setter
@Getter
public class NnpAccBill implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "acc_bill_id", nullable = false)
	private String accBillId;

	@Column(name = "acc_bill_adj_amount")
	private Float accBillAdjAmount;

	@Column(name = "acc_bill_amount")
	private Float accBillAmount;

	@Column(name = "acc_bill_comment")
	private String accBillComment;

	@Column(name = "acc_bill_contact")
	private String accBillContact;

	@Column(name = "acc_bill_dt")
	private ZonedDateTime accBillDt;

	@Column(name = "acc_bill_open_bal")
	private Float accBillOpenBal;

	/*@Column(name = "acc_bill_pay_dt")
	private LocalDateTime accBillPayDt;*/

	@Column(name = "acc_bill_status")
	private String accBillStatus;

	@Column(name = "acc_bill_tax_pct")
	private Float accBillTaxPct;

	@Column(name = "bill_period_start")
	private java.time.LocalDate billPeriodStart;

	@Column(name = "bill_period_end")
	private java.time.LocalDate billPeriodEnd;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "acc_id")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"accBills", "accountPlans", "accSupports"})
	private NnpAccount nnpAccount;
	
	@OneToMany(mappedBy = "accBill",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("accBill")
	private List<NnpAccBillLn> accBillLns=new ArrayList<NnpAccBillLn>();
	
	@OneToMany(mappedBy = "accBill",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("accBill")
	private List<NnpAccBillCol> accBillCols=new ArrayList<NnpAccBillCol>();

}
