package com.nnp.dashboard.service;

import com.nnp.dashboard.repo.NnpAccountRepo;
import com.nnp.dashboard.utils.Utils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nnp.dashboard.event.listener.AccActionEventListener;
import com.nnp.dashboard.event.listener.LatchEventListener;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.model.NnpAccount;
import com.nnp.dashboard.vo.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestration service that drives the end-to-end environment provisioning.
 *
 * Flow ( {@link #captureEnvRequest} ):
 *   1. Validates the requested env-code / admin user do not already exist in
 *      the DB, Keycloak, GitLab or Redmine.
 *   2. Creates the environment (and its master-cloned hierarchy), the default
 *      admin user, the GitLab group + user, and the Redmine project + user.
 *   3. Persists an environment-creation request (env + components + specs) that
 *      is later consumed for GitOps deployment.
 *   4. Waits (up to 120 sec) for the asynchronous Keycloak/GitLab/Redmine
 *      confirmations via {@link LatchEventListener}; on success hands off the
 *      request to the queue for the replication engine ({@code doFinalActivity}).
 *
 * On any failure the method performs a best-effort rollback
 * ( {@link #rollbackEnvironment} ) deleting whatever was already created.
 *
 * Runs asynchronously in {@code REQUIRES_NEW} transaction so that the
 * provisioning can continue independently from the caller's transaction.
 */
@Service
@Slf4j
public class EnvWrapperService {

    @Autowired
    private UserAccessService userAccessServ;

    @Autowired
    private EnvironmentServ envServ;

    @Autowired
    private KeycloakService keycloakService;

    @Autowired
    private GitlabService gitlabService;

    @Autowired
    private RedmineService redmineService;

    @Autowired
    private EnvActivityLogService envActivityLogService;
    
    @Autowired
    private EnvRequestServ envRequestService;

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private LatchEventListener listener;
    
    @Autowired
    private AccActionEventListener accCommListener;
    @Autowired
	private NnpAccountRepo nnpAccountRepo;
    @Autowired
   	public ApplicationEventPublisher appEventPub;
    //Environment creation
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
	public void captureEnvRequest(AllEnvironmentVOV2 allEnvironmentVOV2, NnpAccount account) {
//		log.info("allEnvironmentVOV2 --> {}", allEnvironmentVOV2);
		EnvironmentVOV2 environmentVOV2 = allEnvironmentVOV2.getEnvironment();
		UserVOV2 userVOV2 = allEnvironmentVOV2.getUser();
		EnvRequestVOV2 envReqVOV2 = allEnvironmentVOV2.getEnvReq();

//		log.info("environmentVOV2 --> {}", environmentVOV2);
//		log.info("userVOV2 --> {}", userVOV2);

		List<String> selectedCompIds = envReqVOV2.getReqCompVOV2().stream().map(comp -> comp.getCompId()).collect(Collectors.toList());
//		log.info("selectedCompIds --> {}", selectedCompIds);

		// checks
		if (envServ.checkIfEnvCodeExistsV2(environmentVOV2.getEnvCode())) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("409",
					"Env code " + environmentVOV2.getEnvCode() + " is existing in the system"));
		}
	    else if (keycloakService.isUserAvailableInKeycloak(environmentVOV2.getEnvTenantId(), userVOV2.getUserId())) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("409",
					"Default admin user " + userVOV2.getUserId() + " is existing in keycloak"));
		} else if (keycloakService.isEmailAvailableInKeycloak(environmentVOV2.getEnvTenantId(),
				userVOV2.getEmailId())) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("409",
					"Default admin user email " + userVOV2.getEmailId() + " is existing in keycloak"));
		} else if (gitlabService.isGroupExistsInGit(environmentVOV2.getEnvCode())) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("409",
					"Git group with name " + environmentVOV2.getEnvCode() + " is existing in the gitlab"));
		}
		else if(gitlabService.isUserExistsInGitLab(userVOV2.getUserId())){
			throw  new DashboardConfigException(new DashboardConfigExceptionMessage("409",
					"Default admin user " + userVOV2.getUserId() + " is existing in gitlab (it may still be pending deletion from a prior failed attempt - please retry shortly)"));
		}else if (redmineService.isProjectExistsInRedmine(environmentVOV2.getEnvCode())) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("409",
					"Redmine project " + environmentVOV2.getEnvCode() + " is existing in the redmine"));

		}else if (redmineService.isLoginExistsInRedmine(userVOV2.getUserId())) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("409",
					"Redmine login " + userVOV2.getUserId() + " is existing in the redmine"));
		} else {
			listener.prepareLatch(3); //One for keycloak, two for git and three for redmine
			EnvActivityLogVOV2 envActivityLogVOV2 = new EnvActivityLogVOV2();
			envActivityLogVOV2.setUserId(userVOV2.getUserId());

			if (StringUtils.isBlank(environmentVOV2.getEnvEmail()) && userVOV2 != null && StringUtils.isNotBlank(userVOV2.getEmailId())) {
				environmentVOV2.setEnvEmail(userVOV2.getEmailId());
			}

			EnvironmentVOV2 newEnvironmentVOV2 = envServ.createNewEnvironmentV2(environmentVOV2,selectedCompIds,
					envActivityLogVOV2,account);

			userVOV2.setEnvId(newEnvironmentVOV2.getEnvId());
			envActivityLogVOV2.setEnvId(newEnvironmentVOV2.getEnvId());
			userAccessServ.captureDefaultAdminUser(userVOV2, newEnvironmentVOV2, envActivityLogVOV2);

			// Create Group, subgroup, repo for code generation using low code and gitops
			// deployment
			Map<String, String> requestMapGitlab = new HashMap<>();
			requestMapGitlab.put("email", userVOV2.getEmailId());
			requestMapGitlab.put("name", userVOV2.getFirstName() + " " + userVOV2.getLastName());
			requestMapGitlab.put("envName", environmentVOV2.getEnvCode());
			requestMapGitlab.put("envPath", environmentVOV2.getEnvCode().toLowerCase());
			requestMapGitlab.put("userName", userVOV2.getUserId());
			requestMapGitlab.put("password", userVOV2.getPassword());
//			log.info("requestMapGitlab --> {}", requestMapGitlab);
			envActivityLogVOV2.setActDesc("group and default user creation in gitlab");
			envActivityLogVOV2.setActNote("group-default-user-creation-gitlab");
			envActivityLogVOV2.setActStatus("start");
			envActivityLogService.logActivity(envActivityLogVOV2);
			gitlabService.createGitGroupAndDefaultUser(requestMapGitlab, envActivityLogVOV2);

			// Redmine integration for project and user creation
			Map<String, String> requestMapRedmine = new HashMap<>();
			requestMapRedmine.put("name", environmentVOV2.getEnvCode());
			requestMapRedmine.put("identifier", environmentVOV2.getEnvCode().toLowerCase());
			requestMapRedmine.put("description", "root project for " + environmentVOV2.getEnvDesc());
			requestMapRedmine.put("login", userVOV2.getUserId());
			requestMapRedmine.put("firstname", userVOV2.getFirstName());
			requestMapRedmine.put("lastname", userVOV2.getLastName());
			requestMapRedmine.put("mail", userVOV2.getEmailId());
			requestMapRedmine.put("password", userVOV2.getPassword());
//			log.info("requestMapRedmine --> {}", requestMapRedmine);
			envActivityLogVOV2.setActDesc("project and default user creation in redmine");
			envActivityLogVOV2.setActNote("project-default-user-creation-redmine");
			envActivityLogVOV2.setActStatus("start");
			redmineService.createRedmineProjectAndDefaultUser(requestMapRedmine, envActivityLogVOV2);

			// create request and releted comp and spec - need to be deployed in new
			// environment using gitops
			envReqVOV2.setEnvId(newEnvironmentVOV2.getEnvId());
			String reqId = envRequestService.createEnvCreationReq(envReqVOV2);
			
			try {
				// wait untill all the three event received.
				listener.awaitResponsesAndProcess(reqId, userVOV2.getUserId(), userVOV2.getPassword(),userVOV2.getEmailId());

				// communication captured (Success)
				appEventPub.publishEvent(accCommListener.createAccActivityEvent(account,
						"Environment creation request captured",
						"Environment creation request captured",
						"Environment creation request capture successfully completed."));
			} catch (InterruptedException e) {
				// Catches Timeout, Interrupted, and Failures
				log.error("Environment creation failed! Triggering Rollback.", Utils.sanitizeForLog(e));
				// --> TRIGGER YOUR COMPLETE ROLLBACK <--
				rollbackEnvironment(environmentVOV2.getEnvCode(), userVOV2.getUserId(), environmentVOV2.getEnvTenantId(), account.getAccId());
				throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Environment creation failed: " + e.getMessage()));
			}
		}
	}
	public void rollbackEnvironment(String envCode, String userId, String tenantId, String accountId) {
		log.warn("V3 ENV ROLLBACK START for env: {} and user: {}", Utils.sanitizeForLog(envCode), Utils.sanitizeForLog(userId));

		// 1. Delete GitLab Group & User
		if (gitlabService.isGroupExistsInGit(envCode)) {
			gitlabService.deleteGroup(envCode);
		}
		gitlabService.deleteUser(userId);

		// 2. Delete Redmine Project & User
		if (redmineService.isProjectExistsInRedmine(envCode)) {
			redmineService.deleteProject(envCode);
		}
		redmineService.deleteUser(userId);

		// 3. Delete Keycloak User
		if (keycloakService.isUserAvailableInKeycloak(tenantId, userId)) {
			keycloakService.deleteUser(tenantId, userId);
		}

		// 4. Delete Database Account
		nnpAccountRepo.deleteByAccount(accountId);

//		log.warn("V3 ENV ROLLBACK COMPLETE for env: {}", envCode);
	}


}
