# 🏗️ GitHub Analyzer Architecture Guidelines

This document outlines the architectural principles and project structure for the `github-analyzer` project.

## 🌟 Core Principles

### 1. Domain-Driven Segregation
Interest separation is achieved by grouping code into **domains** rather than technical layers at the top level. Each domain package is a self-contained unit of business logic.

*   **Path**: `com.backend.githubanalyzer.domain.{domain_name}`
*   **Examples**: `user`, `repository`, `analysis`

### 2. Internal MC (Model-Controller) Pattern
Within each domain, we implement the **MC** part of MVC (as the View is handled by the frontend).

- **Controller (Interface Layer)**:
    - Responsible for handling HTTP requests and returning standardized responses.
    - Minimal logic; primary role is to delegate to the Service layer.
- **Model (Business & Data Layer)**:
    - **Service**: Orchestrates business logic and domain rules.
    - **Entity**: JPA-mapped models representing the persistent state.
    - **Repository**: Data access abstractions (Spring Data JPA).
    - **DTO**: Structured objects for data transfer between layers (Request/Response).

### 3. Separation of Concerns (SoC)
Cross-cutting concerns are strictly separated from domain logic:

| Layer | Responsibility |
| :--- | :--- |
| **Security** | Authentication (JWT/OAuth2), authorization, and password encryption. |
| **Infra** | External system integrations (Redis, Database configuration, API clients). |
| **Domain** | Pure business rules and domain state transitions. |

### 4. Mandatory Transactional Management
To ensure data atomicity across multiple domain updates, all service methods performing write operations must use `@Transactional`.
*   **Rule**: Always apply `@Transactional` to service-layer methods that modify state.
*   **Goal**: Ensure data consistency across `User`, `GithubRepository`, `Contribution`, and `Owns` tables.

## 📁 Package Structure

```text
src/main/java/com/backend/githubanalyzer/
├── domain/                # Business Domains
│   └── user/              # User Domain
│       ├── controller/    # API Endpoints
│       ├── service/       # Business Logic
│       ├── entity/        # Database Models
│       ├── repository/    # Persistence Interfaces
│       └── dto/           # Request/Response Objects
├── security/              # Auth & Security Core
├── config/                # General Spring Configurations
└── infra/                 # Infrastructure & External Integrations
```

---
*Last Updated: 2026-01-17*
