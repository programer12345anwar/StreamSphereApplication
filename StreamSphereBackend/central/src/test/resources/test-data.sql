-- Smaller dataset for integration tests
INSERT INTO users (id, name, email, password, phone_number, dob, gender, country, created_at, updated_at) VALUES
('aaaaaaaa-0000-0000-0000-000000000001','Test User1','test.user1@example.com','password',9000001001,'1990-01-01','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('aaaaaaaa-0000-0000-0000-000000000002','Test User2','test.user2@example.com','password',9000001002,'1992-02-02','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

INSERT INTO tags (id, name) VALUES
('bbbbbbbb-0000-0000-0000-000000000001','Java Programming'),
('bbbbbbbb-0000-0000-0000-000000000002','Spring Boot');

INSERT INTO channels (id, name, user_id, description, watch_hours, is_monetized, total_views, total_like_count, total_subs, created_at, updated_at) VALUES
('cccccccc-0000-0000-0000-000000000001','TestChannel','aaaaaaaa-0000-0000-0000-000000000001','Test channel for integration',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

INSERT INTO videos (id, name, description, upload_date_time, updated_at, video_link, thumbnail_link, views, channel_id) VALUES
('vtest-0001','Test Video 1','Integration test video',CURRENT_TIMESTAMP - interval '10 days',CURRENT_TIMESTAMP - interval '9 days','https://cdn.streamsphere.dev/videos/vtest-0001','https://cdn.streamsphere.dev/thumbs/vtest-0001.jpg',123,'cccccccc-0000-0000-0000-000000000001');

INSERT INTO playlists (id, name, channel_id) VALUES
('dddddddd-0000-0000-0000-000000000001','Test Playlist','cccccccc-0000-0000-0000-000000000001');

INSERT INTO playlists_videos (playlist_id, video_id) VALUES
('dddddddd-0000-0000-0000-000000000001','vtest-0001');

INSERT INTO videos_tags (video_id, tag_id) VALUES
('vtest-0001','bbbbbbbb-0000-0000-0000-000000000001');
