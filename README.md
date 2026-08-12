# FinAdvise CRM Backend API

**Author:** Hana Zahálková
**Status:** Academic MVP (V1)

This repository is a university project strictly scoped as a Minimum Viable Product. 

## 🚀 The Mission
Transitioning from legacy Delphi systems to Modern Enterprise Java, I built this project to demonstrate architectural proficiency. Drawing on my past professional experience as a financial advisor, FinAdvise CRM accurately models real-world wealth management domains.

## 🛠 Tech Stack
* **Language**: Java 21
* **Framework**: Spring Boot 4.0.3 (Web, Data JPA, Security, Validation)
* **Security**: Spring Security with OAuth2 Resource Server and Nimbus JOSE (JWT)
* **Data**: Oracle Database 23ai Free
* **Utilities**: Lombok, Hashids, and `.env` configuration mapping
* **Infrastructure**: Docker Compose for database hosting and initialization

## 🏗 Architectural Highlights
* **Modular-Monolith**: Due to the tight scope of the project, Modular Monolithic architecture was chosen for backend. It prevents the overhead and boilerplate of fully modular system while preserving basic modularity requirements like decoupling and statelessness.
* **Zero-Trust Security**: Adopts a default-deny posture. The backend exclusively handles HTTP traffic, relying on a planned Nginx reverse proxy for TLS termination (HTTPS). Identity is verified via stateless JWTs, with method-level `@PreAuthorize` enforcing strict Role-Based Access Control and object-level ownership[cite: 11].
* **Package-by-Feature Encapsulation**: Domain modules (`users`, `clients`, `budget`, `products`) are strictly isolated. Repositories and services are package-private, preventing cyclic dependencies. Cross-module communication occurs exclusively through interface Facades.
* **Defensive IDOR Prevention**: Internal database sequences for critical entities (`User`, `Client`) are obfuscated using Hashids, yielding secure alphanumeric strings (e.g., `clientUid`). As an accepted MVP limitation to avoid unnecessary encryption overhead, lesser supporting entities expose standard `Long` primary keys.
* **Concurrency & Isolation**: Implements Optimistic Locking via JPA `@Version` to prevent "lost updates", returning structured RFC 7807 `409 Conflict` responses. Complex operations, such as Address deduplication, utilize `REQUIRES_NEW` transaction propagation to catch unique constraint violations without rolling back parent transactions.
* **Database-Level Integrity**: Beyond Spring Validation (`@Valid`), absolute data integrity is guaranteed by `BEFORE INSERT OR UPDATE` triggers directly in the Oracle database. This enforces complex temporal and relational business logic (e.g., expired ID cards, inactive advisors) at the lowest possible layer.

## ![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=flat-square&logo=swagger&logoColor=white) Interactive API Documentation
This project includes a fully integrated Swagger UI for real-time API exploration.

#### 1. Access the UI: 
* Navigate to `http://localhost:8080/swagger-ui.html` while the app is running.

#### 2. Authorize: 
* The system automatically seeds a default administrator on the first run. 
* Use the `POST /api/v1/auth/login` endpoint with the credentials defined in your `.env` file (Employee ID and Password) to receive a JWT.
* Click "Authorize" at the top of the Swagger page and paste your token to unlock protected routes.

## 🚦 Getting Started
1. Clone the Repository.
2. Copy `.env.example` to `.env` and fill in the required database and Hashid secrets.
3. Run `docker compose up -d` to launch and initialize the Oracle database.
4. Start the Spring Boot application (the Docker Compose Lifecycle Manager will automatically detect the running database and execute the `schema.sql` and package seeders).

---
💡 **Why this project?**
I am leaving legacy systems behind to embrace type-safe, properly decoupled, and highly tested enterprise architectures.
