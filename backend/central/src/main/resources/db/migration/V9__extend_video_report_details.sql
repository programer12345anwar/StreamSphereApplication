ALTER TABLE video_report
    ADD COLUMN IF NOT EXISTS description TEXT;

CREATE INDEX IF NOT EXISTS idx_video_report_status_created_at
    ON video_report (status, created_at DESC);
