package com.nnp.dashboard.controller;

import java.util.List;
import java.util.Map;

import com.nnp.dashboard.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.service.EnvironmentServ;
import com.nnp.dashboard.service.NNPAccountService;
import com.nnp.dashboard.vo.AllEnvironmentVOV2;
import com.nnp.dashboard.vo.NNPAccountVO;
import com.nnp.dashboard.vo.NnpAccBillColVO;
import com.nnp.dashboard.vo.NnpCountryVO;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for Account (tenant) registration and lifecycle operations.
 *
 * Exposes endpoints under {@code /acc} for:
 *   - Reading reference data (countries)
 *   - Validating uniqueness of account name / user id before registration
 *   - Capturing a new account registration request
 *   - Triggering environment creation after a successful payment or free plan
 *   - Consuming pod status / DMS widget payloads pushed from the front-end
 *
 * Business logic is delegated to {@link NNPAccountService} and, for env-code
 * checks, to {@link EnvironmentServ}.
 */
@RestController
@RequestMapping("/acc")
@Slf4j
public class NNPAccountController {

	@Autowired
	private NNPAccountService accountServ;

	@Autowired
	private EnvironmentServ envService;

	@GetMapping(path = "/read/country/all")
	public List<NnpCountryVO> findAllCountry() {
		return accountServ.getAllCountry();
	}

	/*
	 * AccountName is checked in the NNPAccount table. Also it is check in NNP_ENV
	 * table If found , will return true else false Assumption is accName in
	 * NNPAccount table is unique and same as env_code in NNP_ENV
	 */
	@GetMapping(path = "/check/exists/account/{accName}")
	public Boolean checkIfAccountExists(@PathVariable("accName") String accName) {
		return accountServ.checkIfAccountExists(accName) || envService.checkIfEnvCodeExistsV2(accName);
	}

	/*
	 * UserId is checked in the nnp_user table. If found , will return true else
	 * false
	 */
	@GetMapping(path = "/check/exists/user/{userId}")
	public Boolean checkIfUserExists(@PathVariable("userId") String userId) {
		return accountServ.checkIfUserExists(userId);
	}

	@PostMapping(path = "/register/account")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ResponseEntity<Map<String, String>> captureEnvironmentRequest(@RequestBody NNPAccountVO nnpAccVO) {
		return accountServ.captureAccount(nnpAccVO);

	}

	/*
	 * If Payment Success Call this method to trigger environment creation This will
	 * store the payment details also
	 */

	@PostMapping(path = "/register/payment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> capturePaymentTriggerEnvAction(@RequestBody NnpAccBillColVO nnpAccBillColVO) {
//		log.info("received payment response - {}", nnpAccBillColVO);
		boolean isSuccess = accountServ.capturePaymentTriggerAction(nnpAccBillColVO);
		if(!isSuccess) {
			log.error("capturePaymentTriggerEnvAction error {} ->", Utils.sanitizeForLog("Payment failed for the Bill id "+nnpAccBillColVO.getAccBillId()+ "& account id"+nnpAccBillColVO.getAccId()));
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Payment failed for the Bill id "+nnpAccBillColVO.getAccBillId()+ "& account id"+nnpAccBillColVO.getAccId()));
		}
		return ResponseEntity.status(201).body("Account and User Created "+nnpAccBillColVO.getAccId());
    }
	@PostMapping(path = "/register/free/")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ResponseEntity<String> captureZeroCostTriggerEnvAction(@RequestBody NnpAccBillColVO nnpAccBillColVO) {
//		log.info("received zero-cost activation request for account - {}", nnpAccBillColVO.getAccName());
		boolean isSuccess = accountServ.capturePaymentTriggerActionFree(nnpAccBillColVO);
		if (!isSuccess) {
			log.error("capturePaymentTriggerEnvAction error {} ->",Utils.sanitizeForLog("Zero-cost activation failed for account " + nnpAccBillColVO.getAccName()));
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
					"Zero-cost activation failed for account " + nnpAccBillColVO.getAccName()));
		}
		return ResponseEntity.status(201).body("Account and User Created (zero-cost) " + nnpAccBillColVO.getAccName());
	}
	/**
	 * Receives K8s pod statuses from frontend, stores them as account communications,
	 * and updates the active/inactive status in nnp_account_plan_comp.
	 */
	@PostMapping(path = "/environment/{envName}/check-pods")
	public ResponseEntity<Map<String, String>> checkPodsAndStoreCommunication(
			@PathVariable("envName") String envName,
			@RequestBody Map<String, Object> payload,
			@RequestHeader("X-User-Name") String userName) {

//		log.info("NNPAccountController -> checkPodsAndStoreCommunication() for env: {}", envName);
		accountServ.checkPodsAndStoreCommunication(envName, payload, userName);
		return ResponseEntity.ok(Map.of("message", "Pod statuses checked and communication stored."));
	}

	/**
	 * Receives DMS widget credentials from frontend and caches them temporarily.
	 * Data is NOT stored in the database.
	 */
	@PostMapping(path = "/environment/{envName}/dms")
	public ResponseEntity<Map<String, String>> createDmsWidget(
			@PathVariable("envName") String envName,
			@RequestBody Map<String, Object> dmsPayload) {

//		log.info("NNPAccountController -> createDmsWidget() for env: {}", envName);
		accountServ.createDmsWidget(envName, dmsPayload);
		return ResponseEntity.ok(Map.of("message", "DMS widget credentials cached successfully."));
	}

}
