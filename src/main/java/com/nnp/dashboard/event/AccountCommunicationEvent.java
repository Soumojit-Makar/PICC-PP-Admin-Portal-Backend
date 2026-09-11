package com.nnp.dashboard.event;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import lombok.Data;

/**
 * AccountCommunicationEvent.java
 *
 * Application event payload used to persist a "communication" record against an
 * account (it is mapped to the NNP_ACC_COMM entity by
 * {@link com.nnp.dashboard.event.listener.AccActionEventListener}).
 *
 * It is used to build an audit trail of the account / environment / user
 * provisioning lifecycle (registration, payment captured, environment created,
 * user activated, etc.).
 */
@Data
public class AccountCommunicationEvent{

	
	/* private String accCommId; */
	private String accId;
	private String commType;
	private ZonedDateTime commDate;
	private String commCategory;
	private String commTitle;
	private String commDescription;
	private String commComments;
	private String createdBy;
	private ZonedDateTime createdOn;
	private String modifiedBy;
	private ZonedDateTime modifiedOn;

}
