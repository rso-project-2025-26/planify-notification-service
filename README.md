# Planify Notification Service

Event-driven notification microservice for Planify. Provides in-app notifications (WebSocket) and email delivery (SendGrid), with Kafka-driven events and admin endpoints for templates and logs.

Status
- Enabled now: In‑App via WebSocket, Email via SendGrid
- Planned (not implemented yet): Scheduled reminders (1 day before an event) that send SMS via Twilio

## Features

- In‑App notifications over WebSocket with unread counter updates
- Email notifications via SendGrid
- Kafka consumers for organization invitations and join-request events
- Notification templates (CRUD) with activation and language
- Notification logs with pagination and per‑user views
- OpenAPI/Swagger UI

## Tech Stack

- Spring Boot 3.x, Java 21
- PostgreSQL + Flyway
- Apache Kafka
- SendGrid (email)
- WebSocket (Spring)
- OAuth2 Resource Server (JWT, e.g., Keycloak)

## Run locally (concise)

Prerequisites
- Java 21, Maven 3.9+
- PostgreSQL running locally with a database and user
- Kafka running locally on localhost:9092
- A SendGrid API key

Default ports
- App: 8083
- WebSocket endpoint: ws://localhost:8083/ws/notifications

Environment/config (application.yaml defaults)
- Database: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- Kafka: `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- OAuth2: `OAUTH2_ISSUER_URI` or `OAUTH2_JWK_SET_URI`
- SendGrid: `SENDGRID_API_KEY`, plus optional `sendgrid.from-email`, `sendgrid.from-name`
- Kafka topics (override if needed):
  - `KAFKA_TOPIC_JOIN_REQUESTS` used for both join-request topics
  - `KAFKA_TOPIC_INVITATIONS` used for both invitation topics

Quick start (Maven)
```bash
./mvnw spring-boot:run
```

Quick start (Docker)
```bash
# build image
docker build -t planify/notification-service:latest .

# run (adjust env and mapped ports as needed)
docker run \
  -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify \
  -e SPRING_DATASOURCE_USERNAME=planify \
  -e SPRING_DATASOURCE_PASSWORD=planify \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e SENDGRID_API_KEY=your-sendgrid-api-key \
  planify/notification-service:latest
```

With Makefile
```bash
make run        # mvn spring-boot:run
make build      # package jar
make docker-build
make docker-run # adjust env/ports if needed
```

Database migration
- Flyway runs automatically on startup (schema: `notification`).

## Authentication & Roles

- REST endpoints require a valid Bearer JWT; roles are mapped to `ROLE_*` (Keycloak compatible).
- Method security guards:
  - User endpoints: `UPORABNIK` or `ADMINISTRATOR`
  - Admin endpoints: `ADMINISTRATOR`
- Swagger/OpenAPI defines a helper header `X-Roles` for testing in Swagger UI; in normal usage prefer Authorization: Bearer <token>. Optional `X-User-Id` can be used in tools for testing.

## WebSocket

- URL: `ws://localhost:8083/ws/notifications`
- Handshake auth: query parameters `token` and `userId` are currently expected by the interceptor
  - Example: `ws://localhost:8083/ws/notifications?token=<jwt>&userId=<uuid>`
- Messages: server pushes in‑app notifications and unread count updates; on marking as read via REST, a count update is pushed.

## REST API (current)

Base path: `/api/notifications`

In‑App notifications (user scope)
- GET `/user/{userId}` — list notifications (paginated)
- GET `/user/{userId}/unread` — list all unread
- GET `/user/{userId}/unread/count` — unread count
- PUT `/{notificationId}/read` — mark single notification as read
- PUT `/user/{userId}/read-all` — mark all as read
- DELETE `/{notificationId}` — delete one
- DELETE `/user/{userId}/all` — delete all for user
Required roles: `UPORABNIK` or `ADMINISTRATOR`

Templates & logs (admin)
- GET `/templates` — list all templates
- GET `/templates/{id}` — get by id
- POST `/templates` — create
- PUT `/templates/{id}` — update
- DELETE `/templates/{id}` — delete
- GET `/logs` — paginated logs
- GET `/logs/user/{userId}` — logs by user
Required role: `ADMINISTRATOR`

Minimal curl examples
```bash
# Get user notifications (JWT required)
curl -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8083/api/notifications/user/$USER_ID?page=0&size=20"

# Mark notification as read
curl -X PUT -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8083/api/notifications/$NOTIF_ID/read"
```

## Kafka events consumed

Topics (from `application.yaml`):
- `${kafka.topics.join-request-sent}` → default `user.join-request-sent`
- `${kafka.topics.join-request-responded}` → default `user.join-request-responded`
- `${kafka.topics.invitation-sent}` → default `user.invitation-sent`
- `${kafka.topics.invitation-responded}` → default `user.invitation-responded`

Handled use cases
- Invitation sent to user (email + in‑app)
- Invitation responded (email + in‑app)
- Join request sent/responded (in‑app/email per templates)

## Configuration reference (excerpt)

application.yaml defaults
```yaml
server.port: 8083
spring.datasource.url: jdbc:postgresql://localhost:5432/planify
spring.datasource.username: planify
spring.datasource.password: planify
spring.kafka.bootstrap-servers: localhost:9092
spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:9080/realms/planify
sendgrid.api-key: ${SENDGRID_API_KEY}
sendgrid.from-email: katarinagojkovick@gmail.com
sendgrid.from-name: Planify Events
```

## Deployment notes

- Dockerfile exposes 8083. Ensure DB and Kafka are reachable from the container (use `host.docker.internal` on macOS/Windows).
- `template.yaml` is included for future AWS Lambda scheduler that will trigger reminder checks. Reminders (1 day before event to attending users) and SMS via Twilio are planned, not yet implemented.

## Troubleshooting

- 401/403 on REST: verify JWT issuer and audience, and that roles `UPORABNIK`/`ADMINISTRATOR` are present.
- WebSocket fails to connect: ensure `token` and `userId` are provided as query parameters; check server logs for handshake errors.
- Email not sent: verify `SENDGRID_API_KEY`, sender domain, and template activation.
