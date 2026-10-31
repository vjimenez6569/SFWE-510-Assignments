
# Assignment 9 — Securing Your Microservices

**Course:** SFWE 510 — Cloud Native Software Engineering  
**Project:** *Spring Microservices in Action* (2nd Edition), Chapter 9  
**Repository:** [SFWE-510-Assignments](https://github.com/vjimenez6569/SFWE-510-Assignments)  
**Location:** `Assignment 9/`

## About

This assignment focuses on securing microservices using **Keycloak**, **Spring Security**, and **OAuth 2.0** to implement authentication and role-based authorization.

The application consists of several main services:

- **Keycloak:** Manages user authentication, access tokens, clients, and roles.
- **Gateway Server:** Routes incoming API requests to the appropriate microservices.
- **Licensing Service:** Manages licensing information and uses Keycloak for security.
- **Organization Service:** Manages organization information and restricts access based on user roles.

The assignment demonstrates several security features:

- **Authentication:** Requires users to authenticate through Keycloak before accessing protected endpoints.
- **Authorization:** Restricts access to certain operations based on assigned user roles.
- **OAuth 2.0:** Uses access tokens to authenticate requests to microservices.
- **Role-Based Access Control (RBAC):** Uses `ADMIN` and `USER` roles to control access to organization endpoints.
- **Protected Endpoints:** Allows authenticated users to retrieve organization information while restricting DELETE operations to administrators.

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

Run all commands from the `Assignment 9` directory unless otherwise specified.

### 1. Build the Application

Make sure Docker Desktop is running, then execute:

```powershell
mvn clean package dockerfile:build
```

This builds the Maven modules and Docker images.

If rebuilding the Config Server separately, run:

```powershell
cd configserver
mvn clean package -DskipTests
docker build --build-arg JAR_FILE=target/*.jar -t ostock/configserver:0.0.1-SNAPSHOT .
cd ..
```

### 2. Configure the Hosts File

Open Notepad as Administrator and edit:

```text
C:\Windows\System32\drivers\etc\hosts
```

Add the following entry:

```text
127.0.0.1 keycloak
```

This allows Keycloak to be accessed using the hostname `keycloak`.

### 3. Start Docker Containers

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Allow approximately 1–2 minutes for the services to start.

The application uses the legacy Keycloak image:

```text
quay.io/keycloak/keycloak:16.1.1
```

This version supports the Keycloak configuration and `/auth` endpoints used in Chapter 9.

### 4. Verify Containers

```powershell
docker compose -f docker/docker-compose.yml ps
```

### 5. Access the Services

| Service | URL |
|---|---|
| Keycloak | http://keycloak:8080/auth/ |
| Config Server | http://localhost:8071 |
| Eureka Server | http://localhost:8073 |
| Gateway Server | http://localhost:8072 |
| Gateway Routes | http://localhost:8072/actuator/gateway/routes |

## Keycloak Configuration

### 1. Configure the Client

Access the Keycloak Administration Console:

```text
http://keycloak:8080/auth/
```

Select the `spmia-realm` realm.

The original `ostock` client and its associated realm roles were removed and replaced with the following configuration:

| Setting | Value |
|---|---|
| Realm | `spmia-realm` |
| Client ID | `ds-ostock` |
| Client Protocol | `openid-connect` |
| Access Type | `confidential` |
| Direct Access Grants | Enabled |

### 2. Configure Roles

The `ds-ostock` client contains two client roles:

- `ADMIN`
- `USER`

Two composite realm roles were created:

| Realm Role | Client | Associated Client Role |
|---|---|---|
| `ds-ostock-admin` | `ds-ostock` | `ADMIN` |
| `ds-ostock-user` | `ds-ostock` | `USER` |

These roles determine which operations users can perform within the Organization Service.

### 3. Configure Users

Two users were created in Keycloak:

| Username | Assigned Realm Role | Client Role |
|---|---|---|
| `dsadmin` | `ds-ostock-admin` | `ADMIN` |
| `dsuser` | `ds-ostock-user` | `USER` |

Each user has a non-temporary password configured in Keycloak.

### 4. Configure Microservices

The Organization and Licensing services were updated to use the new Keycloak client.

Configuration files:

```text
configserver/src/main/resources/config/organization-service.properties
configserver/src/main/resources/config/licensing-service.properties
```

Keycloak configuration:

```properties
keycloak.realm=spmia-realm
keycloak.auth-server-url=http://keycloak:8080/auth
keycloak.ssl-required=external
keycloak.resource=ds-ostock
keycloak.credentials.secret=<DS_OSTOCK_CLIENT_SECRET>
keycloak.use-resource-role-mappings=true
keycloak.bearer-only=true
```

Replace the client secret placeholder with the secret from Keycloak. Do not commit actual credentials to GitHub.

After updating the configuration, rebuild and recreate the Config Server, then restart the Organization and Licensing services.

## Test Authentication and Authorization

Use Postman to verify that the microservices enforce authentication and role-based authorization.

### 1. Generate Access Tokens

Create a POST request:

```http
POST http://keycloak:8080/auth/realms/spmia-realm/protocol/openid-connect/token
```

Under **Authorization**, select Basic Auth:

- Username: `ds-ostock`
- Password: Keycloak client secret

Under **Body**, select `x-www-form-urlencoded`:

| Key | Value |
|---|---|
| `grant_type` | `password` |
| `username` | `dsadmin` or `dsuser` |
| `password` | User password |

Send the request separately for each user.

Both requests should return:

```http
200 OK
```

The JSON response contains an `access_token` used to authenticate API requests.

### 2. Test Organization GET Endpoint

Use the following endpoint:

```http
GET http://localhost:8072/organization/v1/organization/{organizationId}
```

Replace `{organizationId}` with an existing organization ID.

Test the endpoint using different authentication scenarios:

| Authentication | Expected Status |
|---|---|
| No Token | `401 Unauthorized` |
| `dsuser` Token | `200 OK` |
| `dsadmin` Token | `200 OK` |

Both authenticated users can retrieve organization information, while unauthenticated requests are rejected.

### 3. Test Organization DELETE Endpoint

Use the following endpoint:

```http
DELETE http://localhost:8072/organization/v1/organization/{organizationId}
```

Test the endpoint using the two access tokens:

| Authentication | Expected Status |
|---|---|
| `dsuser` Token | `403 Forbidden` |
| `dsadmin` Token | `204 No Content` |

The regular user is denied permission to delete organizations, while the administrator is authorized to perform the operation.

For testing, use an organization without associated licenses to avoid PostgreSQL foreign key constraint violations.

### 4. Verify HTTP Status Codes

The following status codes demonstrate the security configuration:

- **200 OK:** The authenticated user successfully accessed the endpoint.
- **204 No Content:** The administrator successfully deleted the organization.
- **401 Unauthorized:** The request was rejected because valid authentication was not provided.
- **403 Forbidden:** The authenticated user did not have the required role.

These results demonstrate that Keycloak authentication and Spring Security role-based authorization are functioning correctly.

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

If only the Config Server configuration files are modified, rebuild the Config Server separately:

```powershell
cd configserver
mvn clean package -DskipTests
docker build --build-arg JAR_FILE=target/*.jar -t ostock/configserver:0.0.1-SNAPSHOT .
cd ..
```

Recreate the Config Server:

```powershell
docker compose -f docker/docker-compose.yml up -d --no-deps --force-recreate configserver
```

Restart the affected microservices:

```powershell
docker compose -f docker/docker-compose.yml restart organizationservice licensingservice
```

This ensures the Organization and Licensing services load the updated Keycloak configuration.
