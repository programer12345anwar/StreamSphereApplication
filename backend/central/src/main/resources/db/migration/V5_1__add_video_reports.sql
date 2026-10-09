CREATE TABLE video_report (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES public.users(id),
    video_id VARCHAR(255) REFERENCES public.videos(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
