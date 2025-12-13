# Notification Service

Event-driven notification microservice for Planify. Sends email invitations, SMS reminders, and push notifications for events.

## Features

-   **Email Notifications** via SendGrid
-   **SMS Notifications** via Twilio
-   **Push Notifications** (placeholder for Firebase FCM)
-   **Event-Driven Architecture** with Kafka consumers
-   **Template Management** with personalization
-   **Scheduled Reminders** with AWS Lambda or Spring Scheduler
-   **Notification Logging** and retry mechanism

## Tech Stack

| Component       | Technology        |
| --------------- | ----------------- |
| Framework       | Spring Boot 3.5.7 |
| Language        | Java 21           |
| Database        | PostgreSQL 16     |
| Messaging       | Apache Kafka      |
| Email           | SendGrid API      |
| SMS             | Twilio API        |
| Serverless      | AWS Lambda        |
| Template Engine | Thymeleaf         |
| Migration       | Flyway            |

## Architecture

### Kafka Topics Consumed

1. **event.created** - New event created notification
2. **guest.added** - Send invitation email/SMS to guest
3. **event.updated** - Notify all guests about event changes
4. **booking.confirmed** - Send booking confirmation
5. **rsvp.updated** - Send RSVP confirmation

### Notification Types

-   `EMAIL` - Email only
-   `SMS` - SMS only
-   `PUSH` - Push notification only
-   `ALL` - All channels

## Getting Started

### Prerequisites

-   Java 21
-   Maven 3.9+
-   PostgreSQL 16
-   Kafka (running on localhost:9092)
-   SendGrid API Key
-   Twilio Account (SID, Auth Token, Phone Number)

### Database Setup

```sql
CREATE DATABASE planify_notifications;
```

Flyway migrations will run automatically on startup.

### Environment Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/planify_notifications
SPRING_DATASOURCE_USERNAME=planify
SPRING_DATASOURCE_PASSWORD=planify

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# SendGrid
SENDGRID_API_KEY=your_sendgrid_api_key

# Twilio
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=+1234567890

# AWS (for Lambda)
AWS_REGION=eu-central-1
```

### Run Locally

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Or run JAR
java -jar target/notification-service-1.0.0-SNAPSHOT.jar
```

Service runs on **http://localhost:8084**

### Docker

```bash
# Build image
docker build -t planify/notification-service:1.0.0 .

# Run container
docker run -p 8084:8084 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify_notifications \
  -e SENDGRID_API_KEY=your_key \
  -e TWILIO_ACCOUNT_SID=your_sid \
  planify/notification-service:1.0.0
```

## API Endpoints

### Template Management

```bash
# Get all templates
GET /api/notifications/templates

# Get template by ID
GET /api/notifications/templates/{id}

# Get template by key
GET /api/notifications/templates/key/EVENT_INVITATION

# Create template
POST /api/notifications/templates
{
  "templateKey": "CUSTOM_TEMPLATE",
  "type": "EMAIL",
  "subject": "Hello ${recipientName}",
  "bodyTemplate": "<p>Your custom message</p>",
  "isActive": true,
  "language": "sl"
}

# Update template
PUT /api/notifications/templates/{id}

# Delete template
DELETE /api/notifications/templates/{id}
```

### Notification Logs

```bash
# Get all logs (paginated)
GET /api/notifications/logs?page=0&size=20

# Get logs for specific event
GET /api/notifications/logs/event/{eventId}

# Get logs for specific user
GET /api/notifications/logs/user/{userId}
```

### Manual Actions

```bash
# Trigger reminder check manually
POST /api/notifications/reminders/check

# Send manual notification
POST /api/notifications/send
{
  "recipientEmail": "user@example.com",
  "recipientPhone": "+38640123456",
  "templateKey": "EVENT_REMINDER",
  "variables": {
    "recipientName": "John",
    "eventName": "Birthday Party",
    "eventDate": "25.12.2025 18:00"
  }
}
```

## Database Schema

### notification_templates

| Column        | Type         | Description                |
| ------------- | ------------ | -------------------------- |
| id            | UUID         | Primary key                |
| template_key  | VARCHAR(100) | Unique template identifier |
| type          | VARCHAR(50)  | EMAIL, SMS, PUSH, ALL      |
| subject       | VARCHAR(200) | Email subject template     |
| body_template | TEXT         | HTML email template        |
| sms_template  | TEXT         | SMS message template       |
| is_active     | BOOLEAN      | Template enabled/disabled  |
| language      | VARCHAR(10)  | sl, en                     |
| created_at    | TIMESTAMP    | Creation time              |
| updated_at    | TIMESTAMP    | Last update time           |

### notification_logs

| Column          | Type         | Description           |
| --------------- | ------------ | --------------------- |
| id              | UUID         | Primary key           |
| event_id        | UUID         | Related event         |
| user_id         | UUID         | Recipient user        |
| recipient_email | VARCHAR(255) | Email address         |
| recipient_phone | VARCHAR(50)  | Phone number          |
| type            | VARCHAR(50)  | Notification type     |
| template_key    | VARCHAR(100) | Template used         |
| subject         | VARCHAR(200) | Sent subject          |
| body            | TEXT         | Sent message          |
| status          | VARCHAR(20)  | PENDING, SENT, FAILED |
| sent_at         | TIMESTAMP    | Send time             |
| error_message   | TEXT         | Error details         |
| retry_count     | INTEGER      | Retry attempts        |
| external_id     | VARCHAR(255) | SendGrid/Twilio ID    |
| created_at      | TIMESTAMP    | Log creation time     |

### scheduled_reminders

| Column              | Type         | Description       |
| ------------------- | ------------ | ----------------- |
| id                  | UUID         | Primary key       |
| event_id            | UUID         | Related event     |
| event_name          | VARCHAR(255) | Event name        |
| event_start_time    | TIMESTAMP    | Event start       |
| recipient_email     | VARCHAR(255) | Email address     |
| recipient_phone     | VARCHAR(50)  | Phone number      |
| recipient_name      | VARCHAR(255) | Recipient name    |
| reminder_time       | TIMESTAMP    | When to send      |
| hours_before_event  | INTEGER      | Lead time         |
| type                | VARCHAR(50)  | Notification type |
| is_sent             | BOOLEAN      | Sent flag         |
| sent_at             | TIMESTAMP    | Actual send time  |
| notification_log_id | UUID         | Related log       |
| created_at          | TIMESTAMP    | Creation time     |

## Default Templates

The service comes with 5 pre-configured templates:

1. **EVENT_INVITATION** - Send when guest is added
2. **EVENT_REMINDER** - Sent 24h before event
3. **RSVP_CONFIRMATION** - Sent when guest responds
4. **BOOKING_CONFIRMATION** - Sent after payment
5. **EVENT_UPDATED** - Sent when event details change

Templates use `${variable}` syntax for personalization.

## AWS Lambda Deployment

### Build Lambda Package

```bash
./mvnw clean package
# Lambda JAR: target/notification-service-1.0.0-SNAPSHOT-lambda.jar
```

### Lambda Configuration

-   **Runtime:** Java 21
-   **Handler:** `com.planify.notification.lambda.ReminderSchedulerHandler::handleRequest`
-   **Memory:** 512 MB
-   **Timeout:** 60 seconds
-   **Trigger:** EventBridge (CloudWatch Events) - cron(0/30 \* \* _ ? _)

### Environment Variables for Lambda

```
SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-endpoint:5432/planify_notifications
SPRING_DATASOURCE_USERNAME=planify
SPRING_DATASOURCE_PASSWORD=your_password
SENDGRID_API_KEY=your_key
TWILIO_ACCOUNT_SID=your_sid
TWILIO_AUTH_TOKEN=your_token
```

### CloudWatch Events Rule

```json
{
	"schedule": "rate(30 minutes)"
}
```

## Testing

```bash
# Run unit tests
./mvnw test

# Test email sending (requires valid SendGrid API key)
curl -X POST http://localhost:8084/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipientEmail": "test@example.com",
    "templateKey": "EVENT_INVITATION",
    "variables": {
      "recipientName": "Test User",
      "eventName": "Test Event",
      "eventDate": "01.01.2026 20:00",
      "eventLocation": "Test Location",
      "rsvpLink": "https://planify.com/rsvp/123"
    }
  }'
```

## Configuration

### application.yaml

Key configuration options:

```yaml
notification:
    email:
        enabled: true
        retry-attempts: 3
    sms:
        enabled: true
        retry-attempts: 3
    reminder:
        check-interval-minutes: 30
        advance-notice-hours: 24
```

## Monitoring

Health check: `GET /actuator/health`

Metrics: `GET /actuator/metrics`

Prometheus: `GET /actuator/prometheus`

## Error Handling

-   Failed notifications are logged with status `FAILED`
-   Retry mechanism up to 3 attempts (configurable)
-   Errors stored in `error_message` field
-   External IDs (SendGrid/Twilio) stored for tracking

## Contributing

1. Create feature branch
2. Implement changes
3. Add tests
4. Submit pull request

## License

MIT License
