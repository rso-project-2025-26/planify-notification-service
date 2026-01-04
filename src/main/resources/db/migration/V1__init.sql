-- Notification Templates Table
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_key VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body_template TEXT NOT NULL,
    sms_template TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    language VARCHAR(10) DEFAULT 'sl',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_template_key ON notification_templates(template_key);
CREATE INDEX idx_template_active ON notification_templates(is_active);

-- Notification Logs Table
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID,
    user_id UUID,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    type VARCHAR(50) NOT NULL,
    template_key VARCHAR(100),
    subject VARCHAR(200) NOT NULL,
    body TEXT,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    external_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_event_id ON notification_logs(event_id);
CREATE INDEX idx_notification_user_id ON notification_logs(user_id);
CREATE INDEX idx_notification_status ON notification_logs(status);
CREATE INDEX idx_notification_created_at ON notification_logs(created_at DESC);

