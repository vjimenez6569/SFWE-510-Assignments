# Assignment 6 — Spring Cloud Config Server and Eureka Service Discovery

**Course:** SFWE 510 — Cloud Native Software Engineering  
**Project:** *Spring Microservices in Action* (2nd edition), Chapter 6  
**Repository:** [SFWE-510-Assignments](https://github.com/vjimenez6569/SFWE-510-Assignments)  
**Location in repository:** `Assignment 6/`

## Overview

This assignment demonstrates centralized application configuration using **Spring Cloud Config Server** with a **GitHub-backed configuration repository**, and service registration and discovery using **Netflix Eureka**.

The application contains two business microservices—**Licensing Service** and **Organization Service**—that register with Eureka. The original standalone Eureka setup was extended into a **three-instance Eureka peer cluster** to demonstrate replication of service registry information.

## Technologies

- Java 11
- Spring Boot and Spring Cloud
- Spring Cloud Config Server (Git backend)
- Netflix Eureka Server and Client
- Maven
- Docker and Docker Compose
- PostgreSQL
- Git and GitHub

## Project structure

```text
Assignment 6/
├── configserver/
│   └── src/main/resources/
│       ├── bootstrap.yml
│       └── config/
│           ├── eureka-server.yml
│           ├── licensing-service.properties
│           ├── licensing-service-dev.properties
│           ├── licensing-service-prod.properties
│           ├── organization-service.properties
│           └── organization-service-dev.properties
├── eurekaserver/
├── licensing-service/
├── organization-service/
├── docker/
│   ├── docker-compose.yml
│   ├── init.sql
│   └── data.sql
├── pom.xml
└── README.md
```

## Prerequisites

Install and start the following before running the application:

1. **JDK 11** (not a newer JDK for this project's Maven build).
2. **Apache Maven**.
3. **Docker Desktop**, with its Docker engine running.
4. **Git**, with the repository cloned locally.
5. An internet connection for Maven dependencies, Docker images, and access to the GitHub configuration repository.

### Verify Java and Maven

In **PowerShell**:

```powershell
java -version
mvn -version
```

Both should indicate Java 11. If PowerShell is using a different Java version, and JDK 11 is installed at the path used during development, run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11.0.32"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
mvn -version
```

These environment-variable changes apply only to the current PowerShell session. Adjust the path if your JDK 11 installation is elsewhere.

## Build and run

Run the following commands from the **`Assignment 6` directory**, not from the repository root.

### 1. Build the application and Docker images

```powershell
mvn clean package dockerfile:build
```

This builds the Maven modules, runs their tests, packages the applications, and builds the Docker images defined by the project.

If the Maven build fails with `InaccessibleObjectException` in Eureka/XStream, check `mvn -version` and ensure Maven is using **Java 11**.

### 2. Start the containers

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Docker Compose starts seven services:

| Compose service | Purpose | Host port |
|---|---|---|
| `database` | PostgreSQL | `5432` |
| `configserver` | Git-backed centralized configuration | `8071` |
| `eurekaserver` | Eureka peer 1 | `8072` |
| `eurekaserver2` | Eureka peer 2 | `8073` |
| `eurekaserver3` | Eureka peer 3 | `8074` |
| `licensingservice` | Licensing microservice | `8080` |
| `organizationservice` | Organization microservice | `8081` |

The first Eureka instance uses host port `8072` because port `8070` was occupied on the development machine. All Eureka instances use port `8070` **inside Docker**.

### 3. Check container status

```powershell
docker compose -f docker/docker-compose.yml ps
```

The containers should be running. Allow approximately one or two minutes for startup, registration, and Eureka peer synchronization.

### 4. Verify Config Server

Open these URLs in a browser:

- [Eureka configuration](http://localhost:8071/eureka-server/default)
- [Licensing Service configuration](http://localhost:8071/licensing-service/default)
- [Organization Service configuration](http://localhost:8071/organization-service/default)

The JSON response should contain a nonempty `propertySources` array referencing the Git-backed configuration. The `label` should match the configured Git branch (for example, `main`).

### 5. Verify Eureka service discovery

Open the Eureka dashboards:

- [Eureka instance 1](http://localhost:8072)
- [Eureka instance 2](http://localhost:8073)
- [Eureka instance 3](http://localhost:8074)

On a healthy, synchronized cluster, each dashboard should show:

```text
DS Replicas: the other two Eureka server hostnames

Instances currently registered with Eureka:
EUREKA-SERVER         UP (3)
LICENSING-SERVICE     UP (1)
ORGANIZATION-SERVICE  UP (1)
```

For example, the first Eureka dashboard should list `eurekaserver2` and `eurekaserver3` under **DS Replicas**.

## Restarting and stopping

Restart/recreate the services after changing Compose settings or after pushing updated Git-backed configuration:

```powershell
docker compose -f docker/docker-compose.yml down
docker compose -f docker/docker-compose.yml up -d
```

Stop the containers:

```powershell
docker compose -f docker/docker-compose.yml down
```

Avoid `down -v` unless you intentionally want to remove Docker-managed volumes and their data.
