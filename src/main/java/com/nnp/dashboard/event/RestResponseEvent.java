/**
 * RestResponseEvent.java
 *
 * @author AC
 * @date 02-Jun-2025
 */
package com.nnp.dashboard.event;

/**
 * RestResponseEvent.java
 *
 * Simple application event carrying the outcome of an asynchronous external
 * integration call (GitLab / Redmine / Keycloak).
 *
 * It is published by those integration services and consumed by the
 * {@link com.nnp.dashboard.event.listener.LatchEventListener}, which counts the
 * expected number of successful responses before the environment-creation flow
 * is allowed to proceed (see {@code EnvWrapperService}).
 *
 * @author AC
 * @date 02-Jun-2025
 */
public class RestResponseEvent {
    private final String response;
    private final boolean success;
    private final String source;
    public RestResponseEvent(String response) {
        this(response, true, "unknown");
    }
    public RestResponseEvent(String response, boolean success, String source) {
        this.response = response;
        this.success = success;
        this.source = source;
    }
    public String getResponse() {
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSource() {
        return source;
    }
}
