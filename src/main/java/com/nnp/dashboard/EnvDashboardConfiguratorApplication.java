package com.nnp.dashboard;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * Entry point of the Dashboard Configurator (backend) application.
 *
 * This Spring Boot micro-service is the core provisioning backend of the NNP
 * (Nubons Platform) portal. It exposes REST endpoints to:
 *   1. Configure "environments" (an isolated NNP workspace) together with its
 *      features, feature-elements, element-details and child (CH) element details.
 *   2. Manage user access against those environments, roles and permissions.
 *   3. Register accounts/tenants and orchestrate the full provisioning pipeline
 *      across the external systems (Keycloak, GitLab, Redmine, Mail and Queue).
 *
 * Annotations:
 *  - {@code @EnableAsync}      - allows background methods such as environment
 *                                provisioning to run on a separate thread pool.
 *  - {@code @EnableCaching}    - enables Spring Cache abstraction backed by the
 *                                {@link com.nnp.dashboard.config.DashboardConfiguratorConfig}
 *                                ConcurrentMapCacheManager.
 *  - {@code @EnableScheduling} - activates Spring's scheduled task execution
 *                                infrastructure so {@code @Scheduled} cron jobs
 *                                (billing schedulers) fire as configured.
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
// JMS is wired explicitly via JMSConfig, so @EnableJms is not required here.
//@EnableJms
@Slf4j
public class EnvDashboardConfiguratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnvDashboardConfiguratorApplication.class, args);
	}
	/**
	 * Global CORS configuration.
	 * Permits cross-origin requests (any origin / any HTTP method) so the
	 * front-end portal can call this backend from a different host/port.
	 * Mappings cover both the standalone contexts ({@code /env}, {@code /user})
	 * and the gateway-nested contexts ({@code /nnpconf/...}).
	 */
    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(@NonNull CorsRegistry registry) {
				registry.addMapping("/env/**").allowedOrigins(allowedOrigins).allowedMethods("*"); //works as context not required
				registry.addMapping("/user/**").allowedOrigins(allowedOrigins).allowedMethods("*"); //works as context not required
				registry.addMapping("/nnpconf/env/**").allowedOrigins(allowedOrigins).allowedMethods("*");
				registry.addMapping("/nnpconf/user/**").allowedOrigins(allowedOrigins).allowedMethods("*");

			}
		};
	}
}
