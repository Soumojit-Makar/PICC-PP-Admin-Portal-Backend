package com.nnp.dashboard.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.nnp.dashboard.service.*;
import com.nnp.dashboard.vo.*;
import com.nnp.dashboard.vo.kc.UserAccess;
import com.nnp.dashboard.vo.kc.UserCreation;
import com.nnp.dashboard.vo.kc.UserCredential;
import com.nnp.dashboard.vo.mail.EmailVO;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for user management and user-access configuration.
 *
 * Exposes endpoints under {@code /user} for:
 *   - Reading / updating the access a user has inside an environment (V3)
 *   - Registering users (V2 / V3 flows) and activating (onboarding) them
 *   - Deleting / migrating users between environments
 *   - Fetching user lists, roles and user status
 *   - Resetting user passwords via Keycloak
 *   - Checking user existence across all downstream systems
 *     (DB + Keycloak + Redmine + GitLab)
 *
 * All business logic is delegated to {@link UserAccessService}.
 */
@RestController
// @CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/user")
@Slf4j
public class UserAccessController {

    @Autowired
    private EnvWrapperService wrapper;
    @Autowired
    private CachingService cache;
    @Autowired
    private KeycloakService keycloakService;
    @Autowired
    private UserAccessService userAccessServ;

    @Autowired
    private MailService mailService;

    @Value("${keycloak.group:nnp-users}")
    private String group;

    // TODO
    @GetMapping(path = "/read/access/v3/env/{envId}/user/{userId}")
    public EnvironmentVOV3 getUserAccessByEnvIdAndUserIdV3(@PathVariable("envId") String envId,
            @PathVariable("userId") String userId) {
        return userAccessServ.getUserAccessByEnvIdAndUserIdV3(envId, userId);
    }

    // TODO
    @PutMapping(path = "/update/access/v3/env/{envId}/user/{userId}")
    public void updateUserAccessV3(@RequestBody List<UserConfigVOV2> userConfigVOV2List,
            @PathVariable("envId") String envId, @PathVariable("userId") String userId) {
        userAccessServ.updateUserAccessV3(userConfigVOV2List, envId, userId);
    }

    // TODO
    @PostMapping(path = "/register/user/v2")
    public UserVOV2 registerUserV2(@RequestBody UserVOV2 userVOV2) {
        return userAccessServ.registerUserV2(userVOV2);
    }

    @DeleteMapping(path = "/delete/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("userId") String userId) {
        userAccessServ.deleteUserV2(userId);
    }
    // TODO
    @PutMapping(path = "/activate/user/v2/{userId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void onboardUserV2(@RequestBody UserVOV2 userVOV2, @PathVariable("userId") String userId) {
        if (userVOV2.getUserId() == null || userVOV2.getUserId().equals(""))
            userVOV2.setUserId(userId);
        // userAccessServ.onboardUserV2(userVOV2);
        CompletableFuture.supplyAsync(() -> {
            userAccessServ.onboardUserV2(userVOV2);
            return null;
        });
    }

    // TODO
    @PutMapping(path = "/update/user/role/v2/{userId}")
    public UserVOV2 updateUserRoleAndTypeV2(@RequestBody UserVOV2 userVOV2, @PathVariable("userId") String userId) {
        userVOV2.setUserId(userId);
        return userAccessServ.updateUserRoleAndTypeV2(userVOV2);
    }

    @GetMapping(path = "/read/user/v2/{userId}")
    public UserVOV2 getUserByUserIdV2(@PathVariable("userId") String userId) {
        return userAccessServ.getUserByUserIdV2(userId);
    }

    @GetMapping(path = "/read/user/status/v2/{status}")
    public List<UserVOV2> getUserListByStatusV2(@PathVariable("status") String status) {
        return userAccessServ.getUserListByStatusV2(status);
    }

    // TODO
    @GetMapping(path = "/read/user/status/v2/{status}/{logedInUserId}")
    public List<UserVOV2> getUserListByStatusV2(@PathVariable("status") String status,
            @PathVariable("logedInUserId") String logedInUserId) {
        return userAccessServ.getUserListByStatusV2(status, logedInUserId);
    }


    // TODO
    @GetMapping(path = "/read/user/v2/env/{envId}/status/{status}")
    public List<UserVOV2> getUserListByEnvIdAndStatusV2(@PathVariable("envId") String envId,
            @PathVariable("status") String status) {
        return userAccessServ.getUserListByEnvIdAndStatusV2(envId, status);
    }

    // TODO
    @GetMapping(path = "/read/user/v2")
    public List<UserVOV2> getAllUserListV2() {
        return userAccessServ.getAllUserListV2();
    }

    // TODO
    @GetMapping(path = "/read/roles/v2")
    public List<RoleVOV2> getAllUserRoleDetailsV2() {
        return userAccessServ.getAllUserRoleDetailsV2();
    }


    // TODO
    @PutMapping(path = "/reset/realm/{realmName}/users/{userId}/password/v2")
    public void resetUserPasswordV2(@RequestBody UserCredentialVOV2 userCredentialVOV2,
            @PathVariable("realmName") String realmName, @PathVariable("userId") String userId) {
        userAccessServ.resetUserPasswordV2(userCredentialVOV2, realmName, userId);
    }

    @GetMapping(path = "/read/user/v2/{envCode}/{userId}")
    public boolean isUserValidForEnv(@PathVariable String envCode, @PathVariable String userId) {
//        log.info("in UserAccessController -> isUserValidForEnv():: " + envCode + ":" + userId);
        return userAccessServ.isUserValidForEnv(envCode, userId);
    }

    // TODO
    @PutMapping(path = "/envmigrate/user/v2/{envId}/{userId}")
    public UserVOV2 migrateEnvForUser(@PathVariable String envId, @PathVariable String userId) {
//        log.info("in UserAccessController -> migrateEnvForUser():: " + envId + ":" + userId);
        return userAccessServ.migrateEnvForUser(envId, userId);
    }

    @GetMapping("exists")
    public boolean doesUserExist(@RequestParam String userIdentifier) {
        // User Identifier Can Be Email Or User Id
        return userAccessServ.ifUserExistsByUserIdentifierV2(userIdentifier);
    }
    @PostMapping(path = "/register/user/v3")
    public UserVOV2 registerUserV3(@RequestBody UserVOV2 userVOV2) {
        return userAccessServ.registerUserV3(userVOV2);
    }
    @GetMapping("exists/v3")
    public Map<String, Object> doesUserExistAcrossAllSystems(
            @RequestParam String userIdentifier,
            @RequestParam String envId) {
        // Checks DB + Keycloak + Redmine + GitLab
        // envId is needed to resolve the correct Keycloak realm
        return userAccessServ.checkUserExistsAcrossAllSystems(userIdentifier, envId);
    }

}
