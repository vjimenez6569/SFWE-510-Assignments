
# Assignment 10 — Event-Driven Microservices

**Course:** SFWE 510 — Cloud Native Software Engineering  
**Project:** *Spring Microservices in Action* (2nd Edition), Chapter 10  
**Repository:** [SFWE-510-Assignments](https://github.com/vjimenez6569/SFWE-510-Assignments)  
**Location:** `Assignment 10/`

## About

This assignment focuses on implementing event-driven communication and distributed caching in microservices using **Apache Kafka**, **Spring Cloud Stream**, and **Redis**.

The application consists of several main services:

- **Organization Service:** Manages organization information and publishes Kafka events when organizations are created, retrieved, updated, or deleted.
- **Licensing Service:** Manages licensing information, consumes Kafka messages, and uses Redis to cache organization information.
- **Apache Kafka:** Acts as a message broker to enable asynchronous communication between microservices.
- **Redis:** Provides distributed caching to reduce unnecessary REST requests to the Organization Service.
- **Gateway Server:** Routes incoming API requests to the appropriate microservices.

The assignment demonstrates several features:

- **Event-Driven Communication:** Allows microservices to communicate asynchronously through Kafka messages.
- **Message Producer:** Uses Spring Cloud Stream to publish organization events to Kafka.
- **Message Consumer:** Receives and processes Kafka messages in the Licensing Service.
- **Kafka Topic Configuration:** Demonstrates changing the topic name used for communication between services.
- **Distributed Caching:** Stores organization information in Redis to improve performance and reduce repeated service calls.
- **Cache Invalidation:** Uses Kafka events to remove outdated organization information from Redis when changes occur.

The application also uses **Spring Cloud Config Server** for centralized configuration, **Netflix Eureka** for service discovery, and **PostgreSQL** for persistent data storage.

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

Run all commands from the `Assignment 10` directory unless otherwise specified.

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

Allow approximately 1–2 minutes for the services to start.

Docker Compose starts the application's microservices along with Kafka, Zookeeper, Redis, PostgreSQL, Config Server, Eureka, and the API Gateway.

### 3. Verify Containers

```powershell
docker compose -f docker/docker-compose.yml ps
```

Verify that all required containers are running.

To check Kafka and Redis logs:

```powershell
docker compose -f docker/docker-compose.yml logs --tail=50 kafkaserver redisserver
```

### 4. Access the Services

| Service | URL / Port |
|---|---|
| Config Server | http://localhost:8071 |
| Eureka Server | http://localhost:8073 |
| Gateway Server | http://localhost:8072 |
| Licensing Service | http://localhost:8080 |
| Organization Service | http://localhost:8081 |
| Apache Kafka | `kafkaserver:9092` (Docker network) |
| Redis | `redisserver:6379` (Docker network) |

## Kafka Configuration

### 1. Message Producer

The Organization Service acts as the Kafka message producer.

The producer is implemented in:

```text
organization-service/src/main/java/com/optimagrowth/organization/events/source/SimpleSourceBean.java
```

When an organization operation occurs, the Organization Service creates an `OrganizationChangeModel` event containing:

- Event type
- Action (`GET`, `SAVE`, `UPDATE`, or `DELETE`)
- Organization ID
- Correlation ID

Spring Cloud Stream publishes the event to the configured Kafka topic.

### 2. Message Consumer

The Licensing Service acts as the Kafka message consumer.

The consumer is implemented in:

```text
licensing-service/src/main/java/com/optimagrowth/license/events/handler/OrganizationChangeHandler.java
```

The consumer uses the `@StreamListener` annotation to receive organization events.

When a message is received, the Licensing Service identifies the action and organization ID.

For `SAVE`, `UPDATE`, and `DELETE` events, the consumer also invalidates the corresponding Redis cache entry.

### 3. Change Kafka Topic Name

The original Kafka topic was:

```text
orgChangeTopic
```

It was changed to:

```text
organizationUpdatesTopic
```

The topic is configured in the following files:

```text
configserver/src/main/resources/config/organization-service.properties
configserver/src/main/resources/config/licensing-service.properties
```

Organization Service producer configuration:

```properties
spring.cloud.stream.bindings.output.destination=organizationUpdatesTopic
```

Licensing Service consumer configuration:

```properties
spring.cloud.stream.bindings.inboundOrgChanges.destination=organizationUpdatesTopic
```

Both services must use the same topic name for messages to be delivered correctly.

After updating the configuration, rebuild the Config Server and restart the affected microservices.

## Test Kafka Producer and Consumer

Use Postman to verify that Kafka messages are successfully published and consumed.

### 1. Create an Organization

Send a POST request:

```http
POST http://localhost:8081/v1/organization
```

Under **Body**, select `raw` and `JSON`:

```json
{
  "name": "Assignment 10 Test Organization",
  "contactName": "Test User",
  "contactEmail": "test@example.com",
  "contactPhone": "555-0100"
}
```

A successful request creates an organization and triggers a `SAVE` event.

### 2. Verify Kafka Messaging

Open PowerShell and run:

```powershell
docker compose -f docker/docker-compose.yml logs -f organizationservice licensingservice
```

The Organization Service publishes the event to Kafka.

The Licensing Service receives the event and logs the organization ID.

Example:

```text
Received a message of type
com.optimagrowth.organization.events.model.OrganizationChangeModel

Received a SAVE event from the organization service
for organization id <organizationId>
```

This demonstrates successful asynchronous communication between the two microservices.

### 3. Verify the Renamed Topic

After changing the topic to `organizationUpdatesTopic`, repeat the POST request using a new organization.

Verify that the Licensing Service continues receiving the `SAVE` event.

This demonstrates that both the producer and consumer are correctly configured to communicate using the new Kafka topic.

## Distributed Caching with Redis

Redis is used by the Licensing Service to cache organization information.

The caching logic is implemented in:

```text
licensing-service/src/main/java/com/optimagrowth/license/service/client/OrganizationRestTemplateClient.java
```

The Redis repository is implemented in:

```text
licensing-service/src/main/java/com/optimagrowth/license/repository/OrganizationRedisRepository.java
```

### 1. Test Redis Cache Miss

Send a GET request using an existing organization ID and associated license ID:

```http
GET http://localhost:8080/v1/organization/{organizationId}/license/{licenseId}/rest
```

When the organization is not already cached:

1. The Licensing Service checks Redis.
2. Redis does not contain the requested organization.
3. The Licensing Service retrieves the organization from the Organization Service.
4. The organization information is stored in Redis.

This demonstrates a **cache miss**.

### 2. Test Redis Cache Hit

Repeat the same GET request.

The Licensing Service should retrieve the organization directly from Redis rather than making another REST request.

Example log:

```text
I have successfully retrieved an organization
e839ee96-28de-4f67-bb79-870ca89743a0
from the redis cache
```

This demonstrates a **cache hit**.

### 3. Verify Redis Cache Entries

Open Redis CLI:

```powershell
docker compose -f docker/docker-compose.yml exec redisserver redis-cli
```

List cached organization keys:

```redis
KEYS organization:*
```

The cached organization should appear in the results.

### 4. Test Cache Invalidation

Update an existing organization through Postman:

```http
PUT http://localhost:8081/v1/organization/{organizationId}
```

Example JSON body:

```json
{
  "id": "e839ee96-28de-4f67-bb79-870ca89743a0",
  "name": "Ostock Updated",
  "contactName": "Illary Huaylupo",
  "contactEmail": "illaryhs@gmail.com",
  "contactPhone": "888888888"
}
```

When the organization is updated:

1. The Organization Service updates the organization in PostgreSQL.
2. The Organization Service publishes an `UPDATE` event to Kafka.
3. The Licensing Service receives the event.
4. The Kafka consumer calls `redisRepository.deleteById(organizationId)`.
5. The outdated organization information is removed from Redis.

Expected Licensing Service log:

```text
Received a UPDATE event for organization <organizationId>
Removed organization <organizationId> from Redis cache
```

### 5. Verify Cache Invalidation in Redis

In Redis CLI, run:

```redis
EXISTS organization:e839ee96-28de-4f67-bb79-870ca89743a0
```

Expected result after invalidation:

```text
(integer) 0
```

This confirms that the outdated cached organization was removed.

### 6. Verify Cache Refresh

Repeat the original Licensing Service GET request:

```http
GET http://localhost:8080/v1/organization/{organizationId}/license/{licenseId}/rest
```

Since the cached entry was removed, the Licensing Service retrieves the updated organization information and stores it in Redis again.

Repeat the GET request once more to demonstrate another cache hit.

The cached organization should now contain the updated information.

This demonstrates the complete distributed caching process:

**Cache Miss → Cache Hit → Organization Update → Kafka Event → Cache Invalidation → Cache Refresh**

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

If only the Licensing Service Java code was modified, recreate that container:

```powershell
docker compose -f docker/docker-compose.yml up -d --no-deps --force-recreate licensingservice
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

This ensures that the Organization and Licensing services load the updated Kafka configuration.
