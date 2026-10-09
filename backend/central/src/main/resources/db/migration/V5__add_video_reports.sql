CREATE TABLE video_report (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES app_user(id),
    video_id VARCHAR(255) REFERENCES video(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
