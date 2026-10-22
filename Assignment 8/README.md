
# Assignment 8 — Spring Cloud Gateway

**Course:** SFWE 510 — Cloud Native Software Engineering  
**Project:** *Spring Microservices in Action* (2nd Edition), Chapter 8  
**Repository:** [SFWE-510-Assignments](https://github.com/vjimenez6569/SFWE-510-Assignments)  
**Location:** `Assignment 8/`

## About

This assignment focuses on implementing an API Gateway using **Spring Cloud Gateway** to manage and route requests between microservices.

The application consists of three main services:

- **Gateway Server:** Acts as the entry point for incoming requests and forwards them to the appropriate services.
- **Licensing Service:** Manages licensing information.
- **Organization Service:** Manages organization information used by the Licensing Service.

The assignment demonstrates several Spring Cloud Gateway features:

- **Automatic Routing:** Uses Netflix Eureka to automatically discover and route requests to registered microservices.
- **Manual Routing:** Defines custom routes for the Licensing and Organization services.
- **Dynamic Route Reloading:** Updates Gateway routes without restarting the Gateway container.
- **Pre-filter:** Checks incoming requests for a correlation ID and generates one if it is missing.
- **Post-filter:** Adds the correlation ID to outgoing response headers.
- **External Routing:** Configures routes to Google and the University of Arizona website.

The application also uses **Spring Cloud Config Server** for centralized configuration, **Netflix Eureka** for service discovery, and **PostgreSQL** for data storage.

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

Run all commands from the `Assignment 8` directory.

### 1. Build the Application

Make sure Docker Desktop is running, then execute:

```powershell
mvn clean package dockerfile:build
```

This builds the Maven modules and Docker images.

### 2. Start Docker Containers

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Allow approximately 1–2 minutes for the services to start and register with Eureka.

### 3. Verify Containers

```powershell
docker compose -f docker/docker-compose.yml ps
```

### 4. Access the Services

| Service | URL |
|---|---|
| Config Server | http://localhost:8071 |
| Eureka Server | http://localhost:8073 |
| Gateway Server | http://localhost:8072 |
| Licensing Service | http://localhost:8080 |
| Organization Service | http://localhost:8081 |
| Gateway Routes | http://localhost:8072/actuator/gateway/routes |

### 5. Test Gateway Routing

Use Postman to send GET requests through the Gateway.

**Automatic Routing (Eureka Discovery)**

```http
GET http://localhost:8072/ORGANIZATION-SERVICE/v1/organization/{organizationId}
GET http://localhost:8072/LICENSING-SERVICE/v1/organization/{organizationId}/license/
```

**Manual Routing**

```http
GET http://localhost:8072/organization/v1/organization/{organizationId}
GET http://localhost:8072/license/v1/organization/{organizationId}/license/
```

Replace `{organizationId}` with an existing organization ID.

Both automatic and manual routes should forward requests to the corresponding microservices.

### 6. Test External Routes

The Gateway also includes manually configured routes to external websites.

**Google:**

```http
GET http://localhost:8072/google/
```

**University of Arizona:**

```http
GET http://localhost:8072/arizona/
```

These requests are forwarded to their configured external destinations.

### 7. Test Dynamic Route Reloading

After modifying the Gateway routes in `configserver/src/main/resources/config/gateway-server.yml`, refresh the configuration using Postman:

```http
POST http://localhost:8072/actuator/refresh
```

Then refresh the Gateway routes:

```http
POST http://localhost:8072/actuator/gateway/refresh
```

Verify the updated routes:

```http
GET http://localhost:8072/actuator/gateway/routes
```

This allows Gateway routes to be updated without restarting the Gateway container, provided Config Server is reading the updated configuration.

### 8. Test Pre-filter and Post-filter

Send a GET request through the Gateway with the following header:

```http
tmx-correlation-id: assignment8-test-123
```

The pre-filter checks for the correlation ID and generates one if it is missing.

The post-filter adds the correlation ID to the HTTP response headers.

To view the Gateway logs:

```powershell
docker compose -f docker/docker-compose.yml logs -f gatewayserver
```

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

Changes to the Gateway routing configuration can be dynamically refreshed without rebuilding the application when the Config Server is configured to read the updated files.
