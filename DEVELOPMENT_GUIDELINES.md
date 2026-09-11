# Development Guidelines and Contribution Standards: `PICC-PP-Admin-Portal-Backend`

This document defines the architectural standards, development workflows, coding conventions, and security requirements for contributors to **`PICC-PP-Admin-Portal-Backend`** (`env-dashboard-configurator`).

---

## Table of Contents

1. [Architecture & Design Principles](#1-architecture--design-principles)
2. [Development Environment Setup](#2-development-environment-setup)
3. [Package Structure & Code Navigation](#3-package-structure--code-navigation)
4. [Coding Standards & Best Practices](#4-coding-standards--best-practices)
   - [Zero Hardcoded Values Principle](#zero-hardcoded-values-principle)
   - [REST Controller Standards](#rest-controller-standards)
   - [Keycloak Identity & Access Management](#keycloak-identity--access-management)
   - [ActiveMQ Artemis Asynchronous Messaging](#activemq-artemis-asynchronous-messaging)
   - [HAProxy Integration Gateway](#haproxy-integration-gateway)
   - [Exception Handling & Problem Details](#exception-handling--problem-details)
   - [Logging & Sensitive Data Sanitization](#logging--sensitive-data-sanitization)
5. [Security, Code Quality & Compliance Tooling](#5-security-code-quality--compliance-tooling)
   - [SAST: SpotBugs & FindSecBugs](#sast-spotbugs--findsecbugs)
   - [SCA: OWASP Dependency-Check](#sca-owasp-dependency-check)
   - [SBOM: CycloneDX Aggregate Generation](#sbom-cyclonedx-aggregate-generation)
   - [Checkstyle: Google Java Style](#checkstyle-google-java-style)
6. [Git Workflow & Branching Strategy](#6-git-workflow--branching-strategy)
   - [Branch Naming Conventions](#branch-naming-conventions)
   - [Conventional Commits](#conventional-commits)
7. [Pull Request (PR) Checklist](#7-pull-request-pr-checklist)
8. [Release Lifecycle & Versioning](#8-release-lifecycle--versioning)

---

## 1. Architecture & Design Principles

`PICC-PP-Admin-Portal-Backend` serves as the central orchestration and management backend for the **Nubo Native Platform (NNP)** administration portal. It follows these core architectural principles:

1. **Decoupled Asynchronous Workflows**: Heavy environment provisioning tasks and multi-step tenant on-boarding are offloaded to **ActiveMQ Artemis JMS** message queues (`env-rep::env_replication`), ensuring sub-second response times for interactive portal operations.
2. **Zero Hardcoded Configuration**: All IP addresses, DNS names, credentials, port bindings, and external service URLs must be externalized to environment variables and injected via Spring `@Value("${...:default}")` with resilient, non-crashing defaults.
3. **Reactive External Integrations**: Outbound communication with ecosystem microservices (GitLab, Redmine, Mail, Keycloak, HAProxy DataPlane) uses Spring WebClient with dedicated connection pools, request logging, and configurable timeouts.
4. **Defense in Depth & Log Sanitization**: All user-supplied inputs echoed to logs must pass through `Utils.sanitizeForLog()` to prevent CRLF log injection attacks. Sensitive tokens, passwords, and API keys are strictly masked.
5. **Standardized API Contracts**: Every REST endpoint exposes clean, schema-validated request/response DTOs documented with OpenAPI 3.0 annotations.

---

## 2. Development Environment Setup

### Required Tools
- **JDK 21** (Eclipse Temurin 21 or OpenJDK 21 LTS).
- **Maven 3.9+** (or use `./mvnw`).
- **Docker & Docker Compose** (for local PostgreSQL and ActiveMQ Artemis instances).
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Lombok and Spring Tools plugins enabled.

### Local Initialization
```bash
# Clone the repository
git clone https://github.com/Nubo-Native-Platform/PICC-PP-Admin-Portal-Backend.git
cd PICC-PP-Admin-Portal-Backend

# Copy environment configuration template
cp .env.example .env

# Start backing services (PostgreSQL & ActiveMQ Artemis)
docker compose up -d postgres artemis

# Compile and run test suite
./mvnw clean test
```

---

## 3. Package Structure & Code Navigation

```
src/main/java/com/nnp/dashboard/
├── EnvDashboardConfiguratorApplication.java  # Main Spring Boot entrypoint & CORS config
├── client/                                  # HTTP & Feign client integrations (Keycloak, HAProxy)
├── config/                                  # Spring configurations (JMS, WebClient, OpenAPI, Cache)
├── controller/                              # REST API controllers
│   ├── BillingAdminController.java
│   ├── CHElementDetailController.java
│   ├── ElementDetailController.java
│   ├── EnvironmentController.java
│   ├── HAProxyDashboardController.java
│   ├── NNPAccountController.java
│   └── UserAccessController.java
├── exception/                               # Global exception handler & error response models
├── model/                                   # JPA entities & database mappings
├── repo/                                    # Spring Data JPA repositories
├── service/                                 # Domain services and business logic orchestration
├── utils/                                   # Common security and formatting utilities
└── vo/                                      # Value objects, request/response DTOs, and view models
```

---

## 4. Coding Standards & Best Practices

### Zero Hardcoded Values Principle
* **NEVER** hardcode IP addresses (e.g., `10.x.x.x`), internal domains (e.g., `.nnp.nubons.com`), credentials, or ports.
* Always supply a fallback default in `@Value` annotations:
  ```java
  // CORRECT:
  @Value("${haproxy.base.domain:nnp.example.com}")
  private String haproxyBaseDomain;

  // INCORRECT:
  String domain = "my-service.nnp.nubons.com";
  ```

### REST Controller Standards
* Controllers should focus strictly on request validation, HTTP status mapping, and delegating to services.
* Use `@ResponseStatus` or `ResponseEntity<T>` explicitly.
* Ensure all endpoints are tagged appropriately for OpenAPI documentation.

### Logging & Sensitive Data Sanitization
* All logged parameters that originate from requests must be sanitized:
  ```java
  log.info("Processing request for component: {}", Utils.sanitizeForLog(compName));
  ```
* Never log user passwords, bearer tokens, or Redmine/GitLab API keys.

---

## 5. Security, Code Quality & Compliance Tooling

Contributors must verify their code against our automated DevSecOps scanning stack:

| Tool | Maven Command | Purpose | Threshold |
| :--- | :--- | :--- | :--- |
| **SpotBugs + FindSecBugs** | `./mvnw spotbugs:check` | Static Application Security Testing (SAST) | Zero high-severity vulnerabilities |
| **OWASP Dependency-Check** | `./mvnw dependency-check:check` | Software Composition Analysis (SCA) | CVSS >= 7 fails build |
| **CycloneDX SBOM** | `./mvnw cyclonedx:makeAggregateBom` | Immutable Software Bill of Materials (SBOM) | Generated during `package` phase |
| **Checkstyle** | `./mvnw checkstyle:check` | Code style enforcement (Google Java Style) | Informational |

---

## 6. Git Workflow & Branching Strategy

- **`main`**: Production-ready code. Directly protected.
- **`develop`**: Integration branch for tested contributions.
- **`feature/<issue-id>-<short-description>`**: New features and architectural updates.
- **`fix/<issue-id>-<short-description>`**: Bug fixes.

### Conventional Commits
All commits must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
* `feat: add dynamic domain suffix resolution for HAProxy routes`
* `fix: remove hardcoded config server IP from application-dev.properties`
* `docs: add comprehensive development guidelines and API documentation`
* `refactor: externalize ActiveMQ Artemis credentials with safe defaults`

---

## 7. Pull Request (PR) Checklist

Before submitting a PR, verify:
- [ ] Code compiles cleanly with `./mvnw compile`.
- [ ] All unit tests pass with `./mvnw clean test`.
- [ ] SpotBugs check passes with `./mvnw spotbugs:check`.
- [ ] No hardcoded passwords, IP addresses, or internal domains exist.
- [ ] New features include corresponding unit tests.
- [ ] OpenAPI annotations and documentation are updated if endpoints changed.

---

## 8. Release Lifecycle & Versioning

Releases follow [Semantic Versioning 2.0.0](https://semver.org/):
`MAJOR.MINOR.PATCH` (e.g., `0.0.1-SNAPSHOT` -> `1.0.0`).
Build artifacts are published automatically to **GitHub Packages** on tag release.
