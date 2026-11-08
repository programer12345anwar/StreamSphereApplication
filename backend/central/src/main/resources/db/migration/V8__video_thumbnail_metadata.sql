-- Add thumbnail processing metadata and transactional event delivery.
ALTER TABLE public.videos
    ADD COLUMN IF NOT EXISTS thumbnail_source character varying(32),
    ADD COLUMN IF NOT EXISTS thumbnail_processing_status character varying(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS duration_seconds integer,
    ADD COLUMN IF NOT EXISTS thumbnail_processing_event_id uuid;

UPDATE public.videos
SET thumbnail_source = CASE
    WHEN thumbnail_link IS NOT NULL AND thumbnail_link <> '' THEN 'CUSTOM'
    ELSE NULL
END,
thumbnail_processing_status = CASE
    WHEN thumbnail_link IS NOT NULL AND thumbnail_link <> '' THEN 'READY'
    ELSE 'PENDING'
END;

CREATE TABLE public.video_processing_outbox (
    id uuid NOT NULL,
    video_id character varying(255) NOT NULL,
    video_url character varying(2048) NOT NULL,
    custom_thumbnail_url character varying(2048),
    created_at timestamp(6) without time zone NOT NULL,
    published_at timestamp(6) without time zone,
    CONSTRAINT video_processing_outbox_pkey PRIMARY KEY (id)
);

CREATE INDEX video_processing_outbox_pending_idx
    ON public.video_processing_outbox (created_at)
    WHERE published_at IS NULL;
