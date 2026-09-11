package com.nnp.dashboard.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * OpenAPI 3.0 / Swagger UI Configuration for PICC-PP-Admin-Portal-Backend.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PICC-PP-Admin-Portal-Backend REST API")
                        .version("0.0.1-SNAPSHOT")
                        .description("""
                                Enterprise Admin Portal Backend and Environment Dashboard Configurator microservice \
                                for the Nubo Native Platform (NNP).
                                
                                ### Key Capabilities:
                                * **Environment Management**: Lifecycle operations, environment provisioning, plan specifications, and component templating.
                                * **User Access & RBAC**: Automated identity and access management orchestrating Keycloak realms, groups, and role assignments.
                                * **HAProxy Routing Integration**: Declarative routing rule management, backend synchronisation, and DataPlane integration.
                                * **Billing & Subscriptions**: Account metering, token usage tracking, and invoice line calculation.
                                * **Ecosystem Integration**: Asynchronous workflow integration with GitLab, Redmine issue tracking, and notification mail services.
                                """)
                        .contact(new Contact()
                                .name("Nubo Native Platform Team")
                                .email("contribution@nubons.com")
                                .url("https://github.com/Nubo-Native-Platform/PICC-PP-Admin-Portal-Backend"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Default Server / Current Host"),
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server")
                ))
                .tags(List.of(
                        new Tag().name("Environment Management").description("Endpoints for provisioning, configuring, and inspecting platform environments"),
                        new Tag().name("User Access & RBAC").description("Endpoints for user onboarding, Keycloak identity management, roles, and credentials"),
                        new Tag().name("HAProxy Proxy Configs").description("Endpoints for inspecting and managing dynamic HAProxy routing rules"),
                        new Tag().name("Billing & Subscriptions").description("Endpoints for managing tenant accounts, subscription plans, and billing lines"),
                        new Tag().name("Diagnostics & Health").description("Service health, metrics, and operational readiness endpoints")
                ));
    }
}
