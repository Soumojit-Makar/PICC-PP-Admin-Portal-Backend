package com.nnp.dashboard.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;

import com.nnp.dashboard.utils.Utils;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Central Spring {@link Configuration} of integration components.
 *
 * Provides:
 *   - three reactive {@link WebClient} beans for the GitLab, Redmine and Mail
 *     integrations, each with its own isolated named connection pool so that
 *     bursts of calls to one integration can never starve the others
 *   - request-logging exchange filter
 *   - an explicit {@link CacheManager} with the cache regions used at runtime
 *     ({@code user-cache} and {@code acc-pwd-cache})
 *
 * Base URLs are supplied from the config-server via {@code @Value} properties
 * ({@code gitlab.service.baseurl}, {@code redmine.service.baseurl},
 * {@code mail.service.baseurl}).
 */
@Configuration
@Slf4j
public class DashboardConfiguratorConfig {

    @Value("${gitlab.service.baseurl:http://localhost:8089}")
    private String gitlabBaseUrl;

    @Value("${redmine.service.baseurl:http://localhost:3000}")
    private String redmineBaseUrl;

    @Value("${mail.service.baseurl:http://localhost:8085}")
    private String mailServiceBaseUrl;

    @Value("${k8s.api.url:https://kubernetes.default.svc}")
    private String k8sApiUrl;

    @Bean(name = "webClientGitInt")
    public WebClient getGitlabWebClient() {
        return WebClient.builder()
                .baseUrl(gitlabBaseUrl)
                .filter(logReq())
                //.filter(errorHandling())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(configWebClient("gitlab-pool")))
                .build();
    }

    @Bean(name = "webClientRedmineInt")
    public WebClient getRedmineWebClient() {
        return WebClient.builder()
                .baseUrl(redmineBaseUrl)
                .filter(logReq())
                //.filter(errorHandling())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(configWebClient("redmine-pool")))
                .build();
    }

    @Bean(name = "webClientMailService")
    public WebClient getMailServiceWebClient() {
        return WebClient.builder()
                .baseUrl(mailServiceBaseUrl)
                .filter(logReq())
                //.filter(errorHandling())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(configWebClient("mail-pool")))
                .build();
    }

    @Bean(name = "webClientK8sApi")
    public WebClient getK8sApiWebClient() {
        return WebClient.builder()
                .baseUrl(k8sApiUrl)
                .filter(logReq())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(configWebClient("k8s-pool")))
                .build();
    }

    /**
     * Builds an HttpClient with its own named, bounded connection pool.
     * Previously all three WebClients called HttpClient.create() with no
     * explicit ConnectionProvider, which meant they all shared Reactor
     * Netty's single global "default" pool (45s pendingAcquireTimeout).
     * Under concurrent load (e.g. rollback bursts hitting GitLab, Redmine
     * and Mail back-to-back) that shared pool could stall waiting for a
     * connection, and by the time the 20s ReadTimeoutHandler fired, the
     * *actual* downstream call (e.g. Redmine's createUser) had often
     * already succeeded — leaving orphaned records behind after rollback.
     * Isolating each integration into its own pool prevents contention
     * between services and keeps failures attributable to the right cause.
     */
    private HttpClient configWebClient(String poolName) {
        ConnectionProvider provider = ConnectionProvider.builder(poolName)
                .maxConnections(50)
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(60))
                .build();

        return HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 200000)
                .responseTimeout(Duration.ofMillis(200000))
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(200000, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(200000, TimeUnit.MILLISECONDS)));
    }

    private ExchangeFilterFunction logReq() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
//            log.info("Logging request URL {}", clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    /**
     * Explicit CacheManager with named cache regions.
     *
     * "user-cache"    — existing: user access/permission cache
     * "acc-pwd-cache" — new: temporary password store for env provisioning
     *                   (one-time use — evicted immediately after retrieval)
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("user-cache", "acc-pwd-cache");
    }

    /**
     * Registers the Hibernate6Module with Spring's Jackson ObjectMapper.
     *
     * Without this, Jackson encounters Hibernate's ByteBuddy lazy-proxy
     * objects on uninitialized LAZY associations and throws:
     *   "Type definition error: [simple type, class
     *    org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]"
     *
     * With this bean registered, Jackson treats any unloaded proxy as null
     * rather than attempting to introspect the ByteBuddy class itself.
     */
    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Do NOT force-load lazy associations during serialization;
        // uninitialized proxies serialize as null.
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        // Suppress serializing the identifier of a lazy proxy that hasn't
        // been initialized — avoids partial objects leaking into responses.
        module.disable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        return module;
    }

}