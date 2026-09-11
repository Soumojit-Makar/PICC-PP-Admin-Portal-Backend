package com.nnp.dashboard.service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.nnp.dashboard.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.nnp.dashboard.event.RestResponseEvent;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.model.UserV2;
import com.nnp.dashboard.vo.EnvActivityLogVOV2;
import com.nnp.dashboard.vo.git.User;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Integration service for GitLab, talking to the `gitlab-integration`
 * CommonService through a dedicated {@link WebClient} bean
 * ({@code webClientGitInt} - see {@code DashboardConfiguratorConfig}).
 *
 * Responsibilities:
 *   - Creating the GitLab group + default user when a new environment is provisioned
 *   - Onboarding an existing user into a group
 *   - Existence checks for groups and users (used for validation &amp; rollback)
 *   - Deleting users / groups
 *
 * The async methods publish a {@link RestResponseEvent} on success/failure;
 * the {@link com.nnp.dashboard.event.listener.LatchEventListener} counts these
 * events so the caller can await completion.
 */
@Service
@Slf4j
public class GitlabService {

    @Qualifier("webClientGitInt")
    @Autowired
    private WebClient webClientGitlab;
    
    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private EnvActivityLogService envActivityLogService;
    
    public void deleteUser(String userId) {
    	try {
            User usr = webClientGitlab.get()
                    .uri(uriBuilder -> uriBuilder
                    		.path("/api/getUser")
                    		.queryParam("username", userId)
                    		.build())
                    .retrieve()
                    .bodyToMono(User.class)
                    .doOnError(err-> handleErr("getUser call ",err))
                    .onErrorResume(err -> Mono.just(new User()))
                    .block();

            if(usr!=null && usr.getId()!=null) {
            	webClientGitlab.delete()
                .uri(uriBuilder -> uriBuilder
                		.path("/api/deleteUser")
                		.queryParam("id", usr.getId())
                		.build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(err-> handleErr("deleteUser call ",err))
                .onErrorResume(err -> Mono.just("Error in deleteUser call "+err.getMessage()))
                .block();
            }
                   
            
        } catch (Exception e) {
            log.error("$$ inside exception handler exception --> {}", Utils.sanitizeForLog(e.getMessage()));
            throw new RuntimeException(e);
        }

    }

    private void handleErr(String s ,Throwable err) {
		log.info("Error during {}",Utils
        .sanitizeForLog(s+err.getMessage()));
		throw new RuntimeException(err);
	}

    public void onboardGitLab(Map<String, String> requestMap, EnvActivityLogVOV2 envActivityLogVOV2) {

        //log.info("isCallError == {}", isCallError);
        try {
            webClientGitlab.post()
                    .uri("/api/v1/onboard/user")
                    .body(BodyInserters.fromValue(requestMap))
                    .retrieve()
                    //.onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(new DashboardConfigException("GitlabInt call failed" + clientResponse.createException())))
                    //.onStatus(HttpStatusCode::isError, ClientResponse::createException)
                    .bodyToMono(String.class)
                    .subscribe(s -> handleResponse(s, envActivityLogVOV2), throwable -> handleError(throwable, envActivityLogVOV2));

        } catch (Exception e) {
            log.error("$$ inside exception handler exception --> {}", Utils.sanitizeForLog(e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public void createGitGroupAndDefaultUser(Map<String, String> requestMap, EnvActivityLogVOV2 envActivityLogVOV2) {
        webClientGitlab.post()
                .uri("/api/createEnvironment")
                .body(BodyInserters.fromValue(requestMap))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    // Publish event
                    log.info("Publishing event from GitLab************************");
                    publisher.publishEvent(new RestResponseEvent("GitLab action done", true, "gitlab"));
                })
                .doOnError(throwable ->
                        publisher.publishEvent(new RestResponseEvent(
                                "GitLab action failed: " + extractDetail(throwable), false, "gitlab")))
                .subscribe(s -> logResponse(s, envActivityLogVOV2), throwable -> logError(throwable, envActivityLogVOV2));
    }

    private String extractDetail(Throwable throwable) {
        if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
            String body = wcre.getResponseBodyAsString();
            return !body.isBlank() ? body : wcre.getMessage();
        }
        return throwable.getMessage();
    }
    public Boolean isGroupExistsInGit(String groupName) {
        ResponseEntity<Boolean> booleanResponseEntity;
        try {
            booleanResponseEntity = webClientGitlab.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/exist/group/{groupName}")
                            .build(groupName))
                    .retrieve()
                    .toEntity(Boolean.class)
                    .toFuture()
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            //throw new RuntimeException(e);
            log.error("Gitlab call failed exception message --> {}", Utils.sanitizeForLog(e.getMessage()));
            throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Gitlab call failed"), e);
        }
        return booleanResponseEntity.getBody();
    }

    public Boolean isGroupExistsInGitNew(String groupName) {
        Mono<Boolean> booleanMono = webClientGitlab.get()
                .uri(uriBuilder -> uriBuilder.path("/api/exist/group/{groupName}")
                        .build(groupName))
                .retrieve()
                .bodyToMono(Boolean.class);
        return booleanMono.block();
    }

    private void handleError(Throwable throwable, EnvActivityLogVOV2 envActivityLogVOV2) {
        log.error("GitlabService --> handleError --> {}", Utils.sanitizeForLog(throwable.getMessage()));
        envActivityLogVOV2.setActDesc("user activation in gitlab");
        envActivityLogVOV2.setActNote("activate-gitlab");
        envActivityLogVOV2.setActStatus("Failed");
        envActivityLogService.logActivity(envActivityLogVOV2);
    }


    private void handleResponse(String s, EnvActivityLogVOV2 envActivityLogVOV2) {
        log.info("GitlabService --> handleResponse --> {}", Utils.sanitizeForLog(s));
        envActivityLogVOV2.setActDesc("user activation in gitlab");
        envActivityLogVOV2.setActNote("activate-gitlab");
        envActivityLogVOV2.setActStatus("complete");
        envActivityLogService.logActivity(envActivityLogVOV2);
    }

    private void logResponse(String s, EnvActivityLogVOV2 envActivityLogVOV2) {
        log.info("response from gitlab createEnvironment --> {}", Utils.sanitizeForLog(s));
        envActivityLogVOV2.setActDesc("group and default user creation in gitlab");
        envActivityLogVOV2.setActNote("group-default-user-creation-gitlab");
        envActivityLogVOV2.setActStatus("complete");
    }

    private void logError(Throwable throwable, EnvActivityLogVOV2 envActivityLogVOV2) {
        log.error("error from gitlab createEnvironment --> {}", Utils.sanitizeForLog(throwable.getMessage()));
        envActivityLogVOV2.setActDesc("group and default user creation in gitlab");
        envActivityLogVOV2.setActNote("group-default-user-creation-gitlab");
        envActivityLogVOV2.setActStatus("failed");
    }
    /**
     * V3: Synchronous (blocking) GitLab user onboard.
     * Uses webClientGitlab → gitlab-integration CommonService → GitLab.
     * Throws on failure so the caller can trigger rollback.
     */
    public void onboardGitLabBlocking(Map<String, String> requestMap) {
        log.info("V3: onboarding GitLab user (blocking): {}", Utils.sanitizeForLog(requestMap.get("userName")));
        webClientGitlab.post()
                .uri("/api/v1/onboard/user")
                .body(BodyInserters.fromValue(requestMap))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info("V3: GitLab onboard completed (blocking): {}", Utils.sanitizeForLog(requestMap.get("userName")));
    }
    /**
     * Checks whether a user exists in GitLab by username.
     * Goes via: webClientGitlab → gitlab-integration CommonService → GitLab
     */
    public boolean isUserExistsInGitLab(String username) {
        try {
            com.nnp.dashboard.vo.git.User usr = webClientGitlab.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/getUser")
                            .queryParam("username", username)
                            .build())
                    .retrieve()
                    .bodyToMono(com.nnp.dashboard.vo.git.User.class)
                    .block();
            return usr != null && usr.getId() != null;
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
            if (wcre.getStatusCode().value() == 417) {
                // gitlab-integration's convention for "no such user" - confirmed absent
                return false;
            }
            log.error("GitLab existence check genuinely failed for user {}: {}", Utils.sanitizeForLog(username), Utils.sanitizeForLog(wcre.getMessage()));
            throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
                    "Could not verify GitLab user state for " + username + " before creation: " + wcre.getMessage()));
        } catch (Exception e) {
            log.error("GitLab existence check genuinely failed for user {}: {}", Utils.sanitizeForLog(username), Utils.sanitizeForLog(e.getMessage()));
            throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
                    "Could not verify GitLab user state for " + username + " before creation: " + e.getMessage()));
        }
    }
    public void deleteGroup(String groupName) {
        try {
            webClientGitlab.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/deleteGroup") // Calls your CommonService
                            .queryParam("groupName", groupName)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("GitLab Group {} deleted successfully", Utils.sanitizeForLog(groupName));
        } catch (Exception e) {
            log.error("Failed to delete GitLab Group: {}",Utils.sanitizeForLog( e.getMessage()));
        }
    }

}
