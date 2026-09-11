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
@Table(name = "nnp_acc_bill_col", schema = "portal")
@Getter
@Setter
public class NnpAccBillCol implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "acc_bill_col_id", nullable = false, length = 255)
	private String accBillColId;

	@Column(name = "pay_amount")
	private float payAmount;

	@Column(name = "pay_dt")
	private ZonedDateTime payDt;

	@Column(name = "pay_mode", length = 255)
	private String payMode;

	@Column(name = "pay_ref", length = 255)
	private String payRef;

	@ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL,optional = false)
	@JoinColumn(name = "acc_bill_id")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"accBillLns", "accBillCols"})
	private NnpAccBill accBill;
	
	@Column(name = "payment_notes")
	private String paymentNotes;

	
}
