-- In-App Notifications Table for WebSocket-based notifications
CREATE TABLE in_app_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50),
    reference_id UUID,
    reference_type VARCHAR(50),
    action_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX idx_in_app_notification_user_id ON in_app_notifications(user_id);
CREATE INDEX idx_in_app_notification_user_read ON in_app_notifications(user_id, is_read);
CREATE INDEX idx_in_app_notification_created_at ON in_app_notifications(created_at DESC);
CREATE INDEX idx_in_app_notification_reference ON in_app_notifications(reference_type, reference_id);
