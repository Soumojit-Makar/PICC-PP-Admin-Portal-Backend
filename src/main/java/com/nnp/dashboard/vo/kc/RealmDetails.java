package com.nnp.dashboard.vo.kc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RealmDetails {
	private String id;
	private String realm;
	private int notBefore;
	private String defaultSignatureAlgorithm;
	private boolean revokeRefreshToken;
	private int refreshTokenMaxReuse;
	private int accessTokenLifespan;
	private int accessTokenLifespanForImplicitFlow;
	private int ssoSessionIdleTimeout;
	private int ssoSessionMaxLifespan;
	private int ssoSessionIdleTimeoutRememberMe;
	private int ssoSessionMaxLifespanRememberMe;
	private int offlineSessionIdleTimeout;
	private boolean offlineSessionMaxLifespanEnabled;
	private int offlineSessionMaxLifespan;
	private int clientSessionIdleTimeout;
	private int clientSessionMaxLifespan;
	private int clientOfflineSessionIdleTimeout;
	private int clientOfflineSessionMaxLifespan;
	private int accessCodeLifespan;
	private int accessCodeLifespanUserAction;
	private int accessCodeLifespanLogin;
	private int actionTokenGeneratedByAdminLifespan;
	private int actionTokenGeneratedByUserLifespan;
	private int oauth2DeviceCodeLifespan;
	private int oauth2DevicePollingInterval;
	private boolean enabled;
	private String sslRequired;
	private boolean registrationAllowed;
	private boolean registrationEmailAsUsername;
	private boolean rememberMe;
	private boolean verifyEmail;
	private boolean loginWithEmailAllowed;
	private boolean duplicateEmailsAllowed;
	private boolean resetPasswordAllowed;
	private boolean editUsernameAllowed;
	private boolean bruteForceProtected;
	private boolean permanentLockout;
	private int maxFailureWaitSeconds;
	private int minimumQuickLoginWaitSeconds;
	private int waitIncrementSeconds;
	private int quickLoginCheckMilliSeconds;
	private int maxDeltaTimeSeconds;
	private int failureFactor;
}
