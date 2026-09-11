package com.nnp.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nnp.dashboard.event.RestResponseEvent;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.utils.Utils;
import com.nnp.dashboard.vo.EnvActivityLogVOV2;
import com.nnp.dashboard.vo.rm.Issue;
import com.nnp.dashboard.vo.rm.ResponseUser;
import com.nnp.dashboard.vo.rm.RootProject;
import com.nnp.dashboard.vo.rm.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Integration service for Redmine, talking to the `redmine-integration`
 * CommonService through a dedicated {@link WebClient} bean
 * ({@code webClientRedmineInt}).
 *
 * Responsibilities:
 *   - Creating the root project + default user when a new environment is provisioned
 *   - Creating developer users and support tickets
 *   - Existence checks for projects and logins (used for validation &amp; rollback)
 *   - Deleting users / projects
 *
 * Like {@link GitlabService}, the async provisioning calls publish
 * {@link RestResponseEvent} events awaited by {@link LatchEventListener}.
 */
@Service
@Slf4j
public class RedmineService {

	@Qualifier("webClientRedmineInt")
	@Autowired
	private WebClient webClientRedmine;

	@Autowired
	private EnvActivityLogService envActivityLogService;

	@Value("${redmine.apiKey:}")
	private String redmineApiKey;

	@Value("${redmine.support.apiKey:}")
	private String redmineSupportApiKey;
	
    @Autowired
    private ApplicationEventPublisher publisher;

	public void onboardRedmine(Map<String, Object> requestMap, EnvActivityLogVOV2 envActivityLogVOV2) {
		webClientRedmine.post()
				.uri(uriBuilder -> uriBuilder.path("/api/onboarduser").queryParam("apiKey", redmineApiKey).build())
				.body(BodyInserters.fromValue(requestMap)).retrieve().bodyToMono(Boolean.class)
				.subscribe(aBoolean -> handleResponse(aBoolean, envActivityLogVOV2),
						throwable -> handleError(throwable, envActivityLogVOV2));

	}

	public void createRedmineProjectAndDefaultUser(Map<String, String> requestMap,
	                                               EnvActivityLogVOV2 envActivityLogVOV2) {
		webClientRedmine.post()
				.uri(uriBuilder -> uriBuilder.path("/api/createRootProject").queryParam("apiKey", redmineApiKey)
						.build())
				.body(BodyInserters.fromValue(requestMap)).retrieve().bodyToMono(RootProject.class)
				.doOnSuccess(response -> {
					// Publish event
//					log.info("Publishing event for Redmine ***************** ");
					publisher.publishEvent(new RestResponseEvent("Redmine Action Done", true, "redmine"));
				})
				.doOnError(throwable ->
						publisher.publishEvent(new RestResponseEvent(
								"Redmine action failed: " + extractDetail(throwable), false, "redmine")))
				.subscribe(rootProject -> logResponse(rootProject, envActivityLogVOV2),
						throwable -> logError(throwable, envActivityLogVOV2));
	}

	private String extractDetail(Throwable throwable) {
		if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
			String body = wcre.getResponseBodyAsString();
			return (body != null && !body.isBlank()) ? body : wcre.getMessage();
		}
		return throwable.getMessage();
	}
	public Boolean isProjectExistsInRedmine(String project) {
		ResponseEntity<Boolean> booleanResponseEntity;
		try {
			booleanResponseEntity = webClientRedmine.get()
					.uri(uriBuilder -> uriBuilder.path("/api/checkProjectExists").queryParam("project", project)
							.queryParam("apiKey", redmineApiKey).build())
					.retrieve().toEntity(Boolean.class).toFuture().get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Redmine call failed exception message --> {}", Utils.sanitizeForLog(e.fillInStackTrace().toString()));
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Redmine call failed"), e);
		}
		return booleanResponseEntity.getBody();
	}

	public Boolean isLoginExistsInRedmine(String login) {
		ResponseEntity<Boolean> booleanResponseEntity;
		try {
			booleanResponseEntity = webClientRedmine.get()
					.uri(uriBuilder -> uriBuilder.path("/api/checkLoginExists").queryParam("login", login)
							.queryParam("apiKey", redmineApiKey).build())
					.retrieve().toEntity(Boolean.class).toFuture().get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Redmine call failed exception message --> {}", Utils.sanitizeForLog(e.fillInStackTrace().toString()));
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Redmine call failed"), e);
		}
		return booleanResponseEntity.getBody();
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public String createSupportIssue(Issue issue) {
		ResponseEntity<String> stringResponseEntity;
		try {
			stringResponseEntity = webClientRedmine.post()
					.uri(uriBuilder -> uriBuilder.path("/api/createSupportIssue")
							.queryParam("apiKey", redmineSupportApiKey).build())
					.body(BodyInserters.fromValue(issue)).retrieve().toEntity(String.class).toFuture().get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Redmine support ticket creation call failed exception message --> {}", Utils.sanitizeForLog(e.fillInStackTrace().toString()));
			throw new DashboardConfigException(
					new DashboardConfigExceptionMessage("500", "Redmine support ticket creation call failed"), e);
		}
		return stringResponseEntity.getBody();
	}

	private void handleError(Throwable throwable, EnvActivityLogVOV2 envActivityLogVOV2) {
//		log.error("RedmineService --> handleError --> {}", throwable.getMessage());
		envActivityLogVOV2.setActDesc("user activation in redmine");
		envActivityLogVOV2.setActNote("activate-redmine");
		envActivityLogVOV2.setActStatus("Failed");
		envActivityLogService.logActivity(envActivityLogVOV2);
	}

	private void handleResponse(Boolean aBoolean, EnvActivityLogVOV2 envActivityLogVOV2) {
//		log.info("RedmineService --> handleResponse --> " + aBoolean);
		envActivityLogVOV2.setActDesc("user activation in redmine");
		envActivityLogVOV2.setActNote("activate-redmine");
		envActivityLogVOV2.setActStatus("complete");
		envActivityLogService.logActivity(envActivityLogVOV2);
	}

	private void logResponse(RootProject rootProject, EnvActivityLogVOV2 envActivityLogVOV2) {
//		log.info("response from redmine createEnvironment --> name --> {} -- login --> {}", rootProject.getName(),
//				rootProject.getLogin());
		envActivityLogVOV2.setActDesc("project and default user creation in redmine");
		envActivityLogVOV2.setActNote("project-default-user-creation-redmine");
		envActivityLogVOV2.setActStatus("complete");
	}

	private void logResponse(ResponseUser user, EnvActivityLogVOV2 envActivityLogVOV2) {
//		log.info("response from redmine create redmine dev user --> name --> {} -- login --> {}",
//				user.getUser().getFirstname() + " " + user.getUser().getLastname(), user.getUser().getLogin());
		envActivityLogVOV2
				.setActDesc("user creation in redmine for normal developer user onboard of existing environment");
		envActivityLogVOV2.setActNote("project-dev-user-creation-redmine");
		envActivityLogVOV2.setActStatus("complete");
	}

	private void logError(Throwable throwable, EnvActivityLogVOV2 envActivityLogVOV2) {
		log.error("error from redmine createEnvironment --> {}",Utils.sanitizeForLog( throwable.getMessage()));
		envActivityLogVOV2.setActDesc("project and default user creation in redmine");
		envActivityLogVOV2.setActNote("project-default-user-creation-redmine");
		envActivityLogVOV2.setActStatus("failed");
	}

	public void createRedmineDeveloperUser(User redmineUser, EnvActivityLogVOV2 envActivityLogVOV2) {
		webClientRedmine.post()
				.uri(uriBuilder -> uriBuilder.path("/api/createUser").queryParam("apiKey", redmineApiKey).build())
				.body(BodyInserters.fromValue(redmineUser)).retrieve().bodyToMono(ResponseUser.class)
				.subscribe(user -> logResponse(user, envActivityLogVOV2),
						throwable -> logDevUserCreateError(throwable, envActivityLogVOV2));
	}

	private void logDevUserCreateError(Throwable throwable, EnvActivityLogVOV2 envActivityLogVOV2) {
		log.error("error from redmine create redmine dev user --> {}", Utils.sanitizeForLog(throwable.getMessage()));
		envActivityLogVOV2.setActDesc("user creation in redmine for normal developer user onboard of existing environment");
		envActivityLogVOV2.setActNote("project-dev-user-creation-redmine");
		envActivityLogVOV2.setActStatus("failed");
	}

	public void deleteUser(String username) {
		try {
			ResponseEntity<Map> usersResponse = webClientRedmine.get()
					.uri(uriBuilder -> uriBuilder.path("/api/getUsers")
							.queryParam("apiKey", redmineApiKey).build())
					.retrieve().toEntity(Map.class).toFuture().get();

			Map<String, Object> usersMap = usersResponse.getBody();
			if (usersMap != null && usersMap.containsKey(username)) {
				Object userIdObj = usersMap.get(username);
				String redmineId = String.valueOf(userIdObj);
//				log.info("Deleting user {} from Redmine with ID {}", username, redmineId);

				webClientRedmine.delete()
						.uri(uriBuilder -> uriBuilder.path("/api/deleteUser")
								.queryParam("id", redmineId)
								.queryParam("apiKey", redmineApiKey).build())
						.retrieve().toEntity(String.class).toFuture().get();
			} else {
				log.error("User {} not found in Redmine, skipping deletion.", Utils.sanitizeForLog(username));
			}
		} catch (Exception e) {
			log.error("Failed to delete user {} from Redmine: {}", Utils.sanitizeForLog(username), Utils.sanitizeForLog(e.getMessage()));
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Redmine deletion call failed: " + e.getMessage()), e);
		}
	}

	/**
	 * V3: Synchronous (blocking) Redmine user creation.
	 * Uses webClientRedmine → redmine-integration CommonService → Redmine.
	 * Throws on failure so the caller can trigger rollback.
	 */
	public void createRedmineDeveloperUserBlocking(User redmineUser) {
//		log.info("V3: creating Redmine user (blocking) for login: {}", redmineUser.getUser().getLogin());
		webClientRedmine.post()
				.uri(uriBuilder -> uriBuilder
						.path("/api/createUser")
						.queryParam("apiKey", redmineApiKey)
						.build())
				.body(BodyInserters.fromValue(redmineUser))
				.retrieve()
				.bodyToMono(ResponseUser.class)
				.block();
//		log.info("V3: Redmine user created (blocking) for login: {}", redmineUser.getUser().getLogin());
	}
	public void deleteProject(String projectIdentifier) {
		try {
			webClientRedmine.delete()
					.uri(uriBuilder -> uriBuilder
							.path("/api/deleteProject") // Calls your CommonService
							.queryParam("identifier", projectIdentifier)
							.queryParam("apiKey", redmineApiKey)
							.build())
					.retrieve()
					.bodyToMono(String.class)
					.block();
//			log.info("Redmine Project {} deleted successfully", projectIdentifier);
		} catch (Exception e) {
			log.error("Failed to delete Redmine Project: {}", Utils.sanitizeForLog(e.fillInStackTrace().toString()));
		}
	}


}
