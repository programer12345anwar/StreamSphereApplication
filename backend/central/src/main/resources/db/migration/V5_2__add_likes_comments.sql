CREATE TABLE video_likes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    video_id VARCHAR(255) NOT NULL,
    is_like BOOLEAN NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_videolike_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_videolike_video FOREIGN KEY (video_id) REFERENCES public.videos(id) ON DELETE CASCADE,
    CONSTRAINT uq_videolike_user_video UNIQUE (user_id, video_id)
);

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    text TEXT NOT NULL,
    user_id UUID NOT NULL,
    video_id VARCHAR(255) NOT NULL,
    parent_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_video FOREIGN KEY (video_id) REFERENCES public.videos(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);
