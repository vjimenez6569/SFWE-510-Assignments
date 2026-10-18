
# Assignment 7 — Building Resilient Microservices

**Course:** SFWE 510 — Cloud Native Software Engineering  
**Project:** *Spring Microservices in Action* (2nd Edition), Chapter 7  
**Repository:** [SFWE-510-Assignments](https://github.com/vjimenez6569/SFWE-510-Assignments)  
**Location:** `Assignment 7/`

## About

This assignment focuses on building resilient microservices using **Resilience4j** with Spring Boot and Spring Cloud.

The application consists of two main microservices:

- **Licensing Service:** Manages licensing information and demonstrates resilience patterns.
- **Organization Service:** Manages organization information used by the Licensing Service.

The Licensing Service implements several resilience patterns:

- **Circuit Breaker:** Helps prevent repeated calls when a service experiences failures.
- **Retry:** Automatically retries failed operations.
- **Rate Limiter:** Limits how frequently requests can be made.
- **Bulkhead:** Controls the number of concurrent requests.
- **Fallback:** Returns an alternative response when an operation fails.

A custom fallback method, `customFallbackLicenseList()`, was also implemented to provide an alternative response and log information about failures.

The application uses **Spring Cloud Config Server** for centralized configuration, **Netflix Eureka** for service discovery, and **PostgreSQL** for data storage.

## Prerequisites

Make sure the following are installed:

- Java 11
- Apache Maven
- Docker Desktop
- Git
- Postman (for API testing)

### Verify Java and Maven

Open PowerShell and run:

```powershell
java -version
mvn -version
```

Both should indicate Java 11.

If Maven is using a different Java version, update the environment variables:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11.0.32"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Run the Application

Run all commands from the `Assignment 7` directory.

### 1. Build the Application

```powershell
mvn clean package dockerfile:build
```

This builds the Maven modules and Docker images.

### 2. Start Docker Containers

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Allow approximately 1–2 minutes for the services to start.

### 3. Verify Containers

```powershell
docker compose -f docker/docker-compose.yml ps
```

### 4. Access the Services

| Service | URL |
|---|---|
| Config Server | http://localhost:8071 |
| Eureka Server 1 | http://localhost:8072 |
| Eureka Server 2 | http://localhost:8073 |
| Eureka Server 3 | http://localhost:8074 |
| Licensing Service | http://localhost:8080 |
| Organization Service | http://localhost:8081 |

### 5. Test the Licensing Service

Use Postman or a browser to send a GET request:

```http
GET http://localhost:8080/v1/organization/e839ee96-28de-4f67-bb79-870ca89743a0/license/
```

The endpoint returns licensing information for the organization.

Some requests may trigger a simulated failure and return the custom fallback response instead.

## Stop the Application

To stop and remove the containers:

```powershell
docker compose -f docker/docker-compose.yml down
```

To start them again:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

## Rebuild After Changes

After modifying Java source code, rebuild the application:

```powershell
mvn clean package dockerfile:build
```

Then recreate the containers using the updated images:

```powershell
docker compose -f docker/docker-compose.yml up -d --force-recreate
```
