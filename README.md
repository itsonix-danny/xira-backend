# Xira Backend

A Jira-like issue tracker and project management system built with Spring Boot 3.5.5 and Java 25.

## Quick Start

```bash
# Start PostgreSQL database
docker-compose up -d

# Build and run tests
mvn clean install

# Run the application
mvn spring-boot:run
```

## Development

```bash
# Build without tests
mvn clean install -DskipTests

# Run specific test
mvn test -Dtest=AuthServiceTest

# Stop database
docker-compose down
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


