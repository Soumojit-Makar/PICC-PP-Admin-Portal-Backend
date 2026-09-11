package com.nnp.dashboard.event.listener;

import java.time.ZoneOffset;
import java.util.Objects;

import com.nnp.dashboard.utils.Utils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.nnp.dashboard.event.AccountCommunicationEvent;
import com.nnp.dashboard.event.UserActionCommunicationEvent;
import com.nnp.dashboard.model.NnpAccComm;
import com.nnp.dashboard.model.NnpAccount;
import com.nnp.dashboard.model.UserV2;
import com.nnp.dashboard.repo.IDRepo;
import com.nnp.dashboard.repo.NnpAccCommunicationRepo;
import com.nnp.dashboard.repo.NnpAccountRepo;
import com.nnp.dashboard.service.NNPAccountService;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener that turns {@link AccountCommunicationEvent}s (account / user action
 * events) into persistent {@link NnpAccComm} audit rows.
 *
 * The listener:
 *   - maps the event to the NNP_ACC_COMM entity,
 *   - assigns the primary key via the sequence table (ACC_COMM_&lt;seq&gt;),
 *   - links the row to the owning account and defaults {@code createdBy} to
 *     the account creator when the event does not carry it,
 *   - flushes the row to the DB.
 *
 * Additionally exposes factory helpers ({@code createAccActivityEvent} /
 * {@code createAccUserActivityEvent}) used by the account / user services to
 * produce events with the correct title/description/owner association.
 */
@Component
@Slf4j
public class AccActionEventListener {
	@Autowired
	private ModelMapper mapper;
	
	@Autowired
	private NnpAccountRepo nnpAccRepo;
	
	@Autowired
	private NnpAccCommunicationRepo accCommRepo;
	
	@Autowired
	private IDRepo idRepo;
	
	@EventListener
	public void handleComminicationEvent(AccountCommunicationEvent accCommEvent) {
		NnpAccComm communication = mapper.map(accCommEvent, NnpAccComm.class);
		communication.setAccCommId("ACC_COMM_"+idRepo.getNextSeqVal());
		
		communication.setNnpAccount(nnpAccRepo.findById(accCommEvent.getAccId()).get());
		if(Objects.isNull(communication.getCreatedBy()) || communication.getCreatedBy().isBlank()) {
			communication.setCreatedBy(communication.getNnpAccount().getCreatedBy());
		}
		accCommRepo.saveAndFlush(communication);
		log.info("Communication saved with title {} for account {} ", Utils.sanitizeForLog(communication.getCommTitle()), Utils.sanitizeForLog(accCommEvent.getAccId()));
	}

	public AccountCommunicationEvent createAccActivityEvent(NnpAccount nnpAcc, String commtTitle, String comments, String commDescription) {
		AccountCommunicationEvent accCommEvent = new AccountCommunicationEvent();
		//accCommEvent.setAccCommId("ACC_COMM_"+idRepo.getNextSeqVal());
		accCommEvent.setCommTitle(commtTitle +" - " +nnpAcc.getAccId());
		accCommEvent.setCommComments(comments +" - "+nnpAcc.getAccName());
		accCommEvent.setCommDescription(commDescription+" - "+nnpAcc.getAccName());
		accCommEvent.setCommDate(nnpAcc.getCreatedOn());
		accCommEvent.setCreatedBy(nnpAcc.getCreatedBy());
		accCommEvent.setCreatedOn(nnpAcc.getCreatedOn());
		accCommEvent.setAccId(nnpAcc.getAccId());
		
		return accCommEvent;
	}

	public AccountCommunicationEvent createAccUserActivityEvent(UserV2 user, String commtTitle, String comments, String commDescription) {
		UserActionCommunicationEvent userEvent = new UserActionCommunicationEvent();
		//accCommEvent.setAccCommId("ACC_COMM_"+idRepo.getNextSeqVal());
		userEvent.setCommTitle(commtTitle +" - " +user.getAccId());
		userEvent.setCommComments(comments);
		userEvent.setCommDescription(commDescription);
		userEvent.setCommDate(user.getRequestDate().atZone(ZoneOffset.UTC));
		userEvent.setCreatedOn(user.getRequestDate().atZone(ZoneOffset.UTC));
		userEvent.setAccId(user.getAccId());
		
		return userEvent;
	}

}
