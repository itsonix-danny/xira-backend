# Xira Backend

A Jira-like issue tracker and project management system built with Spring Boot 3.5.5 and Java 25.

## Prerequisites

- Java 25
- Maven 3.9+
- Docker

## First Time Setup

```bash
# 1. Copy `.env.example` to `.env` and adjust the values if needed:
cp .env.example .env
  
# 2. Start the PostgreSQL database:
docker compose up -d

# 3. Build the project and run all tests:
mvn clean install
```

## Running the Application

```bash
docker compose up -d
mvn spring-boot:run
```

## Development

```bash
# Build without tests
mvn clean install -DskipTests

# Run unit tests only
mvn test

# Run all tests including integration tests
mvn verify

# Run a specific test
mvn test -Dtest=AuthServiceTest

# Stop database
docker compose down
```

## Tech Stack

- **Framework**: Spring Boot 3.5.5 (Java 25)
- **Database**: PostgreSQL
- **Security**: JWT with Spring Security
- **API**: OpenAPI/Swagger specification
- **Testing**: JUnit 5, Mockito, TestContainers, REST Assured

## Architecture

Layered architecture with:

- **Controllers** (`web`): REST endpoints from OpenAPI spec
- **Services** (`service`): Business logic
- **Data Layer** (`jpa`): JPA entities and Spring Data repositories


