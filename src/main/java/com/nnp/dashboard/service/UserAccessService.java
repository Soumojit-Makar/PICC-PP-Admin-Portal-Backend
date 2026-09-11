package com.nnp.dashboard.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.nnp.dashboard.utils.Utils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import com.nnp.dashboard.event.RestResponseEvent;
import com.nnp.dashboard.event.listener.AccActionEventListener;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.model.EnvUserAccess;
import com.nnp.dashboard.model.NnpAccount;
import com.nnp.dashboard.model.UserConfig;
import com.nnp.dashboard.model.UserConfigV2;
import com.nnp.dashboard.model.UserV2;
import com.nnp.dashboard.repo.ChElemDetailRepo;
import com.nnp.dashboard.repo.DomainValueRepoV2;
import com.nnp.dashboard.repo.ElementDetailRepo;
import com.nnp.dashboard.repo.EnvFeatureRepo;
import com.nnp.dashboard.repo.EnvUserAccessRepo;
import com.nnp.dashboard.repo.EnvironmentRepo;
import com.nnp.dashboard.repo.FeatureElementRepo;
import com.nnp.dashboard.repo.IDRepo;
import com.nnp.dashboard.repo.NnpAccountRepo;
import com.nnp.dashboard.repo.RoleElemRepoV2;
import com.nnp.dashboard.repo.RoleRepoV2;
import com.nnp.dashboard.repo.UserConfigRepo;
import com.nnp.dashboard.repo.UserConfigRepoV2;
import com.nnp.dashboard.repo.UserRepoV2;
import com.nnp.dashboard.vo.DomainValueVOV2;
import com.nnp.dashboard.vo.EnvActivityLogVOV2;
import com.nnp.dashboard.vo.EnvironmentVOV2;
import com.nnp.dashboard.vo.EnvironmentVOV3;
import com.nnp.dashboard.vo.NNPAccountVO;
import com.nnp.dashboard.vo.RoleVOV2;
import com.nnp.dashboard.vo.UserAccessVO;
import com.nnp.dashboard.vo.UserConfigVO;
import com.nnp.dashboard.vo.UserConfigVOV2;
import com.nnp.dashboard.vo.UserCredentialVOV2;
import com.nnp.dashboard.vo.UserVOV2;
import com.nnp.dashboard.vo.kc.UserAccess;
import com.nnp.dashboard.vo.kc.UserAttributes;
import com.nnp.dashboard.vo.kc.UserCreation;
import com.nnp.dashboard.vo.kc.UserCredential;
import com.nnp.dashboard.vo.kc.UserUpdate;
import com.nnp.dashboard.vo.mail.EmailVO;
import com.nnp.dashboard.vo.rm.Issue;
import com.nnp.dashboard.vo.rm.Issue__1;
import com.nnp.dashboard.vo.rm.User;
import com.nnp.dashboard.vo.rm.User__1;

import lombok.extern.slf4j.Slf4j;

/**
 * Business service for everything related to users and their access.
 *
 * Responsibilities:
 *   - User registration (V2 and hardened V3 flows) and activation / onboarding
 *   - Assigning per-environment access (NNP_ENV_USER_ACCESS rows) based on the
 *     user's role (admin / superAdmin / user)
 *   - Keeping the downstream systems in sync: the user is created/updated in
 *     Keycloak (identity realm), Redmine (project member) and GitLab (repo access)
 *   - Sending welcome / notification emails
 *   - Role management, user listing, password reset, env migration and user deletion
 *
 * The V3 flow (registerUserV3) is write-ahead: it applies each write sequentially
 * aligned with the external system, tracks state, and on any failure performs a
 * "hard rollback" ( {@link #hardRollbackUser} ) so that no orphaned identity is
 * left behind in any subsystem.
 *
 * All DB writes are transactional; external calls intentionally escape the
 * transaction (via NOT_SUPPORTED / async wrappers) where a remote timeout must
 * not lock a DB transaction.
 */
@Service
@Transactional
@Slf4j
public class UserAccessService {

	@Autowired
	private EnvUserAccessRepo userAccessRepo;

	@Autowired
	private UserConfigRepo userConfRepo;

	@Autowired
	private EnvironmentRepo envRepository;

	@Autowired
	private EnvFeatureRepo envFeaRepository;

	@Autowired
	private FeatureElementRepo featureElementRepo;

	@Autowired
	private ElementDetailRepo elementDetailRepo;

	@Autowired
	private ChElemDetailRepo chElementDetailRepo;

	@Autowired
	private IDRepo idRepo;

	@Autowired
	private NnpAccountRepo nnpAccountRepo;

	// @Autowired
	// private FusionAuthService fusionAuthService;

	@Autowired
	private KeycloakService keycloakService;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private UserRepoV2 userRepoV2;

	@Autowired
	private RoleRepoV2 roleRepoV2;

	@Autowired
	private EnvironmentServ envServ;

	@Autowired
	private DomainValueRepoV2 domainValueRepoV2;

	@Autowired
	private RoleElemRepoV2 roleElemRepoV2;

	@Autowired
	private UserConfigRepoV2 userConfigRepoV2;

	@Autowired
	private GitlabService gitlabService;

	@Autowired
	private RedmineService redmineService;

	@Autowired
	private EnvActivityLogService envActivityLogService;

	@Autowired
	private MailService mailService;

	@Value("${keycloak.group:nnp-users}")
	private String group;

	@Value("${keycloak.user.group:users}")
	private String userGroup;

	@Value("${keycloak.admin.group:admins}")
	private String adminGroup;

	@Value("${mail.user.welcome.from.email:no-reply@nnp.example.com}")
	private String welcomeFromEmail;

	@Value("${mail.user.welcome.from.name:NNP Admin}")
	private String welcomeFromName;

	@Value("${mail.user.welcome.subject:Registration request to NNP has been received}")
	private String welcomeSubject;

	@Value("${mail.user.welcome.template.name:welcome-user}")
	private String welcomeTemplate;

	@Value("${mail.user.welcome.template.param.nnpName:NNP}")
	private String welcomeTemplatennpName;

	@Value("${mail.user.welcome.template.param.nnpPortalURL:https://nnp.example.com}")
	private String welcomeTemplatennpPortalURL;

	@Value("${mail.user.welcome.template.param.nnpEmail:support@nnp.example.com}")
	private String welcomeTemplatennpEmail;

	@Value("${mail.user.welcome.template.param.nnpAdmin:admin@nnp.example.com}")
	private String welcomeTemplatennpAdmin;

	@Value("${redmine.support.projectId:1}")
	private Integer redmineSupportProjectId;

	@Value("${redmine.support.subject:Support Request}")
	private String redmineSupportSubject;

	@Value("${redmine.support.priorityId:2}")
	private Integer redmineSupportPriorityId;

	@Value("${redmine.support.trackerId:1}")
	private Integer redmineSupportTrackerId;

	@Value("${redmine.support.description:Support request description}")
	private String redmineSupportDescription;

	@Autowired
	private ApplicationEventPublisher publisher;

	@Autowired
	private AccActionEventListener accCommListener;
	public UserAccessVO getUserByUserId(String userId) {

		return modelMapper.map(userAccessRepo.findByUserId(userId).getFirst(), UserAccessVO.class);
	}
	// Need to remove after development is over
	private void checkAndAssignUser(List<EnvUserAccess> userList, EnvUserAccess usrAccEntity) {
		boolean userExist = false;
		for (EnvUserAccess userAcc : userList) {
			if (userAcc.getUserId().equalsIgnoreCase(usrAccEntity.getUserId())) {
				userExist = true;
				break;
			}
		}
		if (!userExist) {
			EnvUserAccess userAccess = new EnvUserAccess();
			userAccess.setUserAccountId("USR_" + idRepo.getNextSeqVal());
			userAccess.setUserId(usrAccEntity.getUserId());
			userList.add(userAccess);
		}

	}

	private void checkAndUnAssignUser(List<EnvUserAccess> userList, EnvUserAccess usrAccEntity) {

        userList.removeIf(userAcc -> userAcc.getUserId().equalsIgnoreCase(usrAccEntity.getUserId()));

	}

	public void updateUserAcc(List<UserConfigVO> userConfVOs) {

		// get user id and the environment

		if (!userConfVOs.isEmpty()) {
			UserConfigVO userConfVO = userConfVOs.getFirst();
			userConfRepo.deleteUserByEnv(userConfVO.getUserId(), userConfVO.getEnvId());
		}

		userConfVOs.forEach(userConfVO -> {
			UserConfig usrConf = modelMapper.map(userConfVO, UserConfig.class);
			usrConf.setUserAccountId("USR_" + idRepo.getNextSeqVal());
			UserConfig user = userConfRepo.saveAndFlush(usrConf);

		});

	}

	public boolean isUserExistsInDbV2(String userId) {
		return userRepoV2.findById(userId).isPresent();
	}

	public UserVOV2 createDefaultAdminUserV2(UserVOV2 userVOV2, EnvironmentVOV2 environmentVOV2,
	                                         EnvActivityLogVOV2 envActivityLogVOV2) {
		UserV2 userV2 = modelMapper.map(userVOV2, UserV2.class);
		UserV2 retUserV2;

//		log.info("Creating default admin user in db");
		envActivityLogVOV2.setActDesc("default admin user creation in db");
		envActivityLogVOV2.setActNote("create-default-admin-db");
		envActivityLogVOV2.setActStatus("start");
		envActivityLogService.logActivity(envActivityLogVOV2);

		userV2.setRequestDate(LocalDateTime.now());
		userV2.setUpdateDate(LocalDateTime.now());
		userV2.setUserType("admin");
		userV2.setUserStatus("active");
		userV2.setRoleId(getUserRoleDetailsByRoleNameV2("adminRole").getRoleId());
		userV2.setUpdateComment("default admin user");
		retUserV2 = userRepoV2.saveAndFlush(userV2);

		List<UserConfigV2> userConfigV2List = new ArrayList<>();
		roleElemRepoV2.findByRoleIdAndEnvId(userV2.getRoleId(), userV2.getEnvId()).forEach(roleElemV2 -> {
			UserConfigV2 userConfigV2 = UserConfigV2.builder().userAccessId("USR_ACC_" + idRepo.getNextSeqVal())
					.userId(userV2.getUserId()).envId(userV2.getEnvId()).chElmDetailId(roleElemV2.getChElmDetailId())
					.build();
			userConfigV2List.add(userConfigV2);
		});
		userConfigRepoV2.saveAllAndFlush(userConfigV2List);

		envActivityLogVOV2.setActDesc("default admin user creation in db");
		envActivityLogVOV2.setActNote("create-default-admin-db");
		envActivityLogVOV2.setActStatus("complete");
		envActivityLogService.logActivity(envActivityLogVOV2);

//		log.info("Creating default admin user in keycloak");
		envActivityLogVOV2.setActDesc("default admin user creation in keycloak");
		envActivityLogVOV2.setActNote("create-default-admin-keycloak");
		envActivityLogVOV2.setActStatus("start");
		envActivityLogService.logActivity(envActivityLogVOV2);

		UserCredential userCredential = UserCredential.builder().type("password").value(userVOV2.getPassword())
				.temporary(false).build();
		UserAccess userAccess = UserAccess.builder().manageGroupMembership(true).view(true).mapRoles(true)
				.impersonate(true).manage(true).build();
		UserAttributes userAttributes = UserAttributes.builder()
				.contactNumber(new String[] { userVOV2.getContactNumber() }).build();
		UserCreation userCreationRequest = UserCreation.builder().username(userVOV2.getUserId())
				.email(userVOV2.getEmailId()).firstName(userVOV2.getFirstName()).lastName(userVOV2.getLastName())
				.emailVerified(true).enabled(true).groups(Arrays.asList(adminGroup))
				.credentials(Arrays.asList(userCredential)).attributes(userAttributes).access(userAccess).build();
//		log.info("userCreationRequest keycloak --> {}", userCreationRequest);
		keycloakService.createUser(userCreationRequest, environmentVOV2.getEnvTenantId());
//		log.info("userCreationSuccessful in keycloak");

		envActivityLogVOV2.setActDesc("default admin user creation in keycloak");
		envActivityLogVOV2.setActNote("create-default-admin-keycloak");
		envActivityLogVOV2.setActStatus("complete");
		envActivityLogService.logActivity(envActivityLogVOV2);

		return modelMapper.map(retUserV2, UserVOV2.class);
	}

	public UserVOV2 registerUserV2(UserVOV2 userVOV2) {
		UserV2 userV2 = modelMapper.map(userVOV2, UserV2.class);
		UserV2 retUserV2;
		NnpAccount acc;

		if (!isUserExistsInDbV2(userV2.getUserId())) {
			acc = nnpAccountRepo.findByAccName(userVOV2.getEnvId()).orElse(null);
			if (acc == null) {
				log.error("Account does not exist for env {}", Utils.sanitizeForLog(userVOV2.getEnvId()));
				throw new DashboardConfigException(
						new DashboardConfigExceptionMessage("409", "Account does not exist"));
			}

			userV2.setAccId(acc.getAccId());
			if (Objects.isNull(userV2.getRequestDate())) {
				// If no date in the payload of the request add system date
				userV2.setRequestDate(LocalDateTime.now());
			}
			userV2.setUserType("user");
			if (userV2.getRoleId() == null || userV2.getRoleId().equals(""))
				userV2.setRoleId(getUserRoleDetailsByRoleNameV2("userRole").getRoleId());
			publisher.publishEvent(accCommListener.createAccUserActivityEvent(userV2,
					"User Creation Started. User id - " + userVOV2.getUserId(),
					"User Creation Started. User id - " + userVOV2.getUserId(),
					"User Creation Started. User id - " + userVOV2.getUserId()));
			retUserV2 = userRepoV2.saveAndFlush(userV2);

			EnvironmentVOV2 environmentVOV2 = envServ.getEnvironmentByEnvIdV2(userVOV2.getEnvId());
//			log.debug("registerUser -- envName --> {} envId --> {} tenantId --> {}", environmentVOV2.getEnvName(),
//					environmentVOV2.getEnvId(), environmentVOV2.getEnvTenantId());

			boolean keycloakCreated = false;
			try {
				if (!keycloakService.isUserAvailableInKeycloak(environmentVOV2.getEnvTenantId(), userVOV2.getUserId())) {
					if (!keycloakService.isEmailAvailableInKeycloak(environmentVOV2.getEnvTenantId(),
							userVOV2.getEmailId())) {
//						log.info("User and email not available in keycloak " + userVOV2);
//						log.info("Creating user in keycloak");
						UserCredential userCredential = UserCredential.builder().type("password")
								.value(userVOV2.getPassword()).temporary(false).build();
						UserAccess userAccess = UserAccess.builder().manageGroupMembership(true).view(true).mapRoles(true)
								.impersonate(true).manage(true).build();
						UserAttributes userAttributes = UserAttributes.builder()
								.contactNumber(new String[] { userVOV2.getContactNumber() }).build();
						UserCreation userCreationRequest = UserCreation.builder().username(userVOV2.getUserId())
								.email(userVOV2.getEmailId()).firstName(userVOV2.getFirstName())
								.lastName(userVOV2.getLastName()).emailVerified(true).enabled(false)
								// .enabled(true)
								// .groups(new ArrayList<String>(Arrays.asList(group)))
								.groups(Arrays.asList(userGroup)).credentials(Arrays.asList(userCredential))
								.attributes(userAttributes).access(userAccess).build();

//						log.debug("userCreationRequest --> {}", userCreationRequest);

						keycloakService.createUser(userCreationRequest, environmentVOV2.getEnvTenantId());
						keycloakCreated = true;
//						log.info("User registration for user {} is successful in keycloak", userVOV2.getUserId());
					} else {
						log.error("User email {} already exists in keycloak", Utils.sanitizeForLog(userVOV2.getEmailId()));

						throw new DashboardConfigException(
								new DashboardConfigExceptionMessage("409", "User email already exists in keycloak"));
					}
				} else {
					log.error("User {} already exists in keycloak",Utils.sanitizeForLog( userVOV2.getUserId()));
					throw new DashboardConfigException(
							new DashboardConfigExceptionMessage("409", "User already exists in keycloak"));
				}
				if (!redmineService.isLoginExistsInRedmine(userVOV2.getUserId())) {
					if (redmineService.isProjectExistsInRedmine(environmentVOV2.getEnvCode())) {

						EnvActivityLogVOV2 envActivityLogVOV2 = new EnvActivityLogVOV2();
						envActivityLogVOV2.setUserId(userVOV2.getUserId());
						envActivityLogVOV2.setEnvId(environmentVOV2.getEnvId());

						User__1 usr = new User__1();
						usr.setLogin(userVOV2.getUserId());
						usr.setFirstname(userVOV2.getFirstName());
						usr.setLastname(userVOV2.getLastName());
						usr.setMail(userVOV2.getEmailId());
						usr.setPassword(userVOV2.getPassword());

						User redmineReqUser = new User();
						redmineReqUser.setUser(usr);

//						log.info("requestMapRedmine --> {}", redmineReqUser);

						redmineService.createRedmineDeveloperUser(redmineReqUser, envActivityLogVOV2);
						envActivityLogService.logActivity(envActivityLogVOV2);
					} else {
						log.error("Project or account {} not exist exists in Redmine or ", Utils.sanitizeForLog(environmentVOV2.getEnvCode()));
						throw new DashboardConfigException(
								new DashboardConfigExceptionMessage("410", "Project not found in Redmine"));
					}
				} else {
					log.error("User {} already exists in Redmine or ", Utils.sanitizeForLog(userVOV2.getUserId()));
					throw new DashboardConfigException(
							new DashboardConfigExceptionMessage("410", "User already exists in Redmine"));
				}
			} catch (Exception e) {
				log.error("Exception during user registration. Rolling back Keycloak registration.", Utils.sanitizeForLog(e.getMessage()));
				if (keycloakCreated) {
					try {
						keycloakService.deleteUser(environmentVOV2.getEnvTenantId(), userVOV2.getUserId());
					} catch (Exception ex) {
						log.error("Rollback failed: could not delete user {} from Keycloak: {}", Utils.sanitizeForLog(userVOV2.getUserId()), Utils.sanitizeForLog(ex.getMessage()));
					}
				}
				throw e;
			}

		} else {
			log.error("User {} already exists in system", Utils.sanitizeForLog(userVOV2.getUserId()));
			throw new DashboardConfigException(
					new DashboardConfigExceptionMessage("409", "User already exists in system"));
		}

		// -- send user welcome email start --//
		sendUserWelcomeNote(modelMapper.map(userV2,UserVOV2.class));

		sendAdminUsrRegNotification(userV2);
//		log.info("registeruser -- end of welcome email to user with no attachement");

		// Now User created in Redmine and Keycloak.. Need to enable user, provide
		// access to gitlab and send communication to user
		publisher.publishEvent(accCommListener.createAccUserActivityEvent(userV2,
				"User Activation Started. User id - " + userVOV2.getUserId(),
				"User Activation Started. User id - " + userVOV2.getUserId(),
				"User Activation Started. User id - " + userVOV2.getUserId()));
		this.onboardUserV2(userVOV2);
		publisher.publishEvent(accCommListener.createAccUserActivityEvent(userV2,
				"User Activation Done. User id - " + userVOV2.getUserId(),
				"User Activation Done. User id - " + userVOV2.getUserId(),
				"User Activation Done. User id - " + userVOV2.getUserId()));

		// userV2.setUserId("USR_" + idRepo.getNextSeqVal());
		return modelMapper.map(retUserV2, UserVOV2.class);
	}

	private void sendAdminUsrRegNotification(UserV2 userV2) {

		List<UserV2> adminUsers = userRepoV2.findByEnvIdAndUserStatusAndUserTypeNot(userV2.getEnvId(), "active",
				"user");

		EmailVO emailVOAdmin = new EmailVO();
		emailVOAdmin.setFrom(welcomeFromEmail);
		String sendToAddr = "";
		for (UserV2 admUser : adminUsers) {
			if (!sendToAddr.isBlank()) {
				sendToAddr = sendToAddr + ",";
			}
			sendToAddr = sendToAddr + admUser.getEmailId();
		}
		emailVOAdmin.setTo(sendToAddr);
		emailVOAdmin.setToName("Admin");
		emailVOAdmin.setSubject("User Registration Notification for user - " + userV2.getUserId());
		emailVOAdmin.setText("Dear Admin," + "\r\n"
				+ "A request for user registration to nnp has been received. This is just for the information. \r\n"
				+ "User Email Address - " + userV2.getEmailId() + "\r\n"
				+ "\r\n"
				+ "With Regards,\r\n"
				+ "NNP Administration Team ");
		mailService.sendUserWelcomeMail(emailVOAdmin);
//		log.info("registeruser -- sent user registration notification email to nnp-support");
	}

	private void sendUserWelcomeNote(UserVOV2 userV2) {
//		log.info("registeruser request for user -- start send welcome email to user");
		EmailVO emailVO = new EmailVO();
		emailVO.setFrom(welcomeFromEmail);
		emailVO.setFromName(welcomeFromName);
		emailVO.setTo(userV2.getEmailId());
		emailVO.setToName(userV2.getFirstName() + " " + userV2.getLastName());
		emailVO.setSubject("Registration request to NNP has been reveived");
		if (userV2.getPassword()==null){
            emailVO.setText("Dear User," + "\r\n"
                    + "Your request for user registration to NNP has been received. \r\n"
                    + "You will be notified shortly after your request has been processed.\r\n"
                    + "\r\n"
                    + "Please note the below credentials for the login to Nubons Platform.\r\n"
                    + "Portal URL : " + welcomeTemplatennpPortalURL + " \r\n"
                    + "Environment Name: " + userV2.getEnvId()
                    + "\r\n"
                    + "NNP Portal Username : " + userV2.getUserId() + " & Password : " + "Please contact the administrator or use the same password."
                    + "\r\n"
                    + "With Regards,\r\n"
                    + "NNP Administration Team ");
		}else {
            emailVO.setText("Dear User," + "\r\n"
                    + "Your request for user registration to NNP has been received. \r\n"
                    + "You will be notified shortly after your request has been processed.\r\n"
                    + "\r\n"
                    + "Please note the below credentials for the login to Nubons Platform.\r\n"
                    + "Portal URL : " + welcomeTemplatennpPortalURL + " \r\n"
                    + "Environment Name: " + userV2.getEnvId()
                    + "\r\n"
                    + "NNP Portal Username : " + userV2.getUserId() + " & Password : " + userV2.getPassword()
                    + "\r\n"
                    + "With Regards,\r\n"
                    + "NNP Administration Team ");
        }

//		log.info("registeruser -- sending email to user with no attachement");
		mailService.sendUserWelcomeMail(emailVO);
//		log.info("registeruser -- sent welcome email to user with no attachement");
	}

	public void onboardUserV2(UserVOV2 userVOV2) {
		UserV2 userV2 = modelMapper.map(userVOV2, UserV2.class);
		NnpAccount acc = nnpAccountRepo.findByAccName(userVOV2.getEnvId())
				.orElseThrow(() -> new DashboardConfigException(
						new DashboardConfigExceptionMessage("409", "Account does not exist")));
		userV2.setAccId(acc.getAccId());
		EnvActivityLogVOV2 envActivityLogVOV2 = new EnvActivityLogVOV2();
		envActivityLogVOV2.setEnvId(userVOV2.getEnvId());
		envActivityLogVOV2.setUserId(userVOV2.getUserId());

		// -- DB update start --//
//		log.info("onboardUser -- start db update");
		envActivityLogVOV2.setActDesc("user activation in db");
		envActivityLogVOV2.setActNote("activate-db");
		envActivityLogVOV2.setActStatus("start");
		envActivityLogService.logActivity(envActivityLogVOV2);

		if (isUserExistsInDbV2(userV2.getUserId())) {
			boolean isGrpChangeRequired = false;
			boolean isAdminMailTemplate = false;

			userV2.setUpdateDate(LocalDateTime.now());
			userV2.setUserStatus("inProgress");
			RoleVOV2 roleVOV2 = getUserRoleDetailsByRoleIdV2(userV2.getRoleId());
			if (roleVOV2 != null) {
				if (roleVOV2.getRoleName().equals("adminRole")) {
					isGrpChangeRequired = true;
					isAdminMailTemplate = true;
					userV2.setUserType("admin");
				} else if (roleVOV2.getRoleName().equals("superAdminRole")) {
					isGrpChangeRequired = true;
					userV2.setUserType("superAdmin");
				} else {
					userV2.setUserType("user");
				}
			} else {
				userV2.setUserType("user");
			}

			userRepoV2.saveAndFlush(userV2);
//			log.info("onboardUser -- db update user details complete");
			List<UserConfigV2> userConfigV2List = new ArrayList<>();

			// Create access of different component link for the user based on the role.
			// Based on the user role_id and environment_id take dtlspec_id from
			// nnp_user_role_elem table. This will return multiple row.
			// then insert row for each dtlspec_id in the nnp_env_user_access table with
			// user_id and env_id
			roleElemRepoV2.findByRoleIdAndEnvId(userV2.getRoleId(), userV2.getEnvId()).forEach(roleElemV2 -> {
				UserConfigV2 userConfigV2 = UserConfigV2.builder().userAccessId("USR_ACC_" + idRepo.getNextSeqVal())
						.userId(userV2.getUserId()).envId(userV2.getEnvId())
						.chElmDetailId(roleElemV2.getChElmDetailId()).build();
				userConfigV2List.add(userConfigV2);
			});

			userConfigRepoV2.saveAllAndFlush(userConfigV2List);
//			log.info("onboardUser -- db update user access complete");

			envActivityLogVOV2.setActDesc("user activation in db");
			envActivityLogVOV2.setActNote("activate-db");
			envActivityLogVOV2.setActStatus("complete");
			envActivityLogService.logActivity(envActivityLogVOV2);
//			log.info("onboardUser -- end db update");
			// -- DB update end--//

			// -- keycloak update start--//
			EnvironmentVOV2 environmentVOV2 = envServ.getEnvironmentByEnvIdV2(userVOV2.getEnvId());
//			log.info("onboardUser -- envName --> {} envId --> {} tenantId --> {}", environmentVOV2.getEnvName(),
//					environmentVOV2.getEnvId(), environmentVOV2.getEnvTenantId();

			envActivityLogVOV2.setActDesc("user activation in keycloak");
			envActivityLogVOV2.setActNote("activate-keycloak");
			envActivityLogVOV2.setActStatus("start");
			envActivityLogService.logActivity(envActivityLogVOV2);
//			log.info("onboardUser -- keycloak user update start");
			if (keycloakService.isUserAvailableInKeycloak(environmentVOV2.getEnvTenantId(), userVOV2.getUserId())) {
//				log.info("User available in keycloak " + userVOV2);
//				log.info("Updating user in keycloak");
				UserUpdate userUpdate = UserUpdate.builder().enabled(true).build();

				keycloakService.updateUser(userUpdate, environmentVOV2.getEnvTenantId(), userVOV2.getUserId());

				if (isGrpChangeRequired) {
					keycloakService.changeUserGroupMembership(environmentVOV2.getEnvTenantId(), userVOV2.getUserId(),
							userGroup, adminGroup);
				}

				envActivityLogVOV2.setActDesc("user activation in keycloak");
				envActivityLogVOV2.setActNote("activate-keycloak");
				envActivityLogVOV2.setActStatus("complete");
				envActivityLogService.logActivity(envActivityLogVOV2);
//				log.info("onboardUser -- keycloak user update end");

			} else {
				log.error("onboardUser -- User does not exist in keycloak");
				envActivityLogVOV2.setActDesc("user activation in keycloak");
				envActivityLogVOV2.setActNote("activate-keycloak");
				envActivityLogVOV2.setActStatus("failed");
				envActivityLogService.logActivity(envActivityLogVOV2);
				throw new DashboardConfigException(
						new DashboardConfigExceptionMessage("404", "User does not exist in keycloak"));
			}
			// -- keycloak update end--//

			// -- gitlab update start -- //
//			log.info(
//					"onboardUser -- GitLabIntegrationService -> onboard::Service call for creating Group and user in gitlab");

			envActivityLogVOV2.setActDesc("user activation in gitlab");
			envActivityLogVOV2.setActNote("activate-gitlab");
			envActivityLogVOV2.setActStatus("start");
			envActivityLogService.logActivity(envActivityLogVOV2);
			//
			Map<String, String> requestMapGitlab = new HashMap<>();
			requestMapGitlab.put("userName", userVOV2.getUserId());
			// requestMapGitlab.put("password", userVOV2.getUserId());
			requestMapGitlab.put("email", userVOV2.getEmailId());
			requestMapGitlab.put("name", userVOV2.getFirstName() + " " + userVOV2.getLastName());
			requestMapGitlab.put("groupName", environmentVOV2.getEnvRepo());
			requestMapGitlab.put("userType", userV2.getUserType());
			//
			try {
				gitlabService.onboardGitLab(requestMapGitlab, envActivityLogVOV2);
			} catch (Exception e) {
				log.error("exception in GitLabIntegrationService -> onboard::Service call. Rolling back Keycloak status.");
				try {
					UserUpdate userUpdate = UserUpdate.builder().enabled(false).build();
					keycloakService.updateUser(userUpdate, environmentVOV2.getEnvTenantId(), userVOV2.getUserId());
				} catch (Exception ex) {
					log.error("Rollback failed: could not disable user in Keycloak: {}", Utils.sanitizeForLog(ex.getMessage()));
				}
				throw e;
			}
			//
			envActivityLogVOV2.setActDesc("user activation in gitlab");
			envActivityLogVOV2.setActNote("activate-gitlab");
			envActivityLogVOV2.setActStatus("complete");
			envActivityLogService.logActivity(envActivityLogVOV2);
//			log.info("onboardUser -- GitLabIntegrationService -> onboard::Service call end");
			// -- gitlab update end -- //

			// -- DB user status update start --//
//			log.info("onboardUser -- start user status db update");
			envActivityLogVOV2.setActDesc("user status update in db");
			envActivityLogVOV2.setActNote("status-update-db");
			envActivityLogVOV2.setActStatus("start");
			envActivityLogService.logActivity(envActivityLogVOV2);

			userV2.setUpdateDate(LocalDateTime.now());
			userV2.setUserStatus("active");

			userRepoV2.saveAndFlush(userV2);

			envActivityLogVOV2.setActDesc("user status update in db");
			envActivityLogVOV2.setActNote("status-update-db");
			envActivityLogVOV2.setActStatus("complete");
			envActivityLogService.logActivity(envActivityLogVOV2);
//			log.info("onboardUser -- end user status db update");
			// -- DB user status update end --//

			// -- Redmine support ticket creation for user start --//
//			log.info("onboardUser -- start redmine support ticket creation");
			String strRedmineSupportSubject = java.text.MessageFormat.format(redmineSupportSubject, userV2.getUserId());
			String strRedmineSupportDescription = java.text.MessageFormat.format(redmineSupportDescription,
					userV2.getUserId(), userV2.getEnvId());

			Issue__1 issue__1 = new Issue__1();
			issue__1.setProjectId(redmineSupportProjectId);
			issue__1.setSubject(strRedmineSupportSubject);
			issue__1.setPriorityId(redmineSupportPriorityId);
			issue__1.setTrackerId(redmineSupportTrackerId);
			issue__1.setDescription(strRedmineSupportDescription);

			Issue issue = new Issue();
			issue.setIssue(issue__1);

			// FOR USER CREATION ACTIVITY
//			log.info("onboardUser -- end redmine support ticket creation");
			// -- Redmine support ticket creation for user end --//

			// -- send user welcome email start --//
//			log.info("onboardUser -- start send welcome email to user");
			EmailVO emailVO = new EmailVO();
			emailVO.setFrom(welcomeFromEmail);
			emailVO.setFromName(welcomeFromName);
			emailVO.setTo(userV2.getEmailId());
			emailVO.setToName(userV2.getFirstName() + " " + userV2.getLastName());
			emailVO.setSubject(welcomeSubject);
			emailVO.setText((isAdminMailTemplate) ? environmentVOV2.getAdmCommunication()
					: environmentVOV2.getUsrCommunication());
			try {
//				log.info("onboardUser -- sending welcome email to user with attachement");
				mailService.sendMailWithAttachement(emailVO, createAttachments(environmentVOV2, isAdminMailTemplate));
//				log.info("onboardUser -- sent welcome email to user with attachement");
			} catch (IOException e) {
				log.error("onboardUser -- exception in send welcome email to user with attachement", Utils.sanitizeForLog(e));
				throw new DashboardConfigException(
						new DashboardConfigExceptionMessage("500",
								"Exception in sending email with attachement" +Utils.sanitizeForLog(e.getMessage())));
			}
//			log.info("onboardUser -- end of welcome email to user with attachement");
			// -- send user welcome email end --//

		} else {
			log.error("onboardUser -- User does not exist in system");
			envActivityLogVOV2.setActDesc("user activation in db");
			envActivityLogVOV2.setActNote("activate-db");
			envActivityLogVOV2.setActStatus("failed");
			envActivityLogService.logActivity(envActivityLogVOV2);
			throw new DashboardConfigException(
					new DashboardConfigExceptionMessage("404", "User does not exist in system"));
		}
	}

	private File createAttachments(EnvironmentVOV2 environmentVOV2, boolean isAdminMailTemplate) throws IOException {
//		log.info("onboardUser -- creting attachement for user witn isAdminMailTemplate = " + isAdminMailTemplate);
		List<File> attachements = new ArrayList<>();
		File f = new File("k8stoken.txt");
		FileWriter wr = new FileWriter(f,StandardCharsets.UTF_8);
		String token = isAdminMailTemplate ? environmentVOV2.getAdmK8SToken() : environmentVOV2.getUsrK8SToken();
		if (token != null) {
			wr.write(token);
		} else {
			wr.write("");
		}
		wr.flush();
		wr.close();
//		log.info("onboardUser -- ATTACHEMENT CREATED for user witn isAdminMailTemplate = " + isAdminMailTemplate);
		return f;
	}

	public void resetUserPasswordV2(UserCredentialVOV2 userCredentialVOV2, String realmName, String userId) {

		UserCredential userCredential = UserCredential.builder().type(userCredentialVOV2.getType())
				.value(userCredentialVOV2.getValue()).temporary(userCredentialVOV2.isTemporary()).build();
		keycloakService.resetUserPassword(userCredential, realmName, userId);
	}

	public List<UserVOV2> getUserListByStatusV2(String status) {
		return userRepoV2.getUsersByStatus(status).stream().map(userV2 -> modelMapper.map(userV2, UserVOV2.class))
				.toList();
	}

	public List<UserVOV2> getUserListByStatusV2(String status, String logedInUserId) {
		UserV2 user = userRepoV2.findByUserId(logedInUserId);
		if ("admin".equalsIgnoreCase(user.getUserType())) {
			return this.getUserListByEnvIdAndStatusV2(user.getEnvId(), status);
		} else {
			return this.getUserListByStatusV2(status);
		}
	}

	public UserVOV2 getUserByUserIdV2(String userId) {
		return userRepoV2.findById(userId).map(userV2 -> modelMapper.map(userV2, UserVOV2.class)).orElse(null);
	}

	private UserVOV2 getUserByUserIdentifierV2(String userIdentifier) {
		return userRepoV2.findByUserIdOrUserEmail(userIdentifier.trim()).stream()
				.findFirst()
				.map(userV2 -> modelMapper.map(userV2, UserVOV2.class))
				.orElse(null);
	}

	public boolean ifUserExistsByUserIdentifierV2(String userIdentifier) {
		UserVOV2 user = getUserByUserIdentifierV2(userIdentifier);
		return Objects.nonNull(user);
	}

	public List<UserVOV2> getUserListByEnvIdV2(String envId) {
		return userRepoV2.findByEnvId(envId).stream().map(userV2 -> modelMapper.map(userV2, UserVOV2.class)).toList();
	}

	public List<UserVOV2> getUserListByEnvIdAndStatusV2(String envId, String status) {
		return userRepoV2.findByEnvIdAndUserStatus(envId, status).stream()
				.map(userV2 -> modelMapper.map(userV2, UserVOV2.class)).toList();
	}

	public List<UserVOV2> getAllUserListV2() {
		return userRepoV2.findAll().stream().map(userV2 -> modelMapper.map(userV2, UserVOV2.class)).toList();
	}

	public List<RoleVOV2> getAllUserRoleDetailsV2() {
		return roleRepoV2.findByRoleStatus("active").stream().map(roleV2 -> modelMapper.map(roleV2, RoleVOV2.class))
				.toList();
	}

	public RoleVOV2 getUserRoleDetailsByRoleNameV2(String roleName) {
		return roleRepoV2.findByRoleName(roleName).stream().map(roleV2 -> modelMapper.map(roleV2, RoleVOV2.class))
				.findFirst().orElseThrow(() -> new DashboardConfigException(
						new DashboardConfigExceptionMessage("404", "role does not exists with provided name")));
	}

	public RoleVOV2 getUserRoleDetailsByRoleIdV2(String roleId) {
		return roleRepoV2.findById(roleId).map(roleV2 -> modelMapper.map(roleV2, RoleVOV2.class)).orElse(null);
	}

	public List<DomainValueVOV2> getDomainValuesByDomainNameV2(String domainName) {
		return domainValueRepoV2.findByDomainName(domainName).stream()
				.map(domainValueV2 -> modelMapper.map(domainValueV2, DomainValueVOV2.class)).toList();
	}

	public EnvironmentVOV3 getUserAccessByEnvIdAndUserIdV3(String envId, String userId) {
		EnvironmentVOV3 environmentVOV3 = envServ.getEnvironmentByEnvIdV3(envId);
		List<UserConfigV2> userConfigV2List = userConfigRepoV2.findByUserIdAndEnvIdAndChElmDetailIdIsNotNull(userId,
				envId);

		environmentVOV3.getEnvFeatures()
				.forEach(envFeatureVOV3 -> envFeatureVOV3.getFeatureElements()
						.forEach(featureElementVOV3 -> featureElementVOV3.getElementDetails()
								.forEach(elementDetailVOV3 -> elementDetailVOV3.getChildElementDtls()
										.forEach(chElementDetailVOV3 -> userConfigV2List.forEach(userConfigV2 -> {
											if (chElementDetailVOV3.getChElementDtlId()
													.equalsIgnoreCase(userConfigV2.getChElmDetailId()))
												chElementDetailVOV3.setAssigned(true);
										})))));
		return environmentVOV3;
	}

	public void updateUserAccessV3(List<UserConfigVOV2> userConfigVOV2List, String envId, String userId) {
		if (userConfigVOV2List != null && !userConfigVOV2List.isEmpty()) {
			userConfigRepoV2.deleteUserByEnv(userId, envId);

			List<UserConfigV2> userConfigV2List = userConfigVOV2List.stream().map(userConfigVOV2 -> {
				if (userConfigVOV2.getEnvId() == null || userConfigVOV2.getEnvId().equals(""))
					userConfigVOV2.setEnvId(envId);
				if (userConfigVOV2.getUserId() == null || userConfigVOV2.getUserId().equals(""))
					userConfigVOV2.setUserId(userId);
				UserConfigV2 userConfigV2 = modelMapper.map(userConfigVOV2, UserConfigV2.class);
				userConfigV2.setUserAccessId("USR_ACC_" + idRepo.getNextSeqVal());
				return userConfigV2;
			}).toList();

			userConfigRepoV2.saveAllAndFlush(userConfigV2List);
		}
	}

	public UserVOV2 updateUserRoleAndTypeV2(UserVOV2 userVOV2) {
		UserVOV2 userVOV2Db = getUserByUserIdV2(userVOV2.getUserId());
		UserVOV2 retUserVOV2 = null;
		if (userVOV2Db == null) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("404",
					"User not found in db with userId " + userVOV2.getUserId()));
		} else if (userVOV2.getRoleId() == null || userVOV2.getRoleId().isEmpty() || userVOV2.getRoleId().isBlank()) {
			throw new DashboardConfigException(
					new DashboardConfigExceptionMessage("400", "User roleId is blank or null"));
		} else {
			if (userVOV2Db.getRoleId().equals(userVOV2.getRoleId())) {
				retUserVOV2 = modelMapper.map(userVOV2Db, UserVOV2.class);
			} else {
				EnvironmentVOV2 environmentVOV2Db = envServ.getEnvironmentByEnvIdV2(userVOV2Db.getEnvId());
				if (keycloakService.isUserAvailableInKeycloak(environmentVOV2Db.getEnvTenantId(),
						userVOV2.getUserId())) {
					RoleVOV2 roleVOV2 = getUserRoleDetailsByRoleIdV2(userVOV2.getRoleId());
					RoleVOV2 roleVOV2Existing = getUserRoleDetailsByRoleIdV2(userVOV2Db.getRoleId());
					boolean isGroupChangeRequired = false;
					String sourceGroup = null;
					String targetGroup = null;
					if (roleVOV2 != null) {
						if (roleVOV2.getRoleName().equals("adminRole")) {
							userVOV2Db.setUserType("admin");
							if (roleVOV2Existing.getRoleName().equals("userRole")) {
								isGroupChangeRequired = true;
								sourceGroup = userGroup;
								targetGroup = adminGroup;
							}
						} else if (roleVOV2.getRoleName().equals("superAdminRole")) {
							userVOV2Db.setUserType("superAdmin");
							if (roleVOV2Existing.getRoleName().equals("userRole")) {
								isGroupChangeRequired = true;
								sourceGroup = userGroup;
								targetGroup = adminGroup;
							}
						} else {
							userVOV2Db.setUserType("user");
							if (roleVOV2Existing.getRoleName().equals("adminRole")
									|| roleVOV2Existing.getRoleName().equals("superAdminRole")) {
								isGroupChangeRequired = true;
								sourceGroup = adminGroup;
								targetGroup = userGroup;
							}
						}
						userVOV2Db.setRoleId(userVOV2.getRoleId());
						userVOV2Db.setUpdateDate(LocalDateTime.now());
						userVOV2Db.setUpdateComment("user role and type update");
						UserV2 userV2 = userRepoV2.saveAndFlush(modelMapper.map(userVOV2Db, UserV2.class));
						if (isGroupChangeRequired) {
							keycloakService.changeUserGroupMembership(environmentVOV2Db.getEnvTenantId(),
									userV2.getUserId(), sourceGroup, targetGroup);
						}
						retUserVOV2 = modelMapper.map(userV2, UserVOV2.class);
					} else {
						log.error("User role not found for roleId {}" ,Utils.sanitizeForLog(userVOV2.getRoleId()));
						throw new DashboardConfigException(new DashboardConfigExceptionMessage("404",
								"User role not found for roleId " + userVOV2.getRoleId()));
					}
				} else {
                    log.error("User not found in keycloak with userId {}", Utils.sanitizeForLog(userVOV2.getUserId()));
					throw new DashboardConfigException(new DashboardConfigExceptionMessage("404",
							"User not found in keycloak with userId " + userVOV2.getUserId()));
				}
			}
		}
		return retUserVOV2;
	}

	@Async
	public void sendTextMessage() {
		sleep(5);
//		System.out.println("Called 3rd party to send SMS");
	}

	private void sleep(int i) {
		try {
			TimeUnit.SECONDS.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean isUserValidForEnv(String envId, String userId) {
		return !userRepoV2.findByEnvIdAndUserId(envId, userId).isEmpty();
	}

	public UserVOV2 migrateEnvForUser(String envId, String userId) {

		EnvActivityLogVOV2 envActivityLogVOV2 = new EnvActivityLogVOV2();
		envActivityLogVOV2.setEnvId(envId);
		envActivityLogVOV2.setUserId(userId);
		envActivityLogVOV2.setActDesc("start user migrated to env - " + envId);
		envActivityLogVOV2.setActNote("user migration");
		envActivityLogVOV2.setActStatus("Start");
		envActivityLogService.logActivity(envActivityLogVOV2);

		UserV2 userV2 = userRepoV2.findByUserId(userId);
		EnvironmentVOV2 existingEnvVOV2 = envServ.getEnvironmentByEnvIdV2(userV2.getEnvId());
		EnvironmentVOV2 targetEnvVOV2 = envServ.getEnvironmentByEnvIdV2(envId);

		// In Current architecture tenant is the Relm in Keycloak.
		// Currently all Environment have only one relm. But if there is different relm
		// for different environment
		// then below logic need to be updated - by creating a new user in new relm and
		// deactivate user from existing rel

		// Check if above assumption is correct - if not then throw exception
		if (!existingEnvVOV2.getEnvTenantId().equalsIgnoreCase(targetEnvVOV2.getEnvTenantId())) {
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
					"Tenant or Relm is different for current and target environment"));
		}

		// considering relm or tenant is same for all the environment - one relm support
		// all environment in keycloak
		// delete all the element access against the user
		userConfigRepoV2.deleteUserByEnv(userId, userV2.getEnvId());
		// set target environment against the user
		userV2.setEnvId(envId);
		// change the status of the user as Active to applied
		userV2.setUserStatus("applied");
		// Save the user with target environment mapped along with status as applied.
		userV2 = userRepoV2.saveAndFlush(userV2);

		// Make the user disabled in keycloak - user need to be approved by target
		// environment admin to activated
//		log.info("Updating user in keycloak");

		// Taking tenantId from targetEnv as existingEnv and TargetEnv has same tenant
		// as per assumption mentioned above
		String tenantId = targetEnvVOV2.getEnvTenantId();
		UserUpdate userUpdate = UserUpdate.builder().enabled(false).build();

		keycloakService.updateUser(userUpdate, tenantId, userId);

		// delete user from git as the user is assigned programmatically to the group
		// dedicated for the environment.
		// user will be created and assigned to correct group while superadmin activate
		// the user
		gitlabService.deleteUser(userV2.getUserId());

		// send user welcome mail
		sendUserWelcomeNote(modelMapper.map(userV2,UserVOV2.class));

		// Notify Environemnt admins
		sendAdminUsrRegNotification(userV2);

		envActivityLogVOV2.setActDesc("end user migrated to env - " + envId);
		envActivityLogVOV2.setActNote("user migration");
		envActivityLogVOV2.setActStatus("End");
		envActivityLogService.logActivity(envActivityLogVOV2);

//		log.info("migrate environment for user " + userId);
		return modelMapper.map(userV2, UserVOV2.class);
	}

	// Environment creation
	public void captureDefaultAdminUser(UserVOV2 userVOV2, EnvironmentVOV2 newEnvironmentVOV2,
	                                    EnvActivityLogVOV2 envActivityLogVOV2) {

		UserV2 userV2 = modelMapper.map(userVOV2, UserV2.class);
		UserV2 retUserV2;

//		log.info("Creating default admin user in db");
		envActivityLogVOV2.setActDesc("default admin user creation in db");
		envActivityLogVOV2.setActNote("create-default-admin-db");
		envActivityLogVOV2.setActStatus("start");
		envActivityLogService.logActivity(envActivityLogVOV2);
		userV2.setUpdateDate(LocalDateTime.now());
		retUserV2 = userRepoV2.saveAndFlush(userV2);

		List<UserConfigV2> userConfigV2List = new ArrayList<>();
		roleElemRepoV2.findByRoleIdAndEnvId(userV2.getRoleId(), userV2.getEnvId()).forEach(roleElemV2 -> {
			UserConfigV2 userConfigV2 = UserConfigV2.builder().userAccessId("USR_ACC_" + idRepo.getNextSeqVal())
					.userId(userV2.getUserId()).envId(userV2.getEnvId()).chElmDetailId(roleElemV2.getChElmDetailId())
					.build();
			userConfigV2List.add(userConfigV2);
		});
		userConfigRepoV2.saveAllAndFlush(userConfigV2List);

		envActivityLogVOV2.setActDesc("default admin user creation in db");
		envActivityLogVOV2.setActNote("create-default-admin-db");
		envActivityLogVOV2.setActStatus("complete");
		envActivityLogService.logActivity(envActivityLogVOV2);

		UserCredential userCredential = UserCredential.builder().type("password").value(userVOV2.getPassword())
				.temporary(false).build();
		UserAccess userAccess = UserAccess.builder().manageGroupMembership(true).view(true).mapRoles(true)
				.impersonate(true).manage(true).build();
		UserAttributes userAttributes = UserAttributes.builder()
				.contactNumber(new String[] { userVOV2.getContactNumber() }).build();
		UserCreation userCreationRequest = UserCreation.builder().username(userVOV2.getUserId())
				.email(userVOV2.getEmailId()).firstName(userVOV2.getFirstName()).lastName(userVOV2.getLastName())
				.emailVerified(true).enabled(true).groups(Arrays.asList(adminGroup))
				.credentials(Arrays.asList(userCredential)).attributes(userAttributes).access(userAccess).build();
//		log.info("userCreationRequest keycloak --> {}", userCreationRequest);
		keycloakService.createUser(userCreationRequest, newEnvironmentVOV2.getEnvTenantId());
//		log.info("userCreationSuccessful in keycloak");

		envActivityLogVOV2.setActDesc("default admin user creation in keycloak");
		envActivityLogVOV2.setActNote("create-default-admin-keycloak");
		envActivityLogVOV2.setActStatus("complete");
		envActivityLogService.logActivity(envActivityLogVOV2);
		publisher.publishEvent(new RestResponseEvent("User created in keycloak", true, "keycloak"));

	}

	// NUBONS User Captuore in Registration
	// User will be captured here against an account. No User creation in keycloak.
	// User will be created in keycloak while environment creation will be
	// triggered.

	public void captureNnpAccAdminUser(NNPAccountVO nnpAccVO) {

		UserV2 userV2 = modelMapper.map(nnpAccVO.getUser(), UserV2.class);

		userV2.setAccId(nnpAccVO.getAccId());
		userV2.setRequestDate(LocalDateTime.now());
		userV2.setUpdateDate(LocalDateTime.now());
		userV2.setUserType("admin");
		userV2.setUserStatus("active");
		userV2.setRoleId(getUserRoleDetailsByRoleNameV2("adminRole").getRoleId());
		userV2.setUpdateComment("default admin user");
		userV2 = userRepoV2.saveAndFlush(userV2);
	}

	public UserVOV2 getUserByAccIdAndType(String accId, String userType) {
		return modelMapper.map(userRepoV2.findByAccIdAndUserTypeAndUserStatus(accId, userType, "active"),
				UserVOV2.class);
	}

	public void deleteUserV2(String userId) {
//		log.info("Deleting user: {}", userId);
		UserV2 user = userRepoV2.findById(userId)
				.orElseThrow(() -> new DashboardConfigException(
						new DashboardConfigExceptionMessage("404", "User not found with id: " + userId)));

		// 1. Resolve environment tenant configuration
		EnvironmentVOV2 environment = envServ.getEnvironmentByEnvIdV2(user.getEnvId());
		String tenantId = environment != null ? environment.getEnvTenantId() : "master";

		// 2. Delete user config mapping in database
//		log.info("Deleting user permissions in database");
		userConfigRepoV2.deleteUserByEnv(userId, user.getEnvId());

		// 3. Delete user from GitLab
//		log.info("Deleting user from GitLab");
		try {
			gitlabService.deleteUser(userId);
		} catch (Exception e) {
			log.error("Failed to delete user {} from GitLab: {}", Utils.sanitizeForLog(userId), Utils.sanitizeForLog(e.fillInStackTrace()));
		}

		// 4. Delete user from Keycloak
//		log.info("Deleting user from Keycloak");
		try {
			keycloakService.deleteUser(tenantId, userId);
		} catch (Exception e) {
			log.error("Failed to delete user {} from Keycloak: {}", Utils.sanitizeForLog(userId), Utils.sanitizeForLog(e.fillInStackTrace()));
		}

		// 5. Delete user from Redmine
//		log.info("Deleting user from Redmine");
		try {
			redmineService.deleteUser(userId);
		} catch (Exception e) {
			log.error("Failed to delete user {} from Redmine: {}", Utils.sanitizeForLog(userId), Utils.sanitizeForLog(e.fillInStackTrace()));
		}

		// 6. Delete user record in database
//		log.info("Deleting user record from database");
		userRepoV2.delete(user);
	}
// ─────────────────────────────────────────────────────────────────────────────
// V3: Private hard rollback helper
// ─────────────────────────────────────────────────────────────────────────────

	private void hardRollbackUser(String userId, String tenantId) {

//		log.warn("V3 ROLLBACK START for user: {}", userId);

		// 1st: delete from GitLab via gitlabService → gitlab-integration CommonService
		try {
//			log.warn("V3 ROLLBACK: deleting user {} from GitLab", userId);
			gitlabService.deleteUser(userId);
		} catch (Exception ex) {
			log.error("V3 ROLLBACK: failed GitLab delete for {}: {}", Utils.sanitizeForLog(userId), Utils.sanitizeForLog(ex.fillInStackTrace()));
		}

		// 2nd: delete from Keycloak via keycloakService → keycloak-integration CommonService
		if (keycloakService.isUserAvailableInKeycloak(tenantId,userId) && !tenantId.isEmpty()) {
			try {
//				log.warn("V3 ROLLBACK: deleting user {} from Keycloak (realm: {})", userId, tenantId);
				keycloakService.deleteUser(tenantId, userId);
			} catch (Exception ex) {
				log.error("V3 ROLLBACK: failed Keycloak delete for {}: {}", Utils.sanitizeForLog(userId), Utils.sanitizeForLog(ex.fillInStackTrace()));
			}
		}

		// 3rd: delete from Redmine via redmineService → redmine-integration CommonService
		if (redmineService.isLoginExistsInRedmine(userId)) {
			try {
//				log.warn("V3 ROLLBACK: deleting user {} from Redmine", userId);
				redmineService.deleteUser(userId);
			} catch (Exception ex) {
				log.error("V3 ROLLBACK: failed Redmine delete for {}: {}", Utils.sanitizeForLog(userId), Utils.sanitizeForLog(ex.fillInStackTrace()));
			}
		}

//		log.warn("V3 ROLLBACK COMPLETE for user: {}", userId);
	}

// ─────────────────────────────────────────────────────────────────────────────
// V3: Main registration method
// ─────────────────────────────────────────────────────────────────────────────

	public UserVOV2 registerUserV3(UserVOV2 userVOV2) {

		String  tenantId         = "";

		UserV2 userV2 = modelMapper.map(userVOV2, UserV2.class);
		UserV2 retUserV2;

		// Guard: duplicate check
		if (isUserExistsInDbV2(userV2.getUserId())) {
			log.error("V3: User {} already exists", Utils.sanitizeForLog(userV2.getUserId()));
			throw new DashboardConfigException(
					new DashboardConfigExceptionMessage("409", "User already exists in system"));
		}

		// Guard: account must exist
		NnpAccount acc = nnpAccountRepo.findByAccName(userVOV2.getEnvId()).orElse(null);
		if (acc == null) {
			log.error("V3: Account not found for env {}", Utils.sanitizeForLog(userVOV2.getEnvId()));
			throw new DashboardConfigException(
					new DashboardConfigExceptionMessage("409", "Account does not exist"));
		}

		userV2.setAccId(acc.getAccId());
		if (Objects.isNull(userV2.getRequestDate())) {
			userV2.setRequestDate(LocalDateTime.now());
		}
		userV2.setUserType("user");
		if (userV2.getRoleId() == null || userV2.getRoleId().equals(""))
			userV2.setRoleId(getUserRoleDetailsByRoleNameV2("userRole").getRoleId());

		publisher.publishEvent(accCommListener.createAccUserActivityEvent(userV2,
				"V3 User Creation Started. User id - " + userVOV2.getUserId(),
				"V3 User Creation Started. User id - " + userVOV2.getUserId(),
				"V3 User Creation Started. User id - " + userVOV2.getUserId()));

		try {
			// ── WRITE 1: Save user to DB ──────────────────────────────────────
			retUserV2 = userRepoV2.saveAndFlush(userV2);
//			log.info("V3 WRITE 1: user {} saved to DB", userV2.getUserId());

			// ── Resolve environment ───────────────────────────────────────────
			EnvironmentVOV2 environmentVOV2 = envServ.getEnvironmentByEnvIdV2(userVOV2.getEnvId());
			tenantId = environmentVOV2.getEnvTenantId();
//			log.info("V3: environment resolved — realm: {}", tenantId);

			// ── Guard: Keycloak duplicates ────────────────────────────────────
			// keycloakService → KeycloakClient → keycloak-integration CommonService
			if (keycloakService.isUserAvailableInKeycloak(tenantId, userVOV2.getUserId())) {
				throw new DashboardConfigException(
						new DashboardConfigExceptionMessage("409", "User already exists in Keycloak"));
			}
			if (keycloakService.isEmailAvailableInKeycloak(tenantId, userVOV2.getEmailId())) {
				throw new DashboardConfigException(
						new DashboardConfigExceptionMessage("409", "User email already exists in Keycloak"));
			}

			// ── WRITE 2: Create user in Keycloak (disabled initially) ─────────
			// keycloakService → KeycloakClient → keycloak-integration CommonService
			UserCredential userCredential = UserCredential.builder()
					.type("password").value(userVOV2.getPassword()).temporary(false).build();
			UserAccess userAccess = UserAccess.builder()
					.manageGroupMembership(true).view(true).mapRoles(true)
					.impersonate(true).manage(true).build();
			UserAttributes userAttributes = UserAttributes.builder()
					.contactNumber(new String[]{userVOV2.getContactNumber()}).build();
			UserCreation userCreationRequest = UserCreation.builder()
					.username(userVOV2.getUserId())
					.email(userVOV2.getEmailId())
					.firstName(userVOV2.getFirstName())
					.lastName(userVOV2.getLastName())
					.emailVerified(true)
					.enabled(false)
					.groups(Arrays.asList(userGroup))
					.credentials(Arrays.asList(userCredential))
					.attributes(userAttributes)
					.access(userAccess)
					.build();
			keycloakService.createUser(userCreationRequest, tenantId);
//			log.info("V3 WRITE 2: user {} created in Keycloak (disabled)", userV2.getUserId());
			// ── WRITE 3: Create user in Redmine — BLOCKING ───────────────────
			//   → webClientRedmine → redmine-integration CommonService → Redmine
			EnvActivityLogVOV2 logVO = new EnvActivityLogVOV2();
			logVO.setUserId(userVOV2.getUserId());
			logVO.setEnvId(environmentVOV2.getEnvId());

			try {
				Map<String,Object> requestMap= new HashMap<>();
				requestMap.put("login",userVOV2.getUserId());
				requestMap.put("firstname",userVOV2.getFirstName());
				requestMap.put("lastname",userVOV2.getLastName());
				requestMap.put("mail",userVOV2.getEmailId());
				requestMap.put("password",userVOV2.getPassword());
				requestMap.put("projectName",userVOV2.getEnvId());
				requestMap.put("identifier",userVOV2.getEnvId().toLowerCase());
				requestMap.put("userType",userVOV2.getUserType());
				redmineService.onboardRedmine(requestMap,logVO);
            } catch (Exception redmineCallEx) {
				// The create call can time out reading the response even though
				// Redmine already committed the write (confirmed 201 Created in
				// redmine-integration logs while this client was still waiting
				// on the body). Don't assume failure — reconcile against actual
				// state before deciding whether this is a real failure.
//				log.warn("V3: Redmine createUser call raised {} for user {} — reconciling actual state before deciding outcome",
//						redmineCallEx.getClass().getSimpleName(), userV2.getUserId());
				boolean actuallyExists;
				try {
					actuallyExists = Boolean.TRUE.equals(redmineService.isLoginExistsInRedmine(userV2.getUserId()));
				} catch (Exception reconcileEx) {
					// Can't confirm either way — treat as a real failure and let
					// rollback attempt the delete (harmless no-op if it turns out
					// the user was never created; safer than assuming success).
					log.error("V3: Redmine reconciliation check failed for user {} — treating as failed: {}",
							Utils.sanitizeForLog(userV2.getUserId()), Utils.sanitizeForLog(reconcileEx.getMessage()));
					throw redmineCallEx;
				}
				if (!actuallyExists) {
					// Genuinely never got created — this is a real failure,
					// rollback correctly skips Redmine since redmineCreated stays false.
					throw redmineCallEx;
				}
				// Redmine did create the user despite the client-side exception.
				// Recover instead of discarding a successful write: mark it
				// created and fall through to continue the rest of provisioning,
				// rather than triggering a full hard rollback for no reason.
//				log.warn("V3: Redmine user {} exists despite client-side exception — recovering and continuing provisioning instead of rolling back",
//						userV2.getUserId());
			}
			envActivityLogService.logActivity(logVO);
//			log.info("V3 WRITE 3: user {} created in Redmine", userV2.getUserId());

			// ── WRITE 4: Update DB user status + resolve role ─────────────────
			boolean isGrpChangeRequired = false;
			boolean isAdminMailTemplate = false;
			userV2.setUpdateDate(LocalDateTime.now());
			userV2.setUserStatus("inProgress");
			RoleVOV2 roleVOV2 = getUserRoleDetailsByRoleIdV2(userV2.getRoleId());
			if (roleVOV2 != null) {
				if (roleVOV2.getRoleName().equals("adminRole")) {
					isGrpChangeRequired = true;
					isAdminMailTemplate  = true;
					userV2.setUserType("admin");
				} else if (roleVOV2.getRoleName().equals("superAdminRole")) {
					isGrpChangeRequired = true;
					userV2.setUserType("superAdmin");
				} else {
					userV2.setUserType("user");
				}
			}
			userRepoV2.saveAndFlush(userV2);
//			log.info("V3 WRITE 4: user {} status set to inProgress", userV2.getUserId());

			// ── WRITE 5: Save permissions (NNP_ENV_USER_ACCESS) ──────────────
			List<UserConfigV2> userConfigV2List = new ArrayList<>();
			roleElemRepoV2.findByRoleIdAndEnvId(userV2.getRoleId(), userV2.getEnvId())
					.forEach(roleElemV2 -> {
						UserConfigV2 cfg = UserConfigV2.builder()
								.userAccessId("USR_ACC_" + idRepo.getNextSeqVal())
								.userId(userV2.getUserId())
								.envId(userV2.getEnvId())
								.chElmDetailId(roleElemV2.getChElmDetailId())
								.build();
						userConfigV2List.add(cfg);
					});
			userConfigRepoV2.saveAllAndFlush(userConfigV2List);
//			log.info("V3 WRITE 5: permissions saved for user {}", userV2.getUserId());

			// ── WRITE 6: Enable user in Keycloak ─────────────────────────────
			// keycloakService → KeycloakClient → keycloak-integration CommonService
			UserUpdate enableUpdate = UserUpdate.builder().enabled(true).build();
			keycloakService.updateUser(enableUpdate, tenantId, userVOV2.getUserId());
//			log.info("V3 WRITE 6: user {} enabled in Keycloak", userV2.getUserId());

			if (isGrpChangeRequired) {
				keycloakService.changeUserGroupMembership(tenantId, userVOV2.getUserId(), userGroup, adminGroup);
//				log.info("V3 WRITE 6b: group membership updated for user {}", userV2.getUserId());
			}

			// ── WRITE 7: Onboard user in GitLab — BLOCKING ───────────────────
			// gitlabService.onboardGitLabBlocking()
			//   → webClientGitlab → gitlab-integration CommonService → GitLab
			// NOTE: GitLab's real API (POST /api/v4/users) requires a
			// non-blank password and returns 400 "password can't be blank"
			// without it — that 400 was surfacing here as an opaque 500 from
			// gitlab-integration-service. Reuse the same password already
			// used for the Keycloak credential at WRITE 2 rather than
			// inventing a separate one.
			Map<String, String> requestMapGitlab = new HashMap<>();
			requestMapGitlab.put("userName", userVOV2.getUserId());
			if (userVOV2.getPassword() == null || userVOV2.getPassword().isBlank()) {
				throw new DashboardConfigException(new DashboardConfigExceptionMessage("400",
						"Cannot onboard user " + userVOV2.getUserId() + " to GitLab: password is missing"));
			}
			requestMapGitlab.put("password", userVOV2.getPassword());
			requestMapGitlab.put("email",    userVOV2.getEmailId());
			requestMapGitlab.put("name",     userVOV2.getFirstName() + " " + userVOV2.getLastName());
			requestMapGitlab.put("groupName", environmentVOV2.getEnvRepo());
			requestMapGitlab.put("userType",  userV2.getUserType());

			gitlabService.onboardGitLabBlocking(requestMapGitlab);
//			log.info("V3 WRITE 7: user {} onboarded in GitLab", userV2.getUserId());

			// ── WRITE 8: Mark user as active in DB ───────────────────────────
			userV2.setUpdateDate(LocalDateTime.now());
			userV2.setUserStatus("active");
			userRepoV2.saveAndFlush(userV2);
//			log.info("V3 WRITE 8: user {} marked active", userV2.getUserId());

			// ── EMAIL: Non-fatal — log on failure, never throw ───────────────
			try {
				sendUserWelcomeNote(modelMapper.map(userV2, UserVOV2.class));
				sendAdminUsrRegNotification(userV2);
			} catch (Exception emailEx) {
//				log.error("V3: welcome email failed for {} (non-fatal): {}", userV2.getUserId(), emailEx.getMessage());
			}
			try {
				EmailVO emailVO = new EmailVO();
				emailVO.setFrom(welcomeFromEmail);
				emailVO.setFromName(welcomeFromName);
				emailVO.setTo(userV2.getEmailId());
				emailVO.setToName(userV2.getFirstName() + " " + userV2.getLastName());
				emailVO.setSubject(welcomeSubject);
				emailVO.setText((isAdminMailTemplate)
						? environmentVOV2.getAdmCommunication()
						: environmentVOV2.getUsrCommunication());
				mailService.sendMailWithAttachement(emailVO, createAttachments(environmentVOV2, isAdminMailTemplate));
			} catch (Exception emailEx) {
				log.error("V3: welcome email (attachment) failed for {} (non-fatal): {}", Utils.sanitizeForLog(userV2.getUserId()), Utils.sanitizeForLog(emailEx.getMessage()));
			}

			publisher.publishEvent(accCommListener.createAccUserActivityEvent(userV2,
					"V3 User Creation Done. User id - " + userVOV2.getUserId(),
					"V3 User Creation Done. User id - " + userVOV2.getUserId(),
					"V3 User Creation Done. User id - " + userVOV2.getUserId()));

//			log.info("V3: registerUserV3 SUCCESS for user {}", userV2.getUserId());
			return modelMapper.map(retUserV2, UserVOV2.class);

		} catch (Exception e) {
			log.error("V3: FAILURE for user {} — triggering full hard rollback. Reason: {}",
					Utils.sanitizeForLog(userV2.getUserId()), Utils.sanitizeForLog(e.getMessage()));
			hardRollbackUser(userV2.getUserId(), tenantId);
			throw e;
		}
	}
	/**
	 * V3: Enhanced user existence check across ALL systems.
	 * Checks: DB, Keycloak, Redmine, GitLab
	 *
	 * @param userIdentifier  userId or email address
	 * @param envId           environment id — used to resolve Keycloak realm
	 * @return Map with keys: "existsInDb", "existsInKeycloak", "existsInRedmine", "existsInGitLab", "existsInAny"
	 */
	public Map<String, Object> checkUserExistsAcrossAllSystems(String userIdentifier, String envId) {

		Map<String, Object> result = new HashMap<>();
		String trimmedIdentifier = userIdentifier.trim();

		// ── 1. Check DB ───────────────────────────────────────────────────────
		boolean existsInDb = false;
		try {
			existsInDb = !userRepoV2.findByUserIdOrUserEmail(trimmedIdentifier).isEmpty();
//			log.info("V3 EXISTS CHECK — DB: user {} existsInDb={}", trimmedIdentifier, existsInDb);
		} catch (Exception e) {
			log.error("V3 EXISTS CHECK — DB check failed for {}: {}", Utils.sanitizeForLog(trimmedIdentifier), Utils.sanitizeForLog(e.getMessage()));
		}
		result.put("existsInDb", existsInDb);

		// ── 2. Check Keycloak ─────────────────────────────────────────────────
		// keycloakService → KeycloakClient → keycloak-integration CommonService
		boolean existsInKeycloak = false;
		try {
			EnvironmentVOV2 environmentVOV2 = envServ.getEnvironmentByEnvIdV2(envId);
			String tenantId = environmentVOV2.getEnvTenantId();
			// Check by userId (username)
			existsInKeycloak = keycloakService.isUserAvailableInKeycloak(tenantId, trimmedIdentifier);
			// If not found by userId, also check by email
			if (!existsInKeycloak) {
				existsInKeycloak = keycloakService.isEmailAvailableInKeycloak(tenantId, trimmedIdentifier);
			}
//			log.info("V3 EXISTS CHECK — Keycloak: user {} existsInKeycloak={}", trimmedIdentifier, existsInKeycloak);
		} catch (Exception e) {
			log.error("V3 EXISTS CHECK — Keycloak check failed for {}: {}", Utils.sanitizeForLog(trimmedIdentifier),Utils.sanitizeForLog( e.getMessage()));
		}
		result.put("existsInKeycloak", existsInKeycloak);

		// ── 3. Check Redmine ──────────────────────────────────────────────────
		// redmineService → webClientRedmine → redmine-integration CommonService
		boolean existsInRedmine = false;
		try {
			existsInRedmine = redmineService.isLoginExistsInRedmine(trimmedIdentifier);
//			log.info("V3 EXISTS CHECK — Redmine: user {} existsInRedmine={}", trimmedIdentifier, existsInRedmine);
		} catch (Exception e) {
			log.error("V3 EXISTS CHECK — Redmine check failed for {}: {}", Utils.sanitizeForLog(trimmedIdentifier),Utils.sanitizeForLog( e.getMessage()));
		}
		result.put("existsInRedmine", existsInRedmine);

		// ── 4. Check GitLab ───────────────────────────────────────────────────
		// gitlabService → webClientGitlab → gitlab-integration CommonService
		boolean existsInGitLab = false;
		try {
			existsInGitLab = gitlabService.isUserExistsInGitLab(trimmedIdentifier);
//			log.info("V3 EXISTS CHECK — GitLab: user {} existsInGitLab={}", trimmedIdentifier, existsInGitLab);
		} catch (Exception e) {
			log.error("V3 EXISTS CHECK — GitLab check failed for {}: {}", Utils.sanitizeForLog(trimmedIdentifier), Utils.sanitizeForLog(e.getMessage()));
		}
		result.put("existsInGitLab", existsInGitLab);

		// ── 5. existsInAny = true if found in ANY system ──────────────────────
		boolean existsInAny = existsInDb || existsInKeycloak || existsInRedmine || existsInGitLab;
		result.put("existsInAny", existsInAny);

//		log.info("V3 EXISTS CHECK COMPLETE — user: {} result: {}", trimmedIdentifier, result);
		return result;
	}

}