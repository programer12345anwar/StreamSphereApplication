CREATE TABLE comment_likes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT uq_comment_likes_user_comment UNIQUE (user_id, comment_id)
);

CREATE INDEX idx_comment_likes_comment_id ON comment_likes (comment_id);
