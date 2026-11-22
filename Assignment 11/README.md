
# Assignment 11 — Distributed Tracing and Centralized Logging

**Course:** SFWE 510 — Cloud Native Software Engineering  
**Project:** *Spring Microservices in Action* (2nd Edition), Chapter 11  
**Repository:** [SFWE-510-Assignments](https://github.com/vjimenez6569/SFWE-510-Assignments)  
**Location:** `Assignment 11/`

## About

This assignment focuses on implementing distributed tracing and centralized logging in a microservices architecture using **Spring Cloud Sleuth**, **Logback**, the **ELK Stack (Elasticsearch, Logstash, Kibana)**, and **Zipkin**.

The application consists of several main services:

- **Organization Service:** Manages organization information and generates logs containing trace IDs.
- **Licensing Service:** Manages licensing information and communicates with the Organization Service.
- **Gateway Server:** Routes incoming API requests to the appropriate microservices while propagating tracing information.
- **Spring Cloud Sleuth:** Generates and propagates trace IDs and span IDs across microservices.
- **Logback:** Captures application logs and forwards them to Logstash.
- **Logstash:** Collects and processes logs before forwarding them to Elasticsearch.
- **Elasticsearch:** Stores and indexes application logs for searching and analysis.
- **Kibana:** Provides an interface to search logs, examine trace IDs, and verify service-specific tags.
- **Zipkin:** Visualizes distributed traces and displays the duration of operations across microservices.

The assignment demonstrates several features:

- **Distributed Tracing:** Tracks requests across multiple microservices using a shared trace ID.
- **Centralized Logging:** Collects application logs from multiple services in Elasticsearch.
- **Trace ID Propagation:** Demonstrates that the same trace ID can be used to correlate logs from different services.
- **Log Visualization:** Uses Kibana to search and examine application logs.
- **Performance Monitoring:** Uses Zipkin to visualize request timelines and individual span durations.
- **Service-Specific Log Tags:** Replaces the default `manningPublications` tag with tags identifying each microservice.
- **Log Verification:** Demonstrates that new requests generate logs containing the updated tags.

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

Run all commands from the `Assignment 11` directory unless otherwise specified.

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

Allow approximately 1–2 minutes for the services to start. Elasticsearch and Kibana may require additional time.

Docker Compose starts the application's microservices along with PostgreSQL, Config Server, Eureka, Elasticsearch, Logstash, Kibana, and Zipkin.

### 3. Verify Containers

```powershell
docker compose -f docker/docker-compose.yml ps
```

Verify that all required containers are running.

To check the ELK and Zipkin logs:

```powershell
docker compose -f docker/docker-compose.yml logs --tail=50 elasticsearch logstash kibana zipkin
```

If any service fails to start, examine its container logs for errors.

### 4. Access the Services

| Service | URL / Port |
|---|---|
| Config Server | http://localhost:8071 |
| Eureka Server | http://localhost:8070 |
| Gateway Server | http://localhost:8072 |
| Licensing Service | http://localhost:8080 |
| Organization Service | http://localhost:8081 |
| Elasticsearch | http://localhost:9200 |
| Kibana | http://localhost:5601 |
| Zipkin | http://localhost:9411 |
| Logstash | `logstash:5000` (Docker network) |

## Spring Cloud Sleuth and Logback Configuration

### 1. Spring Cloud Sleuth

Spring Cloud Sleuth provides distributed tracing capabilities for the microservices.

The following dependencies are included in the Gateway, Licensing, and Organization services:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

Sleuth generates two important identifiers:

- **Trace ID:** Identifies the complete request as it travels between microservices.
- **Span ID:** Identifies an individual operation within the trace.

When a request passes through the Gateway, Licensing Service, and Organization Service, Sleuth propagates tracing information between the services.

This allows logs from different services to be correlated using the same trace ID.

### 2. Logback Configuration

Logback is used to capture application logs and forward them to Logstash.

The configuration is located in:

```text
gatewayserver/src/main/resources/logback-spring.xml
licensing-service/src/main/resources/logback-spring.xml
organization-service/src/main/resources/logback-spring.xml
```

The following configuration is used:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>

    <springProperty scope="context"
                    name="application_name"
                    source="spring.application.name"/>

    <appender name="logstash"
              class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5000</destination>

        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${application_name}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="logstash"/>
        <appender-ref ref="CONSOLE"/>
    </root>

    <logger name="org.springframework" level="INFO"/>
    <logger name="com.optimagrowth" level="DEBUG"/>
</configuration>
```

The configuration sends JSON-formatted logs to Logstash on port `5000`.

The `customFields` property adds the application's name to each log event.

This field is later used by Logstash to assign service-specific tags.

### 3. Zipkin Configuration

Zipkin is configured through the centralized configuration files:

```text
configserver/src/main/resources/config/gateway-server.yml
configserver/src/main/resources/config/licensing-service.properties
configserver/src/main/resources/config/organization-service.properties
```

For the Licensing and Organization services:

```properties
spring.zipkin.base-url=http://zipkin:9411
spring.sleuth.sampler.probability=1.0
```

For the Gateway Server:

```yaml
spring:
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 1.0
```

The sampling probability is set to `1.0`, allowing all requests to be sampled for tracing during the assignment.

Zipkin receives tracing information and displays the relationships and durations of operations across microservices.

## ELK Stack Integration

The ELK Stack is used to collect, store, and visualize logs generated by the microservices.

### 1. Elasticsearch

Elasticsearch stores application logs in searchable indices.

Verify that Elasticsearch is running by opening:

```text
http://localhost:9200
```

A successful response displays JSON containing information about the Elasticsearch cluster.

To view available indices:

```text
http://localhost:9200/_cat/indices?v
```

### 2. Logstash

Logstash receives JSON log events from the microservices through TCP port `5000`.

The configuration file is located at:

```text
docker/config/logstash.conf
```

The initial configuration used the following filter:

```ruby
filter {
  mutate {
    add_tag => [ "manningPublications" ]
  }
}
```

This assigned the same tag to all application logs regardless of which microservice generated them.

The configuration was later modified to assign a different tag to each service.

### 3. Kibana

Kibana provides an interface for searching and examining the logs stored in Elasticsearch.

Open:

```text
http://localhost:5601
```

Navigate to **Stack Management → Index Patterns**.

Create the following index pattern:

```text
logstash-*
```

Select `@timestamp` as the time field.

The index pattern allows Kibana to search Elasticsearch indices whose names begin with `logstash-`.

After creating the index pattern, navigate to **Discover** to examine the application logs.

If the index pattern is unavailable, generate requests first and verify that Logstash has created the corresponding Elasticsearch indices.

## Demonstrate Trace ID Propagation Using Kibana

Use Postman and Kibana to verify that tracing information is propagated between microservices.

### 1. Send a Request Through the Gateway

Send a GET request using an existing organization ID and license ID:

```http
GET http://localhost:8072/license/v1/organization/{organizationId}/license/{licenseId}/rest
```

Replace `{organizationId}` and `{licenseId}` with valid IDs from the database.

This request is routed through the Gateway to the Licensing Service.

When the Licensing Service retrieves organization information through REST, the request also involves the Organization Service.

### 2. View Logs in Kibana

Open Kibana:

```text
http://localhost:5601
```

Navigate to **Discover** and select the `logstash-*` index pattern.

Set the time filter to include the most recent requests.

Locate the logs generated by the Gateway, Licensing Service, and Organization Service.

Expand a log entry to examine its fields.

Sleuth adds tracing information such as:

```json
{
  "traceId": "8a1b2c3d4e5f6789",
  "spanId": "1234567890abcdef",
  "service": "licensing-service",
  "message": "Retrieving license information"
}
```

The example above illustrates the expected log structure.

### 3. Verify the Shared Trace ID

Copy a trace ID from one of the log entries.

Search Kibana using that trace ID:

```text
traceId:"8a1b2c3d4e5f6789"
```

If the trace ID is present in the logs, Kibana displays matching events associated with the same request.


## Distributed Tracing Visualization with Zipkin

Zipkin provides a visual representation of requests traveling between microservices.

### 1. Open Zipkin

Navigate to:

```text
http://localhost:9411
```

### 2. Generate Requests

Use Postman to send several requests through the Gateway:

```http
GET http://localhost:8072/license/v1/organization/{organizationId}/license/{licenseId}/rest
```

Repeat the request several times to generate tracing data.

### 3. Search for Traces

In Zipkin:

1. Open the trace search interface.
2. Select the Licensing Service or another participating service.
3. Select a recent time range.
4. Search for traces.
5. Open one of the returned traces.

Zipkin displays the operations associated with the selected request.

### 4. Examine the Trace Timeline

A distributed request may involve the following services:

```text
API Gateway
    |
    v
Licensing Service
    |
    v
Organization Service
```

Zipkin displays a timeline containing the spans associated with the request.

Each span includes timing information that can be used to examine how long individual operations take.

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

After modifying Java source code or Logback configuration, rebuild the application:

```powershell
mvn clean package dockerfile:build
```

Then recreate the containers using the updated images:

```powershell
docker compose -f docker/docker-compose.yml up -d --force-recreate
```

If only the Logstash configuration was modified, recreate the Logstash container:

```powershell
docker compose -f docker/docker-compose.yml up -d --no-deps --force-recreate logstash
```

If only the Config Server configuration files were modified, rebuild the Config Server separately:

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
docker compose -f docker/docker-compose.yml restart organizationservice licensingservice gatewayserver
```

This ensures that the microservices load the updated tracing and logging configuration.
