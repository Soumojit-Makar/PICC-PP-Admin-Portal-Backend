package com.nnp.dashboard.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NnpAccBillColVO {
	private String accId;
	private String accName;
	private String accBillId;
	private String payDt;
	private float payAmount;
	private String payMode;
	private String payRef;
	private String paymentNotes;
	@JsonProperty("isPaymentSuccess")
	private boolean paymentSuccess;

}
