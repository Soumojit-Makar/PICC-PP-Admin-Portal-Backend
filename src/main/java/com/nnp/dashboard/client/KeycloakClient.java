package com.nnp.dashboard.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.*;

import com.nnp.dashboard.vo.kc.*;

import java.util.List;

/**
 * Declarative HTTP client for the Keycloak admin API (consumed via the
 * `keycloak-integration` CommonService proxied by the keycloak WebClient).
 *
 * The interface is proxied at runtime by Spring's {@code HttpServiceProxyFactory}
 * (see {@code WebClientConfig#keycloakClient()}). Each annotated method maps to
 * a REST call; the prefix {@code /keycloak} is the route mount of the
 * keycloak-integration service that forwards the call to the real Keycloak.
 */
@HttpExchange(url = "/keycloak")
public interface KeycloakClient {
    @GetExchange("/admin/realms/{realmName}/users/{userId}")
    public List<UserDetails> getUsersByUserName(@PathVariable("realmName") String realmName, @PathVariable("userId") String userId);

    //@GetExchange(value = "/admin/realms/users", accept = MediaType.APPLICATION_JSON_VALUE)
    @GetExchange("/admin/realms/{realmName}/users")
    public List<UserDetails> getAllUsersByRealmName(@PathVariable("realmName") String realmName);

    @GetExchange("/admin/realms")
    public List<RealmDetails> getAllRealmDetails();

    //@DeleteExchange(value = "/admin/realms/users/{userId}", contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
    @DeleteExchange("/admin/realms/{realmName}/users/{userId}")
    public void deleteUser(@PathVariable("realmName") String realmName, @PathVariable("userId") String userId);

    @PostExchange(value = "/admin/realms/{realmName}/users", contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
    public void createUser(@RequestBody UserCreation userCreationRequest, @PathVariable("realmName") String realmName);

    @PutExchange(value = "/admin/realms/{realmName}/users/{userId}", contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
    public void updateUser(@RequestBody UserUpdate userUpdateRequest, @PathVariable("realmName") String realmName, @PathVariable("userId") String userId);

    @PutExchange(value = "/admin/realms/{realmName}/users/{userId}/reset-password", contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
    public void resetUserPassword(@RequestBody UserCredential userCredential, @PathVariable("realmName") String realmName, @PathVariable("userId") String userId);

    @PutExchange("/admin/realms/{realmName}/users/{userId}/groups/{groupName}")
    public void updateUserAddGroupMembership(@PathVariable("realmName") String realmName, @PathVariable("userId") String userId, @PathVariable("groupName") String groupName);

    @DeleteExchange("/admin/realms/{realmName}/users/{userId}/groups/{groupName}")
    public void updateUserRemoveGroupMembership(@PathVariable("realmName") String realmName, @PathVariable("userId") String userId, @PathVariable("groupName") String groupName);

    @PutExchange("/admin/realms/{realmName}/users/{userId}/execute-actions-email/forgotpass")
    public void executeActionsEmailResetPass(@PathVariable("realmName") String realmName, @PathVariable("userId") String userId);

    @PutExchange("/admin/realms/{realmName}/users/{userId}/execute-actions-email/verifyemail")
    public void executeActionsEmailVerifyEmail(@PathVariable("realmName") String realmName, @PathVariable("userId") String userId);

    @PostExchange(value = "/admin/realms", contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
    public void createRealm(@RequestBody CreateRealmRequest createRealmRequest);
}
