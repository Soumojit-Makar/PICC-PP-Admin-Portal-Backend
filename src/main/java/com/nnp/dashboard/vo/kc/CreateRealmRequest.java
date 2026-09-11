package com.nnp.dashboard.vo.kc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//@ToString
public class CreateRealmRequest {
	private String id;
	private String realm;
	private int notBefore = 0;
	private String defaultSignatureAlgorithm = "RS256";
	private boolean revokeRefreshToken = false;
	private int refreshTokenMaxReuse = 0;
	private int accessTokenLifespan = 300;
	private int accessTokenLifespanForImplicitFlow = 900;
	private int ssoSessionIdleTimeout = 1800;
	private int ssoSessionMaxLifespan = 36000;
	private int ssoSessionIdleTimeoutRememberMe = 0;
	private int ssoSessionMaxLifespanRememberMe = 0;
	private int offlineSessionIdleTimeout = 2592000;
	private boolean offlineSessionMaxLifespanEnabled = false;
	private int offlineSessionMaxLifespan = 5184000;
	private int clientSessionIdleTimeout = 0;
	private int clientSessionMaxLifespan = 0;
	private int clientOfflineSessionIdleTimeout = 0;
	private int clientOfflineSessionMaxLifespan = 0;
	private int accessCodeLifespan = 60;
	private int accessCodeLifespanUserAction = 300;
	private int accessCodeLifespanLogin = 1800;
	private int actionTokenGeneratedByAdminLifespan = 43200;
	private int actionTokenGeneratedByUserLifespan = 300;
	private int oauth2DeviceCodeLifespan = 600;
	private int oauth2DevicePollingInterval = 5;
	private boolean enabled = true;
	private String sslRequired = "external";
	private boolean registrationAllowed = false;
	private boolean registrationEmailAsUsername = false;
	private boolean rememberMe = false;
	private boolean verifyEmail = false;
	private boolean loginWithEmailAllowed = true;
	private boolean duplicateEmailsAllowed = false;
	private boolean resetPasswordAllowed = false;
	private boolean editUsernameAllowed = false;
	private boolean bruteForceProtected = false;
	private boolean permanentLockout = false;
	private int maxFailureWaitSeconds = 900;
	private int minimumQuickLoginWaitSeconds = 60;
	private int waitIncrementSeconds = 60;
	private int quickLoginCheckMilliSeconds = 1000;
	private int maxDeltaTimeSeconds = 43200;
	private int failureFactor = 30;
}
