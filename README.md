# Planify Notification Service

Microservice for managing notifications in the Planify platform. Provides in-app notifications via WebSocket, email delivery via SendGrid, and SMS reminders via Vonage. Consumes Kafka events and offers administrative endpoints for managing templates and logs.

## Technologies

### Backend Framework & Language
- **Java 21** - Programming language
- **Spring Boot 3.5.7** - Application framework
- **Spring Security** - Security and authentication
- **Spring Data JPA** - Database access
- **Hibernate** - ORM framework
- **Lombok** - Boilerplate code reduction

### Database
- **PostgreSQL** - Database
- **Flyway** - Database migrations
- **HikariCP** - Connection pooling

### Security & Authentication
- **Keycloak** - OAuth2/OIDC authentication and authorization
- **Spring OAuth2 Resource Server** - JWT validation

### Messaging System
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration

### External Services
- **SendGrid** - Email service provider
- **Vonage** - SMS service provider

### Real-time Communication
- **Spring WebSocket** - WebSocket support
- **STOMP** - Messaging protocol for WebSocket

### Monitoring & Health
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer Prometheus** - Metrics export
- **Resilience4j** - Circuit breakers, retry, rate limiting, bulkheads

### API Documentation
- **SpringDoc OpenAPI 3** - OpenAPI/Swagger documentation

### Cloud & Serverless
- **Azure Functions** - Serverless functions for scheduled reminders

### Containerization
- **Docker** - Application containerization
- **Kubernetes/Helm** - Orchestration (Helm charts included)

## System Integrations

- **Keycloak**: OAuth2/OIDC authentication and authorization. All endpoints require a valid JWT Bearer token with appropriate roles (UPORABNIK or ADMINISTRATOR).
- **Kafka**: Consumes domain events from user-service and event-service to trigger notifications. Topics include join requests, invitations, and event attendance confirmations.
- **SendGrid**: Email delivery service for sending notification emails with customizable templates.
- **Vonage**: SMS service provider for sending reminder messages one day before events 11:00 AM UTC.
- **PostgreSQL**: Stores notification templates, in-app notifications, and notification logs in the notification schema with Flyway migrations.
- **WebSocket**: Real-time bidirectional communication for pushing in-app notifications to connected clients.
- **Azure Functions**: Timer-triggered serverless functions for scheduled reminder checks and sending.

## Features

### In-App Notifications
- Real-time notifications via WebSocket connection
- Tracking of unread notifications
- Automatic update of unread message counter
- Marking messages as read
- Deleting notifications

### Email Notifications
- Sending emails via SendGrid API
- Templates for different notification types
- Retry mechanism for failed deliveries

### SMS Notifications
- Sending SMS messages via Vonage API
- Reminders one day before events at 11:00 AM UTC
- Customizable SMS templates

### Template Management
- CRUD operations for notification templates
- Template activation/deactivation
- Templates for email, SMS, and in-app notifications

### Logs and Monitoring
- Tracking all sent notifications
- Filtering logs by user
- Result pagination
- Metrics via Actuator endpoints

### System Resilience
- Circuit Breaker for external calls
- Retry mechanism with exponential backoff
- Bulkhead for limiting concurrent calls
- Rate limiting

## API Endpoints

All endpoints require `Authorization: Bearer <JWT_TOKEN>` header unless otherwise specified.

### In-App Notifications (`/api/notifications`)

- `GET /api/notifications/user/{userId}` — List user notifications (paginated, UPORABNIK/ADMINISTRATOR)
- `GET /api/notifications/user/{userId}/unread` — List unread notifications (UPORABNIK/ADMINISTRATOR)
- `GET /api/notifications/user/{userId}/unread/count` — Get unread notification count (UPORABNIK/ADMINISTRATOR)
- `PUT /api/notifications/{notificationId}/read` — Mark notification as read (UPORABNIK/ADMINISTRATOR)
- `PUT /api/notifications/user/{userId}/read-all` — Mark all notifications as read (UPORABNIK/ADMINISTRATOR)
- `DELETE /api/notifications/{notificationId}` — Delete notification (UPORABNIK/ADMINISTRATOR)
- `DELETE /api/notifications/user/{userId}/all` — Delete all user notifications (UPORABNIK/ADMINISTRATOR)

### Notification Templates (`/api/notifications/templates`)

- `GET /api/notifications/templates` — List all templates (ADMINISTRATOR only)
- `GET /api/notifications/templates/{id}` — Get template by ID (ADMINISTRATOR only)
- `POST /api/notifications/templates` — Create new template (ADMINISTRATOR only)
- `PUT /api/notifications/templates/{id}` — Update template (ADMINISTRATOR only)
- `DELETE /api/notifications/templates/{id}` — Delete template (ADMINISTRATOR only)

### Notification Logs (`/api/notifications/logs`)

- `GET /api/notifications/logs` — Get all logs (paginated, ADMINISTRATOR only)
- `GET /api/notifications/logs/user/{userId}` — Get logs by user (ADMINISTRATOR only)

## WebSocket

**Connection URL:**
```
ws://localhost:8083/ws/notifications?token={jwt}&userId={uuid}
```

**Parameters:**
- `token` - JWT access token for authentication
- `userId` - User UUID

## Azure Functions

The service includes Azure Functions for scheduled reminder operations:

### DailyReminderSender (Timer Trigger)
- **Schedule**: Daily at 11:00 AM UTC
- **Cron**: `0 0 11 * * *`
- **Functionality**: Checks all events starting in the next 24 hours and sends SMS reminders to users who confirmed attendance

### ManualReminderTrigger (HTTP Trigger)
- **Endpoint**: `POST /api/ManualReminderTrigger`
- **Auth Level**: Function
- **Functionality**: Manual triggering of reminder sending for testing or exceptional cases

### HealthCheck (HTTP Trigger)
- **Endpoint**: `GET /api/health`
- **Auth Level**: Anonymous
- **Functionality**: Health check for Azure Functions with initialization status and invocation metrics

## Installation and Setup

### Prerequisites
- Java 21 or newer
- Maven 3.9+
- Docker and Docker Compose
- Git

### Infrastructure Setup

This service requires PostgreSQL, Kafka, and Keycloak to run. These dependencies are provided via Docker containers in the main Planify repository.

Clone and setup the infrastructure:

```bash
# Clone the main Planify repository
git clone https://github.com/rso-project-2025-26/planify.git
cd planify

# Follow the setup instructions in the main repository README
# This will start all required infrastructure services (PostgreSQL, Kafka, Keycloak)
```

Refer to the main Planify repository (https://github.com/rso-project-2025-26/planify) documentation for detailed infrastructure setup instructions.

### Configuration

The application uses a single `application.yaml` configuration file located in `notification-core/src/main/resources/`.

Important environment variables:

```bash
SERVER_PORT=8083
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/planify
SPRING_DATASOURCE_USERNAME=planify
SPRING_DATASOURCE_PASSWORD=planify
SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=notification
KEYCLOAK_ISSUER_URI=http://localhost:9080/realms/planify
KEYCLOAK_JWK_SET_URI=http://localhost:9080/realms/planify/protocol/openid-connect/certs
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SENDGRID_API_KEY=your-sendgrid-api-key
VONAGE_API_KEY=your-vonage-key
VONAGE_API_SECRET=your-vonage-secret
```

### Local Run

```bash
# Build project
mvn clean install

# Run application (notification-core module)
mvn spring-boot:run -pl notification-core
```

### Using Makefile

```bash
# Build project
make build

# Run application
make run

# Docker build
make docker-build

# Docker run
make docker-run

# Tests
make test
```

### Docker Run

```bash
# Build Docker image
docker build -t planify/notification-service:latest .

# Run container
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://host.docker.internal:9080/realms/planify \
  -e SENDGRID_API_KEY=your-sendgrid-api-key \
  -e VONAGE_API_KEY=your-vonage-key \
  -e VONAGE_API_SECRET=your-vonage-secret \
  planify/notification-service:latest
```

### Kubernetes/Helm Deployment

```bash
# Install with Helm (development)
helm install notification-service ./helm/notification -f ./helm/notification/values-dev.yaml

# Install with Helm (production)
helm install notification-service ./helm/notification -f ./helm/notification/values-prod.yaml

# Upgrade
helm upgrade notification-service ./helm/notification -f ./helm/notification/values-prod.yaml

# Uninstall
helm uninstall notification-service
```

### Flyway Migrations

Migrations are located in `notification-core/src/main/resources/db/migration/`:

- `V1__init.sql` - Initial schema setup
- `V2__dodane_predloge_za_obvestila.sql` - Added notification templates for notifications
- `V3__in_app_notifications.sql` - Added in-app notifications table
- `V4__event_attendees.sql` - Added table for event attendance tracking and new notification template

Manual migration run:

```bash
mvn flyway:migrate -pl notification-core
```

## Health Check & Monitoring

### Actuator Endpoints

- `GET /actuator/health` — Health check endpoint
- `GET /actuator/prometheus` — Prometheus metrics
- `GET /actuator/info` — Application information
- `GET /actuator/metrics` — Available metrics

## API Documentation

After starting the application, Swagger UI is available at:

```
http://localhost:8083/swagger-ui.html
```

## Kafka Events

The service consumes the following events from Kafka:

### Join Request Events

**Topic**: `user.join-request-sent`
- **Event**: User sends a join request to an organization
- **Payload**: Contains joinRequestId, organizationId, organizationName, requesterUserId, timestamp
- **Action**: Sends in-app notification to organization administrators

**Topic**: `user.join-request-responded`
- **Event**: Organization admin responds to a join request (APPROVED/REJECTED)
- **Payload**: Contains joinRequestId, organizationId, organizationName, requesterUserId, isApproved, timestamp
- **Action**: Sends in-app and email notification to requester about approval/rejection

### Invitation Events

**Topic**: `user.invitation-sent`
- **Event**: Organization admin invites a user to join
- **Payload**: Contains invitationId, organizationId, organizationName, invitedUserId, invitedByUserId, timestamp
- **Action**: Sends in-app notification and email invitation to invited user

**Topic**: `user.invitation-responded`
- **Event**: User responds to an invitation (ACCEPTED/DECLINED)
- **Payload**: Contains invitationId, organizationId, organizationName, invitedUserId, isAccepted, timestamp
- **Action**: Sends in-app notification to administrators about acceptance/rejection

### Event Attendance Events

**Topic**: `event-attendance-accepted`
- **Event**: User accepted event attendance
- **Payload**: Contains eventId, userId, eventName, eventStartAt, timestamp
- **Action**: Saves user for scheduled reminder (24h before event)

## Resilience4j

The service implements:

- **Circuit Breakers** - Prevention of cascading failures for external service calls (SendGrid, Vonage)
  - Sliding window: 100 calls
  - Failure rate threshold: 50%
  - Wait duration: 5s

- **Retry** - Automatic retry of failed calls with exponential backoff
  - Max attempts: 3
  - Wait duration: 1s
  - Exponential backoff: 2x multiplier

- **Rate Limiting** - Request rate limiting
  - Limit: 100 requests per second

- **Bulkheads** - Resource isolation to prevent overload
  - Max concurrent calls: 25

## Architecture

The application is divided into two main modules:

### notification-core
Main Spring Boot module containing:
- REST API endpoints
- WebSocket server
- Kafka consumers
- Business logic
- Database integration

### notification-functions
Azure Functions module for:
- Scheduled reminders (Timer Trigger)
- Manual reminder triggering (HTTP Trigger)
- Health check endpoint

## Database Structure

The service uses PostgreSQL with the following core entities in the `notification` schema:

### Notification Templates
Stores templates for different notification types. Contains:
- id (UUID, PK)
- template_key (unique identifier)
- type (EMAIL, SMS, IN_APP)
- subject, body_template, sms_template
- language, is_active
- created_at, updated_at

### In-App Notifications
User-specific notifications displayed in the application. Contains:
- id (UUID, PK)
- user_id (UUID)
- title, message
- notification_type (INVITATION, JOIN_REQUEST, etc.)
- is_read, read_at
- created_at

### Notification Logs
Audit trail of all sent notifications. Contains:
- id (UUID, PK)
- event_id (UUID)
- user_id (UUID)
- recipient_email, recipient_phone (optional)
- type (EMAIL, SMS, IN_APP)
- template_key
- subject, body
- status (SENT, FAILED)
- sent_at
- error_message
- retry_count
- external_id
- created_at

### Event Reminders
Scheduled reminders for upcoming events. Contains:
- id (UUID, PK)
- event_id, user_id (UUIDs)
- event_title, event_start_at
- is_sent, sent_at
- created_at
- notification_log_id (UUID)

Relationships: All entities use UUIDs and enforce referential integrity. Audit fields track changes. Database schema is versioned via Flyway migrations in `notification-core/src/main/resources/db/migration/`.

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=NotificationServiceTest

# Run with coverage report
mvn test jacoco:report
```
