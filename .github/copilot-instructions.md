# Project instructions

You are working on a production-style modular-monolith Distributed URL Shortener.

Use:
- Java 21
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Data JPA
- Jakarta Bean Validation
- Spring Data Redis
- PostgreSQL
- Flyway
- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers where practical
- Docker and Docker Compose
- Springdoc OpenAPI
- Spring Boot Actuator

Do not use MongoDB, Kafka, RabbitMQ, Kubernetes, GraphQL, microservices, authentication systems, frontend frameworks, or cloud-specific SDKs.

General rules:
- Actually edit and create files in the current workspace.
- Do not merely provide snippets or instructions.
- Use constructor injection only.
- Keep controllers thin.
- Put business logic in services.
- Do not expose JPA entities directly.
- Use DTOs for API requests and responses.
- Use UTC timestamps.
- Use Flyway migrations instead of Hibernate schema generation.
- Do not commit secrets.
- Do not use Java native serialization in Redis.
- Do not use random strings or UUIDs for visible short codes.
- Never claim a build, test, Docker command, or endpoint verification succeeded unless it was actually executed successfully.
- If a dependency or external service cannot be executed, document it as unverified.
- Keep the implementation simple, readable, and production-oriented.
- Avoid unnecessary abstractions and dependencies.