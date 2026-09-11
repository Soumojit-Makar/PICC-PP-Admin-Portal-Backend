package com.nnp.dashboard.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nnp.dashboard.utils.Utils;
import com.nnp.dashboard.vo.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nnp.dashboard.event.listener.AccActionEventListener;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.model.BBCompSpec;
import com.nnp.dashboard.model.HostPlanComp;
import com.nnp.dashboard.model.NnpAccBill;
import com.nnp.dashboard.model.NnpAccBillCol;
import com.nnp.dashboard.model.NnpAccount;
import com.nnp.dashboard.model.NnpAccountPlan;
import com.nnp.dashboard.model.NnpAccountPlanComp;
import com.nnp.dashboard.repo.EnvTypeRepoV2;
import com.nnp.dashboard.repo.IDRepo;
import com.nnp.dashboard.repo.NnpAccBillRepo;
import com.nnp.dashboard.repo.NnpAccountRepo;
import com.nnp.dashboard.repo.NnpCountryRepository;
import com.nnp.dashboard.repo.PlanCompRepoV2;
import com.nnp.dashboard.repo.PlanRepoV2;

import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import com.nnp.dashboard.model.NnpAccComm;
import com.nnp.dashboard.repo.NnpAccCommunicationRepo;
import com.nnp.dashboard.repo.NnpAccountPlanCompRepo;

/**
 * Service responsible for the account (tenant) registration and payment flow.
 *
 * Responsibilities:
 *   - Persisting a new NNP account together with its selected plan and bill
 *   - Capturing the confirmed payment (paid / free) and, once confirmed,
 *     assembling the {@link AllEnvironmentVOV2} payload used to trigger
 *     full environment provisioning via {@link EnvWrapperService}
 *   - Uniqueness checks for account name / user id
 *   - Handling front-end pod-status and DMS widget payloads
 *
 * The payment-confirmation handlers build the environment, user and request
 * (component+spec) DTOs from the stored plan and delegate the actual
 * provisioning to {@link EnvWrapperService#captureEnvRequest}.
 */
@Service
@Slf4j
public class NNPAccountService {

	@Autowired
	private NnpCountryRepository countryRepo;

	@Autowired
	private NnpAccountRepo nnpAccRepo;

	@Autowired
	private PlanRepoV2 hostPlanRepo;

	@Autowired
	private PlanCompRepoV2 hostPlanCompRepo;

	@Autowired
	private ModelMapper mapper;

	@Autowired
	private IDRepo idRepo;

	@Autowired
	private UserAccessService userAccessServ;

	@Autowired
	private NnpAccBillRepo accBillRepo;

	@Autowired
	private EnvTypeRepoV2 envTypeRepo;

	@Autowired
	public ApplicationEventPublisher appEventPub;

	@Autowired
	private EnvWrapperService envWrapperServ;

	@Autowired
	private AccActionEventListener accCommListener;
	@Autowired
	private CachingService cachingService;
	@Autowired
	private NnpAccCommunicationRepo accCommRepo;

	@Autowired
	private NnpAccountPlanCompRepo accountPlanCompRepo;
	@Autowired
	private EnvActivityLogService envActivityLogService;
	@Value("${keycloak.realm.default:master}")
	private String realmForAccount;

	@Value("${env.type.default:standard}")
	private String envType;
	public List<NnpCountryVO> getAllCountry() {
		List<NnpCountryVO> allCountryList = new ArrayList();
		countryRepo.findAll().forEach(c -> {
			allCountryList.add(mapper.map(c, NnpCountryVO.class));
		});
		return allCountryList;
	}


	@Transactional
	public ResponseEntity<Map<String, String>> captureAccount(NNPAccountVO nnpAccVO) {

		checkAccountUnique(nnpAccVO);
//		log.info("New Account creation started as part of registration process");
		NnpAccount accModel = new NnpAccount();
		accModel.setAccId("ACC_" + idRepo.getNextSeqVal());
		accModel.setAccName(nnpAccVO.getAccName());
		accModel.setAccCategory(nnpAccVO.getOrgCategory());
		accModel.setAccStatus("Initiated");
		accModel.setNnpCountry(countryRepo.findByCountryCode(nnpAccVO.getCountryCode()).get());
		accModel.setCreatedOn(nnpAccVO.getCreatedDate());
		accModel.setOrganization(nnpAccVO.getOrganization());
		accModel.setDescription(nnpAccVO.getPlatformPurpose());
		accModel.setCreatedBy(nnpAccVO.getUser().getUserId());

		PlanVOV2 planVo = nnpAccVO.getSelectedPlan();
		NnpAccountPlan accPlan = new NnpAccountPlan();
		accPlan.setAccPlanId("ACC_PLAN_" + idRepo.getNextSeqVal());
		accPlan.setNnpPlan(hostPlanRepo.findById(planVo.getHostPlanid()).get());
		accPlan.setBaseDcnt(accPlan.getNnpPlan().getHostDefaultDct());// default discount of PLAN table should be copied
																		// to base discount of Account Plan
		// Never change this status, It is used to identify new account plan which is
		// getting registered and for which environment need to create
		// OTHERWISE LOGIC WILL FAIL FOR createEnvReq()
		accPlan.setAccPlanStatus("Initiated");
		accPlan.setActive(true);
		accPlan.setCreatedOn(nnpAccVO.getCreatedDate());
		accPlan.setPlanStDt(nnpAccVO.getCreatedDate().plusDays(2));// two days to activate the plan with environment
		accPlan.setPlanStDt(accPlan.getPlanStDt().plusDays(180));// Assuming plan valid for 180 days
		accPlan.setNnpAccount(accModel);
		accModel.getAccountPlans().add(accPlan);
//		log.info("Account Plan populated" + accPlan);

		// create Account Bill
		NnpAccBill accBill = new NnpAccBill();
		accBill.setAccBillId("ACC_BILL_" + idRepo.getNextSeqVal());
		accBill.setAccBillAmount(Float.parseFloat(nnpAccVO.getTotalMonthlyCost()));
		accBill.setAccBillAdjAmount(0.0f);
		accBill.setAccBillOpenBal(0.0f);
		accBill.setNnpAccount(accModel);

		NnpAccountPlanComp nnpAccPlanComp = null;
		for (CompVOV2 selectedPlCompVO : nnpAccVO.getSelectedPlan().getCompVOV2()) {
			nnpAccPlanComp = new NnpAccountPlanComp();
			nnpAccPlanComp.setAccPlanCompId("ACC_PLAN_COMP_" + idRepo.getNextSeqVal());
			nnpAccPlanComp.setNnpPlanComp(hostPlanCompRepo.findByEnvBBComp_compIdAndHostPlan_hostPlanid(
					selectedPlCompVO.getCompId(), planVo.getHostPlanid()));
			nnpAccPlanComp.setActive(true);
			nnpAccPlanComp.setCreatedOn(nnpAccVO.getCreatedDate());
			nnpAccPlanComp.setNnpAccountPlan(accPlan);
			accPlan.getAccountPlanComps().add(nnpAccPlanComp);
		}


		// create acc bill collection and add to Acc Bill
		/*
		 * NnpAccBillCol accBillCol = new NnpAccBillCol();
		 * accBillCol.setAccBillColId("ACC_BILL_COL_"+idRepo.getNextSeqVal());
		 * accBillCol.setAccBill(accBill);
		 * accBill.getAccBillCols().add(accBillCol);
		 */

		accModel.getAccBills().add(accBill);
//		log.info("Account Bill and Account Bill Ln populated" + accBill);

		accModel = nnpAccRepo.saveAndFlush(accModel);
//		log.info("Account saved successfully - " + accModel.getAccId());

		nnpAccVO.setAccId(accModel.getAccId());
		userAccessServ.captureNnpAccAdminUser(nnpAccVO);
		if (nnpAccVO.getUser() != null && nnpAccVO.getUser().getPassword() != null) {
			cachingService.savePassword(nnpAccVO.getAccName(), nnpAccVO.getUser().getPassword());
		}
//		log.info("User saved successfully - " + nnpAccVO.getUser().getUserId());

		// communication captured

		appEventPub.publishEvent(accCommListener.createAccActivityEvent(accModel, "Account Creation Registered", "",
				"A new Accont Creation Request Submitted for"));

		Map<String, String> response = new HashMap<String, String>();
		response.put("accId", accModel.getAccId());
		response.put("accName", accModel.getAccName());
		response.put("userId", nnpAccVO.getUser().getUserId());
		response.put("billId", accBill.getAccBillId());
//		log.info("Sending success status 201");
		return ResponseEntity.status(201).body(response);
	}

	private void checkAccountUnique(NNPAccountVO nnpAccVO) {
		UserVOV2 userVO = userAccessServ.getUserByUserIdV2(nnpAccVO.getUser().getUserId());
		if (userVO != null) {
//			log.info("User already exist. Thus throwing exception");
			NnpAccount acc = nnpAccRepo.findById(userVO.getAccId()).get();
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("401",
					"An account named as  " + acc.getAccName() + " already exist for the user " + userVO.getUserId()
							+ " with status as " + acc.getAccStatus()));
		}
	}

	public Boolean checkIfAccountExists(String accName) {
		return nnpAccRepo.findByAccName(accName).isPresent();
	}

	public Boolean checkIfUserExists(String userId) {
		return (userAccessServ.getUserByUserIdV2(userId) != null) ? true : false;
	}

	@Transactional
	public boolean capturePaymentTriggerAction(NnpAccBillColVO nnpAccBillColVO) {
		// get Account.
		NnpAccount account = nnpAccRepo.findByAccName(nnpAccBillColVO.getAccName())
				.orElseThrow(() -> new DashboardConfigException(new DashboardConfigExceptionMessage("500",
						"Account not exist in the NNP_ACCOUNT in database " + nnpAccBillColVO.getAccId())));

		if (!nnpAccBillColVO.isPaymentSuccess()) {
//			log.info("Payment failed for the account acc id - " + nnpAccBillColVO.getAccId());
//			log.info("Deleting the account as user has to register from begining");

			nnpAccRepo.deleteByAccount(account.getAccId());

			return false;
		}
		NnpAccBillCol accBillCollection = mapper.map(nnpAccBillColVO, NnpAccBillCol.class);
		NnpAccBill accBill = accBillRepo.findById(nnpAccBillColVO.getAccBillId())
				.orElseThrow(() -> new DashboardConfigException(
						new DashboardConfigExceptionMessage("500", "No record in Bill table for the Bill id"
								+ nnpAccBillColVO.getAccBillId() + "& account id" + nnpAccBillColVO.getAccId())));

		// Making BillAmount 0.0 and keep the paid amount as open balance
		accBill.setAccBillOpenBal(accBill.getAccBillAmount());
		accBill.setAccBillAmount(0.0f);
		accBill.setAccBillDt(ZonedDateTime.parse(nnpAccBillColVO.getPayDt()));
		accBill.setAccBillStatus("active");
		accBill.setAccBillTaxPct(Float.parseFloat(account.getNnpCountry().getTaxPct()));

		//
		// Collection table will not ne populated from here - IS THIS CORRECT? I am
		// populating it.
		// Collection table will be populated from billing engine at end end of first
		// payment cycle
		accBillCollection.setAccBillColId("ACC_BILL_COL_" + idRepo.getNextSeqVal());
		accBillCollection.setAccBill(accBill);
		accBillCollection.setPayDt(ZonedDateTime.parse(nnpAccBillColVO.getPayDt()));
		accBill.getAccBillCols().add(accBillCollection);
		accBillRepo.saveAndFlush(accBill);
//		log.info("Payment for the Account - {} captured against bill id - {} ", nnpAccBillColVO.getAccId(),
//				nnpAccBillColVO.getAccBillId()/* ,accBillCollection.getAccBillColId() */);
		// communication captured
		appEventPub.publishEvent(accCommListener.createAccActivityEvent(account, "Payment Captured for Account",
				"Payment Recorded for Account creation", "Payment Successfull and also recorded for Account creation"));

		AllEnvironmentVOV2 allEnvVOV2 = new AllEnvironmentVOV2();
		allEnvVOV2 = createEnvVO(allEnvVOV2, account);
		allEnvVOV2 = createUserVO(allEnvVOV2, account);
		allEnvVOV2 = createEnvReq(allEnvVOV2, account);
		String pwd = cachingService.retrieveAndDeletePassword(account.getAccName());
		allEnvVOV2.getUser().setPassword(pwd);
//		log.info("AllEnvVOV2 -" + allEnvVOV2);

		// communication captured
		appEventPub.publishEvent(accCommListener.createAccActivityEvent(account, "Environment creation initiated",
				"Environment creation initiated", "Environment creation initiated"));
		// call environment replication
		envWrapperServ.captureEnvRequest(allEnvVOV2, account);
		return true;

	}

	private AllEnvironmentVOV2 createEnvReq(AllEnvironmentVOV2 allEnvVOV2, NnpAccount account) {
		NnpAccountPlan accPlan = account.getAccountPlans().stream()
				.filter(ap -> "Initiated".equalsIgnoreCase(ap.getAccPlanStatus())).findFirst().get();
		EnvRequestVOV2 envReq = new EnvRequestVOV2();
		envReq.setPlanId(accPlan.getNnpPlan().getHostPlanid());
		envReq.setReqCapDT(Timestamp.from(Instant.now()));
		envReq.setReqDtl("This request is associated with Account - " + account.getAccName());
		envReq.setReqDtl(account.getDescription());
		envReq.setReqStatus("active");

		accPlan.getAccountPlanComps().forEach(accPlComp -> {
			ReqComponentVOV2 reqComp = createReqComp(accPlComp);
			envReq.getReqCompVOV2().add(reqComp);
		});
		allEnvVOV2.setEnvReq(envReq);
		return allEnvVOV2;
	}

	private ReqComponentVOV2 createReqComp(NnpAccountPlanComp accPlComp) {
		ReqComponentVOV2 reqComp = new ReqComponentVOV2();
		HostPlanComp hostPlanComp = accPlComp.getNnpPlanComp();
		reqComp.setCompId(hostPlanComp.getEnvBBComp().getCompId());
		reqComp.setCompStatus("active");
		hostPlanComp.getEnvBBComp().getBbCompSpec().forEach(bbCompSpec -> {
			ReqSpecVOV2 reqSpec = createReqSpec(bbCompSpec);
			reqComp.getReqSpec().add(reqSpec);
		});

		return reqComp;
	}

	private ReqSpecVOV2 createReqSpec(BBCompSpec bbCompSpec) {
		ReqSpecVOV2 reqSpec = new ReqSpecVOV2();
		reqSpec.setSpecid(bbCompSpec.getSpecid());
		reqSpec.setSpecValue(bbCompSpec.getSpecvalues());
		return reqSpec;
	}

	private AllEnvironmentVOV2 createUserVO(AllEnvironmentVOV2 allEnvVOV2, NnpAccount account) {
		UserVOV2 userVOV2 = userAccessServ.getUserByAccIdAndType(account.getAccId(), "admin");
		allEnvVOV2.setUser(userVOV2);
		return allEnvVOV2;
	}

	private AllEnvironmentVOV2 createEnvVO(AllEnvironmentVOV2 allEnvVOV2, NnpAccount account) {
		EnvironmentVOV2 envVOV2 = new EnvironmentVOV2();
		envVOV2.setEnvCode(account.getAccName());
		envVOV2.setEnvDesc(account.getDescription());
		envVOV2.setEnvName(account.getAccName());
		envVOV2.setEnvNamespace(account.getAccName().toLowerCase());
		envVOV2.setEnvRepo(account.getAccName());
		envVOV2.setEnvStatus("inactive");
		envVOV2.setEnvTenantId(realmForAccount);
		envVOV2.setEnvTypeId(envTypeRepo.findByEnvTypeName(envType).get(0).getEnvTypeId());
		allEnvVOV2.setEnvironment(envVOV2);
		return allEnvVOV2;
	}

	@Transactional
	public boolean capturePaymentTriggerActionFree(NnpAccBillColVO nnpAccBillColVO) {
		// get Account.
		NnpAccount account = nnpAccRepo.findByAccName(nnpAccBillColVO.getAccName())
				.orElseThrow(() -> new DashboardConfigException(new DashboardConfigExceptionMessage("500",
						"Account not exist in the NNP_ACCOUNT in database " + nnpAccBillColVO.getAccId())));
		NnpAccBillCol accBillCollection = mapper.map(nnpAccBillColVO, NnpAccBillCol.class);
		NnpAccBill accBill = accBillRepo.findById(nnpAccBillColVO.getAccBillId())
				.orElseThrow(() -> new DashboardConfigException(
						new DashboardConfigExceptionMessage("500", "No record in Bill table for the Bill id"
								+ nnpAccBillColVO.getAccBillId() + "& account id" + nnpAccBillColVO.getAccId())));

		// Making BillAmount 0.0 and keep the paid amount as open balance
		accBill.setAccBillOpenBal(0.0f);
		accBill.setAccBillAmount(0.0f);
		accBill.setAccBillDt(ZonedDateTime.now());
		accBill.setAccBillStatus("active");
		accBill.setAccBillTaxPct(Float.parseFloat(account.getNnpCountry().getTaxPct()));

		//
		// Collection table will not ne populated from here - IS THIS CORRECT? I am
		// populating it.
		// Collection table will be populated from billing engine at end end of first
		// payment cycle
		accBillCollection.setAccBillColId("ACC_BILL_COL_" + idRepo.getNextSeqVal());
		accBillCollection.setAccBill(accBill);
		accBillCollection.setPayDt(ZonedDateTime.parse(nnpAccBillColVO.getPayDt()));
		accBill.getAccBillCols().add(accBillCollection);
		accBillRepo.saveAndFlush(accBill);
//		log.info("Payment for the Account - {} captured against bill id - {} ", nnpAccBillColVO.getAccId(),
//				nnpAccBillColVO.getAccBillId() /* ,accBillCollection.getAccBillColId() */);
		// communication captured
		appEventPub.publishEvent(accCommListener.createAccActivityEvent(account, "Payment Captured for Account",
				"Payment Recorded for Account creation", "Payment Successfull and also recorded for Account creation"));

		AllEnvironmentVOV2 allEnvVOV2 = new AllEnvironmentVOV2();
		allEnvVOV2 = createEnvVO(allEnvVOV2, account);
		allEnvVOV2 = createUserVO(allEnvVOV2, account);
		allEnvVOV2 = createEnvReq(allEnvVOV2, account);
		String pwd = cachingService.retrieveAndDeletePassword(account.getAccName());
		allEnvVOV2.getUser().setPassword(pwd);
//		log.info("AllEnvVOV2 -" + allEnvVOV2);

		// communication captured
		appEventPub.publishEvent(accCommListener.createAccActivityEvent(account, "Environment creation initiated",
				"Environment creation initiated", "Environment creation initiated"));
		// call environment replication
		envWrapperServ.captureEnvRequest(allEnvVOV2, account);
		return true;

	}

	@Transactional
	public void checkPodsAndStoreCommunication(String envName, Map<String, Object> payload, String userName) {
		// 1. Look up the NnpAccount by account name (accName = envName)
		NnpAccount account = nnpAccRepo.findByAccName(envName)
				.orElseThrow(() -> new RuntimeException("Account not found: " + envName));
//		log.info("CheckPodsAndStoreCommunucation -> {}", payload);

		// 2. Get the pods list from the frontend payload
		Object podsRaw = payload.get("pods");
		List<Map<String, Object>> pods = new ArrayList<>();
		if (podsRaw instanceof List) {
			pods = (List<Map<String, Object>>) podsRaw;
		} else if (podsRaw instanceof Map) {
			Map<String, Object> podsMap = (Map<String, Object>) podsRaw;
			if (podsMap.get("podMetric") instanceof List) {
				pods = (List<Map<String, Object>>) podsMap.get("podMetric");
			} else if (podsMap.get("pods") instanceof List) {
				pods = (List<Map<String, Object>>) podsMap.get("pods");
			} else {
				pods.add(podsMap);
			}
		}

		if (pods == null || pods.isEmpty()) {
			log.error("No pods data received for account: {}", Utils.sanitizeForLog(envName));
			return;
		}

		// 3. Get all plan components for this account
		List<NnpAccountPlanComp> planComps = accountPlanCompRepo
				.findByNnpAccountPlan_NnpAccount_AccName(envName);

		for (Map<String, Object> pod : pods) {
//			log.info("{}:{}", pod.get("deploymentName"), pod.get("podStatus"));
			String podName = (String) pod.getOrDefault("deploymentName", pod.getOrDefault("podName", "unknown"));
			String status  = (String) pod.getOrDefault("podStatus", pod.getOrDefault("status", "unknown"));
			// 4. Save a communication record for each pod
			// Standard project activity event
			EnvActivityLogVOV2 logVo = EnvActivityLogVOV2.builder()
					.envId(account.getEnv() != null ? account.getEnv() : envName) // e.g. "MASTER"
					.actDesc("Pod status check for " + podName)
					.actStatus(status)
					.actNote("POD_STATUS_CHECK")
					.userId(userName)
					.build();
			envActivityLogService.logActivity(logVo);

//			log.info("Saved pod communication for pod: {} | status: {}", podName, status);

			// 5. Update NnpAccountPlanComp active flag based on pod status
			boolean isActive = "Running".equalsIgnoreCase(status);
			for (NnpAccountPlanComp planComp : planComps) {
				planComp.setActive(isActive);
				planComp.setModifiedBy(userName);
				planComp.setModifiedOn(ZonedDateTime.now());
				accountPlanCompRepo.save(planComp);
			}
		}

//		log.info("Completed pod status check and communication storage for account: {}", envName);
	}

	public void createDmsWidget(String envName, Map<String, Object> dmsPayload) {
		// Cache each field temporarily using existing CachingService
		// These are NOT stored in the database, only in memory cache
		cachingService.savePassword("dms-sshKey-" + envName, (String) dmsPayload.get("sshKey"));
		cachingService.savePassword("dms-username-" + envName, (String) dmsPayload.get("username"));
		cachingService.savePassword("dms-portOrIp-" + envName, (String) dmsPayload.get("portOrIp"));
		cachingService.savePassword("dms-deploymentPath-" + envName, (String) dmsPayload.get("deploymentPath"));
//		log.info("DMS PlayLoad = {}", dmsPayload);
//		log.info("DMS widget credentials cached for account: {}", envName);
	}

}
