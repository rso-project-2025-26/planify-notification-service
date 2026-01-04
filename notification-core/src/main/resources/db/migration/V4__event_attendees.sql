CREATE TABLE IF NOT EXISTS event_attendee_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_title VARCHAR(255) NOT NULL,
    event_start_at TIMESTAMPTZ NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMPTZ,
    notification_log_id UUID,
    CONSTRAINT fk_event_attendees_reminders_notification_log FOREIGN KEY (notification_log_id) REFERENCES notification_logs(id),
    CONSTRAINT uk_event_attendee_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_attendees_event_start_at ON event_attendee_reminders(event_start_at);
CREATE INDEX IF NOT EXISTS idx_event_attendees_user_id ON event_attendee_reminders(user_id);

INSERT INTO notification_templates (template_key, type, subject, body_template, sms_template, language) VALUES
('SMS_REMINDER_FALLBACK', 'EMAIL_APP', 'You have an event coming tomorrow!',
 '<h2>You have an event coming tomorrow!</h2><p>Event: {event_title}</p><p>Start time: {event_start_at}</p>',
 null,
 'en')