package com.nnp.dashboard.event.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.nnp.dashboard.config.JMSConfig;
import com.nnp.dashboard.event.RestResponseEvent;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener that coordinates the asynchronous environment provisioning by
 * counting the {@link RestResponseEvent}s published by the GitLab / Redmine /
 * Keycloak integrations.
 *
 * Workflow:
 *   1. {@code EnvWrapperService.prepareLatch(3)} initialises a count-down latch
 *      for the three expected asynchronous confirmations.
 *   2. As each integration finishes it publishes a {@code RestResponseEvent};
 *      {@link #onWebClientResponse} records it (success or failure) and counts
 *      the latch down.
 *   3. {@code awaitResponsesAndProcess} blocks up to 120 seconds until the latch
 *      reaches zero. If all legs succeeded, it calls {@code doFinalActivity}
 *      which pushes the created {@code reqId} (+ user credential) on the JMS
 *      queue so the replication engine can trigger the GitOps deployment.
 *      If any leg failed or the timeout expires, a {@link DashboardConfigException}
 *      is raised so the caller can trigger a rollback.
 *
 * @author AC
 * @date 02-Jun-2025
 */
@Component
@Slf4j
public class LatchEventListener {

    @Autowired
    private JmsTemplate jmsTemplate;

    private CountDownLatch latch;
    private final List<String> responses = new ArrayList();
    private final List<String> failures = new ArrayList();


    public void prepareLatch(int count) {
        latch = new CountDownLatch(count);
        responses.clear();
        failures.clear();
    }

    @EventListener
    public void onWebClientResponse(RestResponseEvent event) {
        if (event.isSuccess()) {
//            log.info("Received event with response: {}", event.getResponse());
            responses.add(event.getResponse());
        } else {
//            log.error("Received FAILURE event from {}: {}", event.getSource(), event.getResponse());
            failures.add("[" + event.getSource() + "] " + event.getResponse());
        }
        latch.countDown();
    }

    public void awaitResponsesAndProcess(String reqId,String userId, String password,String email) throws InterruptedException {
        if (latch != null) {
            if(latch.await(120,TimeUnit.SECONDS)) {
                if (!failures.isEmpty()) {
//                    log.error("❌ One or more legs failed {}: {}",reqId, failures);
                    throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
                            "Environment creation failed: " + String.join("; ", failures)));
                }
//                log.info("✅ All events received. Processing final activity...{}", responses);
                doFinalActivity(reqId,userId,password,email);
            }else {
                throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
                        "Timed out waiting for gitlab/keycloak/redmine confirmation (received: "
                                + (responses.size() + failures.size()) + " of 3, failures so far: " + failures + ")"));
            }

        }
    }

    private void doFinalActivity(String reqId,String userId,String password,String email) {

//        log.info("Sending data to queue for env replication in engine to trigger");
        jmsTemplate.convertAndSend(JMSConfig.REQUEST_QUEUE, reqId+"|"+userId+"|"+password+"|"+email);
//        log.info("✅ Sent message to Q - "+reqId+"|"+userId+"|"+password);

    }
}