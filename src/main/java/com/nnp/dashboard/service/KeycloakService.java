package com.nnp.dashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.nnp.dashboard.client.KeycloakClient;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.vo.kc.*;

import java.util.List;

/**
 * Integration service for Keycloak identity management.
 *
 * Wraps the HTTP {@link KeycloakClient} (an annotated HTTP interface proxied by
 * Spring {@code HttpServiceProxyFactory}) and adds business-level helpers:
 *   - Existence checks for a user (by userId) or an email within a realm
 *   - Create / update / delete / reset-password of users
 *   - Changing a user's group membership (e.g. user &lt;-&gt; admin group)
 *   - Realm listing and creation
 *
 * NOTE: in this product each environment maps to a Keycloak *realm* that is
 * identified by `envTenantId`; realm + tenant are used interchangeably.
 */
@Service
@Slf4j
public class KeycloakService {

    @Autowired
    private KeycloakClient keycloakClient;

    public boolean isEmailAvailableInKeycloak(String realmName, String email) {
        boolean isExist = false;
        List<UserDetails> userDetailsList = keycloakClient.getAllUsersByRealmName(realmName)
                .stream()
                .filter(userDetails -> userDetails.getEmail() != null && email!= null && userDetails.getEmail().equalsIgnoreCase(email))
                .toList();
        if (userDetailsList != null && !userDetailsList.isEmpty()) {
            isExist = true;
        }
        return isExist;
    }

    public boolean isUserAvailableInKeycloak(String realmName, String userId) {
        boolean isExist = false;
        List<UserDetails> userDetails = keycloakClient.getUsersByUserName(realmName, userId);
        if (userDetails != null && !userDetails.isEmpty()) {
            isExist = true;
        }
        return isExist;
    }

    public List<UserDetails> getUsersByUserName(String realmName, String userId) {
        return keycloakClient.getUsersByUserName(realmName, userId);
    }

    public List<UserDetails> getAllUsers(String realmName) {
        return keycloakClient.getAllUsersByRealmName(realmName);
    }

    public void deleteUser(String realmName, String userIdOrUsername) {
        // Pass the identifier straight through. keycloak-integration-service's
        // deleteUser endpoint already resolves username -> internal ID itself;
        // pre-resolving to a UUID here and sending that instead caused it to
        // compare a UUID against every user's *username* field, which can
        // never match — that's what produced the persistent
        // "...getAllUsersByRealmName-user not found" failures. All call sites
        // in this codebase pass a login/username here, never a pre-known UUID,
        // so this is safe.
        keycloakClient.deleteUser(realmName, userIdOrUsername);
    }

    public void createUser(UserCreation userCreationRequest, String realmName) {
        keycloakClient.createUser(userCreationRequest, realmName);
    }

    public void updateUser(UserUpdate userUpdateRequest, String realmName, String userIdOrUsername) {
        // See note in deleteUser() above — same reasoning applies here.
        keycloakClient.updateUser(userUpdateRequest, realmName, userIdOrUsername);
    }

    public void resetUserPassword(UserCredential userCredential, String realmName, String userId) {
        keycloakClient.resetUserPassword(userCredential, realmName, userId);
    }

    public void changeUserGroupMembership(String realmName, String userId, String sourceGroupName, String targetGroupName) {
        keycloakClient.updateUserAddGroupMembership(realmName, userId, targetGroupName);
//        log.info("add to group successful for user - {} and group - {}", userId, targetGroupName);
        keycloakClient.updateUserRemoveGroupMembership(realmName, userId, sourceGroupName);
//        log.info("delete from group successful for user - {} and group - {}", userId, sourceGroupName);
    }

    public void executeActionsEmail(String action, String realmName, String userId) {
        if (action.equalsIgnoreCase("ForgotPassword")) {
            keycloakClient.executeActionsEmailResetPass(realmName, userId);
        } else if (action.equalsIgnoreCase("VerifyEmail")) {
            keycloakClient.executeActionsEmailVerifyEmail(realmName, userId);
        } else {
            throw new DashboardConfigException(new DashboardConfigExceptionMessage("400", "invalid action - action -- " + action));
        }
    }

    public void createRealm(CreateRealmRequest createRealmRequest) {
        keycloakClient.createRealm(createRealmRequest);
    }

    public List<RealmDetails> getAllRealmDetails() {
        return keycloakClient.getAllRealmDetails();
    }
}