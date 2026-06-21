-- Seed data for StreamSphereApplication (central service)
-- NOTE: This SQL assumes standard Hibernate physical naming (snake_case lowercased).
-- If your DB uses different table/column names adjust identifiers accordingly.

-- 20 Users
INSERT INTO users (id, name, email, password, phone_number, dob, gender, country, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111','Alex Johnson','alex.johnson@example.com','password',9000000001,'1995-05-12','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222','Emily Davis','emily.davis@example.com','password',9000000002,'1992-09-02','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333','Michael Smith','michael.smith@example.com','password',9000000003,'1988-03-15','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-444444444444','Sarah Lee','sarah.lee@example.com','password',9000000004,'1994-07-21','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555555','David Brown','david.brown@example.com','password',9000000005,'1990-11-11','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('66666666-6666-6666-6666-666666666666','Laura Wilson','laura.wilson@example.com','password',9000000006,'1993-06-05','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('77777777-7777-7777-7777-777777777777','James Miller','james.miller@example.com','password',9000000007,'1987-12-30','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('88888888-8888-8888-8888-888888888888','Olivia Taylor','olivia.taylor@example.com','password',9000000008,'1996-02-17','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('99999999-9999-9999-9999-999999999999','Daniel Anderson','daniel.anderson@example.com','password',9000000009,'1991-08-29','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('10101010-1010-1010-1010-101010101010','Sophia Thomas','sophia.thomas@example.com','password',9000000010,'1998-04-04','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('12121212-1212-1212-1212-121212121212','Ethan Martinez','ethan.martinez@example.com','password',9000000011,'1990-10-10','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('13131313-1313-1313-1313-131313131313','Ava Robinson','ava.robinson@example.com','password',9000000012,'1997-01-23','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('14141414-1414-1414-1414-141414141414','William Clark','william.clark@example.com','password',9000000013,'1989-09-09','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('15151515-1515-1515-1515-151515151515','Isabella Walker','isabella.walker@example.com','password',9000000014,'1995-12-12','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('16161616-1616-1616-1616-161616161616','Joseph Hall','joseph.hall@example.com','password',9000000015,'1986-05-05','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('17171717-1717-1717-1717-171717171717','Mia Allen','mia.allen@example.com','password',9000000016,'1999-03-03','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('18181818-1818-1818-1818-181818181818','Liam Young','liam.young@example.com','password',9000000017,'1992-06-06','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('19191919-1919-1919-1919-191919191919','Charlotte Hernandez','charlotte.hernandez@example.com','password',9000000018,'1994-08-08','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('20202020-2020-2020-2020-202020202020','Noah King','noah.king@example.com','password',9000000019,'1991-02-02','Male','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('21212121-2121-2121-2121-212121212121','Amelia Wright','amelia.wright@example.com','password',9000000020,'1993-07-07','Female','USA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

-- 20 Tags
INSERT INTO tags (id, name) VALUES
('20000000-0000-0000-0000-000000000001','Java Programming'),
('20000000-0000-0000-0000-000000000002','Spring Boot'),
('20000000-0000-0000-0000-000000000003','Microservices'),
('20000000-0000-0000-0000-000000000004','Docker'),
('20000000-0000-0000-0000-000000000005','Kubernetes'),
('20000000-0000-0000-0000-000000000006','System Design'),
('20000000-0000-0000-0000-000000000007','React'),
('20000000-0000-0000-0000-000000000008','AI/ML'),
('20000000-0000-0000-0000-000000000009','DevOps'),
('20000000-0000-0000-0000-000000000010','Interview Preparation'),
('20000000-0000-0000-0000-000000000011','Docker Compose'),
('20000000-0000-0000-0000-000000000012','Spring Security'),
('20000000-0000-0000-0000-000000000013','Concurrency'),
('20000000-0000-0000-0000-000000000014','Data Structures'),
('20000000-0000-0000-0000-000000000015','Hibernate'),
('20000000-0000-0000-0000-000000000016','Design Patterns'),
('20000000-0000-0000-0000-000000000017','Testing'),
('20000000-0000-0000-0000-000000000018','CI/CD'),
('20000000-0000-0000-0000-000000000019','Cloud'),
('20000000-0000-0000-0000-000000000020','Performance Tuning');

-- 10 Channels (owned by the first 10 users)
INSERT INTO channels (id, name, user_id, description, watch_hours, is_monetized, total_views, total_like_count, total_subs, created_at, updated_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','CodeCrafters','11111111-1111-1111-1111-111111111111','Official channel for CodeCrafters',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','SpringInsights','22222222-2222-2222-2222-222222222222','Spring Boot tutorials and deep dives',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('cccccccc-cccc-cccc-cccc-cccccccccccc','MicroserviceMasters','33333333-3333-3333-3333-333333333333','Patterns and practices for microservices',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('dddddddd-dddd-dddd-dddd-dddddddddddd','DockerDive','44444444-4444-4444-4444-444444444444','Docker tips and containerization',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee','KubeCast','55555555-5555-5555-5555-555555555555','Kubernetes tutorials',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('ffffffff-ffff-ffff-ffff-ffffffffffff','SystemDesignPro','66666666-6666-6666-6666-666666666666','System design and architecture',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('12121212-1212-1212-1212-121212121212','ReactRocket','77777777-7777-7777-7777-777777777777','React tips and fullstack demos',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('13131313-1313-1313-1313-131313131313','AIMinds','88888888-8888-8888-8888-888888888888','AI and Machine Learning tutorials',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('14141414-1414-1414-1414-141414141414','DevOpsDaily','99999999-9999-9999-9999-999999999999','Daily DevOps tips and CI/CD',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('15151515-1515-1515-1515-151515151515','InterviewAce','10101010-1010-1010-1010-101010101010','Interview preparation and system design',0,false,0,0,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

-- 50 Videos
-- We'll insert 50 videos with IDs vid-00001..vid-00050
INSERT INTO videos (id, name, description, upload_date_time, updated_at, video_link, thumbnail_link, views, channel_id) VALUES
('vid-00001','Intro to Java Streams','Hands-on tutorial: Intro to Java Streams — Duration: 12 minutes',CURRENT_TIMESTAMP - interval '400 days',CURRENT_TIMESTAMP - interval '390 days','https://cdn.streamsphere.dev/videos/vid-00001','https://cdn.streamsphere.dev/thumbs/vid-00001.jpg',15432,'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('vid-00002','Spring Boot Microservices Tutorial','Hands-on tutorial: Spring Boot Microservices — Duration: 45 minutes',CURRENT_TIMESTAMP - interval '380 days',CURRENT_TIMESTAMP - interval '379 days','https://cdn.streamsphere.dev/videos/vid-00002','https://cdn.streamsphere.dev/thumbs/vid-00002.jpg',234512,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00003','Building Resilient APIs with Spring','Resilience patterns and examples — Duration: 32 minutes',CURRENT_TIMESTAMP - interval '360 days',CURRENT_TIMESTAMP - interval '358 days','https://cdn.streamsphere.dev/videos/vid-00003','https://cdn.streamsphere.dev/thumbs/vid-00003.jpg',98321,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00004','Containerizing Java Apps with Docker','Full guide on containerizing Java apps — Duration: 22 minutes',CURRENT_TIMESTAMP - interval '350 days',CURRENT_TIMESTAMP - interval '349 days','https://cdn.streamsphere.dev/videos/vid-00004','https://cdn.streamsphere.dev/thumbs/vid-00004.jpg',76012,'dddddddd-dddd-dddd-dddd-dddddddddddd'),
('vid-00005','Kubernetes for Beginners','Intro to k8s — Duration: 28 minutes',CURRENT_TIMESTAMP - interval '340 days',CURRENT_TIMESTAMP - interval '338 days','https://cdn.streamsphere.dev/videos/vid-00005','https://cdn.streamsphere.dev/thumbs/vid-00005.jpg',210045,'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
('vid-00006','System Design: Scalable Architectures','Designing scalable systems — Duration: 55 minutes',CURRENT_TIMESTAMP - interval '330 days',CURRENT_TIMESTAMP - interval '325 days','https://cdn.streamsphere.dev/videos/vid-00006','https://cdn.streamsphere.dev/thumbs/vid-00006.jpg',502301,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00007','React Hooks Deep Dive','React hooks, best practices — Duration: 18 minutes',CURRENT_TIMESTAMP - interval '320 days',CURRENT_TIMESTAMP - interval '319 days','https://cdn.streamsphere.dev/videos/vid-00007','https://cdn.streamsphere.dev/thumbs/vid-00007.jpg',60210,'12121212-1212-1212-1212-121212121212'),
('vid-00008','Intro to Neural Networks (AI/ML)','Practical neural nets — Duration: 40 minutes',CURRENT_TIMESTAMP - interval '310 days',CURRENT_TIMESTAMP - interval '309 days','https://cdn.streamsphere.dev/videos/vid-00008','https://cdn.streamsphere.dev/thumbs/vid-00008.jpg',150321,'13131313-1313-1313-1313-131313131313'),
('vid-00009','DevOps Fundamentals: CI/CD','Pipelines, testing, deployment — Duration: 20 minutes',CURRENT_TIMESTAMP - interval '300 days',CURRENT_TIMESTAMP - interval '299 days','https://cdn.streamsphere.dev/videos/vid-00009','https://cdn.streamsphere.dev/thumbs/vid-00009.jpg',87210,'14141414-1414-1414-1414-141414141414'),
('vid-00010','Cracking the Coding Interview - Tips','Interview tips and practice problems — Duration: 30 minutes',CURRENT_TIMESTAMP - interval '290 days',CURRENT_TIMESTAMP - interval '288 days','https://cdn.streamsphere.dev/videos/vid-00010','https://cdn.streamsphere.dev/thumbs/vid-00010.jpg',430120,'15151515-1515-1515-1515-151515151515'),
('vid-00011','Docker Compose in Practice','Compose file patterns — Duration: 16 minutes',CURRENT_TIMESTAMP - interval '280 days',CURRENT_TIMESTAMP - interval '279 days','https://cdn.streamsphere.dev/videos/vid-00011','https://cdn.streamsphere.dev/thumbs/vid-00011.jpg',32123,'dddddddd-dddd-dddd-dddd-dddddddddddd'),
('vid-00012','Spring Security Essentials','Auth & JWT — Duration: 34 minutes',CURRENT_TIMESTAMP - interval '270 days',CURRENT_TIMESTAMP - interval '269 days','https://cdn.streamsphere.dev/videos/vid-00012','https://cdn.streamsphere.dev/thumbs/vid-00012.jpg',94211,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00013','Java Concurrency Patterns','Threads and executors — Duration: 29 minutes',CURRENT_TIMESTAMP - interval '260 days',CURRENT_TIMESTAMP - interval '258 days','https://cdn.streamsphere.dev/videos/vid-00013','https://cdn.streamsphere.dev/thumbs/vid-00013.jpg',67120,'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('vid-00014','Data Structures in Java - Arrays & Lists','Core data structures — Duration: 26 minutes',CURRENT_TIMESTAMP - interval '250 days',CURRENT_TIMESTAMP - interval '248 days','https://cdn.streamsphere.dev/videos/vid-00014','https://cdn.streamsphere.dev/thumbs/vid-00014.jpg',84120,'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('vid-00015','Hibernate Best Practices','ORM tips — Duration: 24 minutes',CURRENT_TIMESTAMP - interval '240 days',CURRENT_TIMESTAMP - interval '238 days','https://cdn.streamsphere.dev/videos/vid-00015','https://cdn.streamsphere.dev/thumbs/vid-00015.jpg',41230,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00016','Design Patterns Explained','Common patterns with examples — Duration: 38 minutes',CURRENT_TIMESTAMP - interval '230 days',CURRENT_TIMESTAMP - interval '228 days','https://cdn.streamsphere.dev/videos/vid-00016','https://cdn.streamsphere.dev/thumbs/vid-00016.jpg',122340,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00017','Unit Testing with JUnit and Mockito','Testing strategies — Duration: 21 minutes',CURRENT_TIMESTAMP - interval '220 days',CURRENT_TIMESTAMP - interval '219 days','https://cdn.streamsphere.dev/videos/vid-00017','https://cdn.streamsphere.dev/thumbs/vid-00017.jpg',55320,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00018','CI/CD Pipelines with GitHub Actions','Automation pipelines — Duration: 27 minutes',CURRENT_TIMESTAMP - interval '210 days',CURRENT_TIMESTAMP - interval '208 days','https://cdn.streamsphere.dev/videos/vid-00018','https://cdn.streamsphere.dev/thumbs/vid-00018.jpg',76540,'14141414-1414-1414-1414-141414141414'),
('vid-00019','Deploying to Cloud Providers','Deploying microservices — Duration: 36 minutes',CURRENT_TIMESTAMP - interval '200 days',CURRENT_TIMESTAMP - interval '198 days','https://cdn.streamsphere.dev/videos/vid-00019','https://cdn.streamsphere.dev/thumbs/vid-00019.jpg',98812,'14141414-1414-1414-1414-141414141414'),
('vid-00020','Performance Tuning Java Apps','Profiling and tuning — Duration: 42 minutes',CURRENT_TIMESTAMP - interval '190 days',CURRENT_TIMESTAMP - interval '188 days','https://cdn.streamsphere.dev/videos/vid-00020','https://cdn.streamsphere.dev/thumbs/vid-00020.jpg',402120,'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('vid-00021','Reactive Spring Boot Applications','Reactive programming — Duration: 33 minutes',CURRENT_TIMESTAMP - interval '180 days',CURRENT_TIMESTAMP - interval '178 days','https://cdn.streamsphere.dev/videos/vid-00021','https://cdn.streamsphere.dev/thumbs/vid-00021.jpg',120321,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00022','Advanced Microservices Patterns','Saga, CQRS — Duration: 40 minutes',CURRENT_TIMESTAMP - interval '170 days',CURRENT_TIMESTAMP - interval '168 days','https://cdn.streamsphere.dev/videos/vid-00022','https://cdn.streamsphere.dev/thumbs/vid-00022.jpg',220341,'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('vid-00023','Kubernetes Networking Deep Dive','Services, Ingress — Duration: 44 minutes',CURRENT_TIMESTAMP - interval '160 days',CURRENT_TIMESTAMP - interval '158 days','https://cdn.streamsphere.dev/videos/vid-00023','https://cdn.streamsphere.dev/thumbs/vid-00023.jpg',132012,'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
('vid-00024','Stateful vs Stateless Systems','Design tradeoffs — Duration: 18 minutes',CURRENT_TIMESTAMP - interval '150 days',CURRENT_TIMESTAMP - interval '148 days','https://cdn.streamsphere.dev/videos/vid-00024','https://cdn.streamsphere.dev/thumbs/vid-00024.jpg',91012,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00025','React Performance Optimization','Optimizations and profiling — Duration: 20 minutes',CURRENT_TIMESTAMP - interval '140 days',CURRENT_TIMESTAMP - interval '138 days','https://cdn.streamsphere.dev/videos/vid-00025','https://cdn.streamsphere.dev/thumbs/vid-00025.jpg',340120,'12121212-1212-1212-1212-121212121212'),
('vid-00026','Practical Machine Learning Workflows','Training to deployment — Duration: 50 minutes',CURRENT_TIMESTAMP - interval '130 days',CURRENT_TIMESTAMP - interval '128 days','https://cdn.streamsphere.dev/videos/vid-00026','https://cdn.streamsphere.dev/thumbs/vid-00026.jpg',201230,'13131313-1313-1313-1313-131313131313'),
('vid-00027','Docker Security Guidelines','Securing containers — Duration: 17 minutes',CURRENT_TIMESTAMP - interval '120 days',CURRENT_TIMESTAMP - interval '118 days','https://cdn.streamsphere.dev/videos/vid-00027','https://cdn.streamsphere.dev/thumbs/vid-00027.jpg',45120,'dddddddd-dddd-dddd-dddd-dddddddddddd'),
('vid-00028','Building a REST Gateway with Spring','API gateway patterns — Duration: 26 minutes',CURRENT_TIMESTAMP - interval '110 days',CURRENT_TIMESTAMP - interval '108 days','https://cdn.streamsphere.dev/videos/vid-00028','https://cdn.streamsphere.dev/thumbs/vid-00028.jpg',98210,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00029','System Design: Caching Strategies','Caching best practices — Duration: 19 minutes',CURRENT_TIMESTAMP - interval '100 days',CURRENT_TIMESTAMP - interval '98 days','https://cdn.streamsphere.dev/videos/vid-00029','https://cdn.streamsphere.dev/thumbs/vid-00029.jpg',53120,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00030','Interview Prep: System Design Questions','Mock interviews and examples — Duration: 60 minutes',CURRENT_TIMESTAMP - interval '90 days',CURRENT_TIMESTAMP - interval '88 days','https://cdn.streamsphere.dev/videos/vid-00030','https://cdn.streamsphere.dev/thumbs/vid-00030.jpg',110221,'15151515-1515-1515-1515-151515151515'),
('vid-00031','Spring Boot Actuator Monitoring','Monitoring and metrics — Duration: 22 minutes',CURRENT_TIMESTAMP - interval '80 days',CURRENT_TIMESTAMP - interval '78 days','https://cdn.streamsphere.dev/videos/vid-00031','https://cdn.streamsphere.dev/thumbs/vid-00031.jpg',72120,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00032','Event-Driven Microservices','Messaging and patterns — Duration: 37 minutes',CURRENT_TIMESTAMP - interval '70 days',CURRENT_TIMESTAMP - interval '68 days','https://cdn.streamsphere.dev/videos/vid-00032','https://cdn.streamsphere.dev/thumbs/vid-00032.jpg',231120,'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('vid-00033','GraphQL with Java and Spring','GraphQL APIs — Duration: 25 minutes',CURRENT_TIMESTAMP - interval '60 days',CURRENT_TIMESTAMP - interval '58 days','https://cdn.streamsphere.dev/videos/vid-00033','https://cdn.streamsphere.dev/thumbs/vid-00033.jpg',45120,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00034','API Versioning Strategies','Versioning best practices — Duration: 14 minutes',CURRENT_TIMESTAMP - interval '50 days',CURRENT_TIMESTAMP - interval '48 days','https://cdn.streamsphere.dev/videos/vid-00034','https://cdn.streamsphere.dev/thumbs/vid-00034.jpg',23120,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00035','Testing Microservices','Integration and contract testing — Duration: 30 minutes',CURRENT_TIMESTAMP - interval '45 days',CURRENT_TIMESTAMP - interval '44 days','https://cdn.streamsphere.dev/videos/vid-00035','https://cdn.streamsphere.dev/thumbs/vid-00035.jpg',129120,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00036','Distributed Tracing with OpenTelemetry','Tracing microservices — Duration: 28 minutes',CURRENT_TIMESTAMP - interval '40 days',CURRENT_TIMESTAMP - interval '39 days','https://cdn.streamsphere.dev/videos/vid-00036','https://cdn.streamsphere.dev/thumbs/vid-00036.jpg',65210,'14141414-1414-1414-1414-141414141414'),
('vid-00037','Design Patterns: Singleton to Factory','Patterns walk-through — Duration: 35 minutes',CURRENT_TIMESTAMP - interval '35 days',CURRENT_TIMESTAMP - interval '34 days','https://cdn.streamsphere.dev/videos/vid-00037','https://cdn.streamsphere.dev/thumbs/vid-00037.jpg',87212,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00038','React + TypeScript Crash Course','React + TS — Duration: 29 minutes',CURRENT_TIMESTAMP - interval '30 days',CURRENT_TIMESTAMP - interval '28 days','https://cdn.streamsphere.dev/videos/vid-00038','https://cdn.streamsphere.dev/thumbs/vid-00038.jpg',99120,'12121212-1212-1212-1212-121212121212'),
('vid-00039','ML Model Deployment Best Practices','Serving ML models — Duration: 34 minutes',CURRENT_TIMESTAMP - interval '25 days',CURRENT_TIMESTAMP - interval '24 days','https://cdn.streamsphere.dev/videos/vid-00039','https://cdn.streamsphere.dev/thumbs/vid-00039.jpg',43020,'13131313-1313-1313-1313-131313131313'),
('vid-00040','DevSecOps: Security in CI','Security checks in pipelines — Duration: 23 minutes',CURRENT_TIMESTAMP - interval '20 days',CURRENT_TIMESTAMP - interval '19 days','https://cdn.streamsphere.dev/videos/vid-00040','https://cdn.streamsphere.dev/thumbs/vid-00040.jpg',72100,'14141414-1414-1414-1414-141414141414'),
('vid-00041','Performance Profiling Java','Profiling and hotspots — Duration: 31 minutes',CURRENT_TIMESTAMP - interval '18 days',CURRENT_TIMESTAMP - interval '17 days','https://cdn.streamsphere.dev/videos/vid-00041','https://cdn.streamsphere.dev/thumbs/vid-00041.jpg',211220,'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('vid-00042','Building a Video Upload Service','Upload flows and storage — Duration: 26 minutes',CURRENT_TIMESTAMP - interval '15 days',CURRENT_TIMESTAMP - interval '14 days','https://cdn.streamsphere.dev/videos/vid-00042','https://cdn.streamsphere.dev/thumbs/vid-00042.jpg',11230,'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('vid-00043','Kubernetes Operators Intro','Operators basics — Duration: 42 minutes',CURRENT_TIMESTAMP - interval '12 days',CURRENT_TIMESTAMP - interval '11 days','https://cdn.streamsphere.dev/videos/vid-00043','https://cdn.streamsphere.dev/thumbs/vid-00043.jpg',44212,'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
('vid-00044','End-to-End Microservices Example','Full demo project — Duration: 65 minutes',CURRENT_TIMESTAMP - interval '10 days',CURRENT_TIMESTAMP - interval '8 days','https://cdn.streamsphere.dev/videos/vid-00044','https://cdn.streamsphere.dev/thumbs/vid-00044.jpg',502321,'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('vid-00045','Spring Cloud Gateway Patterns','Gateway and routing — Duration: 19 minutes',CURRENT_TIMESTAMP - interval '7 days',CURRENT_TIMESTAMP - interval '6 days','https://cdn.streamsphere.dev/videos/vid-00045','https://cdn.streamsphere.dev/thumbs/vid-00045.jpg',33221,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('vid-00046','Container Orchestration Essentials','Cloud native orchestration — Duration: 27 minutes',CURRENT_TIMESTAMP - interval '6 days',CURRENT_TIMESTAMP - interval '5 days','https://cdn.streamsphere.dev/videos/vid-00046','https://cdn.streamsphere.dev/thumbs/vid-00046.jpg',41212,'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'),
('vid-00047','Scaling Databases for High Traffic','DB sharding and scaling — Duration: 48 minutes',CURRENT_TIMESTAMP - interval '5 days',CURRENT_TIMESTAMP - interval '3 days','https://cdn.streamsphere.dev/videos/vid-00047','https://cdn.streamsphere.dev/thumbs/vid-00047.jpg',221430,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00048','System Design: Consistency Models','CAP and beyond — Duration: 21 minutes',CURRENT_TIMESTAMP - interval '4 days',CURRENT_TIMESTAMP - interval '3 days','https://cdn.streamsphere.dev/videos/vid-00048','https://cdn.streamsphere.dev/thumbs/vid-00048.jpg',11223,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00049','Designing for High Availability','High availability patterns — Duration: 35 minutes',CURRENT_TIMESTAMP - interval '3 days',CURRENT_TIMESTAMP - interval '2 days','https://cdn.streamsphere.dev/videos/vid-00049','https://cdn.streamsphere.dev/thumbs/vid-00049.jpg',90221,'ffffffff-ffff-ffff-ffff-ffffffffffff'),
('vid-00050','Advanced Java Memory Management','GC and tuning — Duration: 29 minutes',CURRENT_TIMESTAMP - interval '1 days',CURRENT_TIMESTAMP,'https://cdn.streamsphere.dev/videos/vid-00050','https://cdn.streamsphere.dev/thumbs/vid-00050.jpg',162120,'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

-- 15 Playlists
INSERT INTO playlists (id, name, channel_id) VALUES
('30000000-0000-0000-0000-000000000001','Top Java Tutorials','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('30000000-0000-0000-0000-000000000002','Spring Boot Essentials','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('30000000-0000-0000-0000-000000000003','Microservices Playbook','cccccccc-cccc-cccc-cccc-cccccccccccc'),
('30000000-0000-0000-0000-000000000004','Docker & Kubernetes','dddddddd-dddd-dddd-dddd-dddddddddddd'),
('30000000-0000-0000-0000-000000000005','System Design Interviews','ffffffff-ffff-ffff-ffff-ffffffffffff'),
('30000000-0000-0000-0000-000000000006','React Tutorials','12121212-1212-1212-1212-121212121212'),
('30000000-0000-0000-0000-000000000007','AI/ML Projects','13131313-1313-1313-1313-131313131313'),
('30000000-0000-0000-0000-000000000008','DevOps & CI/CD','14141414-1414-1414-1414-141414141414'),
('30000000-0000-0000-0000-000000000009','Interview Warmups','15151515-1515-1515-1515-151515151515'),
('30000000-0000-0000-0000-000000000010','Performance Tuning','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('30000000-0000-0000-0000-000000000011','Hibernate & JPA','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('30000000-0000-0000-0000-000000000012','Testing Patterns','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('30000000-0000-0000-0000-000000000013','Cloud Deployments','14141414-1414-1414-1414-141414141414'),
('30000000-0000-0000-0000-000000000014','Design Patterns','ffffffff-ffff-ffff-ffff-ffffffffffff'),
('30000000-0000-0000-0000-000000000015','Fullstack Projects','12121212-1212-1212-1212-121212121212');

-- Playlist to Video mappings (example: assign first N videos to playlists)
INSERT INTO playlists_videos (playlist_id, video_id) VALUES
('30000000-0000-0000-0000-000000000001','vid-00001'),
('30000000-0000-0000-0000-000000000001','vid-00013'),
('30000000-0000-0000-0000-000000000001','vid-00014'),
('30000000-0000-0000-0000-000000000002','vid-00002'),
('30000000-0000-0000-0000-000000000002','vid-00012'),
('30000000-0000-0000-0000-000000000003','vid-00022'),
('30000000-0000-0000-0000-000000000003','vid-00032'),
('30000000-0000-0000-0000-000000000004','vid-00004'),
('30000000-0000-0000-0000-000000000004','vid-00011'),
('30000000-0000-0000-0000-000000000005','vid-00006'),
('30000000-0000-0000-0000-000000000005','vid-00030'),
('30000000-0000-0000-0000-000000000006','vid-00007'),
('30000000-0000-0000-0000-000000000006','vid-00025'),
('30000000-0000-0000-0000-000000000007','vid-00008'),
('30000000-0000-0000-0000-000000000007','vid-00026');

-- Video to Tag mappings (many-to-many)
INSERT INTO videos_tags (video_id, tag_id) VALUES
('vid-00001','20000000-0000-0000-0000-000000000001'),
('vid-00001','20000000-0000-0000-0000-000000000014'),
('vid-00002','20000000-0000-0000-0000-000000000002'),
('vid-00002','20000000-0000-0000-0000-000000000003'),
('vid-00003','20000000-0000-0000-0000-000000000002'),
('vid-00003','20000000-0000-0000-0000-000000000016'),
('vid-00004','20000000-0000-0000-0000-000000000004'),
('vid-00005','20000000-0000-0000-0000-000000000005'),
('vid-00006','20000000-0000-0000-0000-000000000006'),
('vid-00007','20000000-0000-0000-0000-000000000007'),
('vid-00008','20000000-0000-0000-0000-000000000008'),
('vid-00009','20000000-0000-0000-0000-000000000009'),
('vid-00010','20000000-0000-0000-0000-000000000010'),
('vid-00011','20000000-0000-0000-0000-000000000011'),
('vid-00012','20000000-0000-0000-0000-000000000012'),
('vid-00013','20000000-0000-0000-0000-000000000013'),
('vid-00014','20000000-0000-0000-0000-000000000014'),
('vid-00015','20000000-0000-0000-0000-000000000015'),
('vid-00016','20000000-0000-0000-0000-000000000016'),
('vid-00017','20000000-0000-0000-0000-000000000017'),
('vid-00018','20000000-0000-0000-0000-000000000018'),
('vid-00019','20000000-0000-0000-0000-000000000019'),
('vid-00020','20000000-0000-0000-0000-000000000020'),
('vid-00021','20000000-0000-0000-0000-000000000002'),
('vid-00022','20000000-0000-0000-0000-000000000003'),
('vid-00023','20000000-0000-0000-0000-000000000005'),
('vid-00024','20000000-0000-0000-0000-000000000006'),
('vid-00025','20000000-0000-0000-0000-000000000007'),
('vid-00026','20000000-0000-0000-0000-000000000008'),
('vid-00027','20000000-0000-0000-0000-000000000004'),
('vid-00028','20000000-0000-0000-0000-000000000002'),
('vid-00029','20000000-0000-0000-0000-000000000006'),
('vid-00030','20000000-0000-0000-0000-000000000010'),
('vid-00031','20000000-0000-0000-0000-000000000012'),
('vid-00032','20000000-0000-0000-0000-000000000003'),
('vid-00033','20000000-0000-0000-0000-000000000016'),
('vid-00034','20000000-0000-0000-0000-000000000016'),
('vid-00035','20000000-0000-0000-0000-000000000017'),
('vid-00036','20000000-0000-0000-0000-000000000018'),
('vid-00037','20000000-0000-0000-0000-000000000016'),
('vid-00038','20000000-0000-0000-0000-000000000007'),
('vid-00039','20000000-0000-0000-0000-000000000008'),
('vid-00040','20000000-0000-0000-0000-000000000018'),
('vid-00041','20000000-0000-0000-0000-000000000020'),
('vid-00042','20000000-0000-0000-0000-000000000003'),
('vid-00043','20000000-0000-0000-0000-000000000005'),
('vid-00044','20000000-0000-0000-0000-000000000003'),
('vid-00045','20000000-0000-0000-0000-000000000002'),
('vid-00046','20000000-0000-0000-0000-000000000005'),
('vid-00047','20000000-0000-0000-0000-000000000019'),
('vid-00048','20000000-0000-0000-0000-000000000006'),
('vid-00049','20000000-0000-0000-0000-000000000006'),
('vid-00050','20000000-0000-0000-0000-000000000020');

cd d:\springboot-project2\microservices\StreamSphereApplication\StreamSphereBackend\central
set APP_DATA_SEED=true
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local```bash
```bash
cd StreamSphereBackend/central
APP_DATA_SEED=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=localSELECT COUNT(*) FROM videos;
SELECT COUNT(*) FROM tags;
SELECT COUNT(*) FROM playlists;
SELECT COUNT(*) FROM user_watch_history;
SELECT COUNT(*) FROM user_tag_history;
SELECT c.name, COUNT(cu.users_id) AS subscribers
  FROM channels c JOIN channels_users cu ON cu.channels_id = c.id
 GROUP BY c.id, c.name ORDER BY subscribers DESC LIMIT 10;-- Channel subscribers (join table: channels_users)
INSERT INTO channels_users (channels_id, users_id) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','12121212-1212-1212-1212-121212121212'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','13131313-1313-1313-1313-131313131313'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','11111111-1111-1111-1111-111111111111'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','15151515-1515-1515-1515-151515151515'),
('cccccccc-cccc-cccc-cccc-cccccccccccc','16161616-1616-1616-1616-161616161616'),
('cccccccc-cccc-cccc-cccc-cccccccccccc','17171717-1717-1717-1717-171717171717'),
('dddddddd-dddd-dddd-dddd-dddddddddddd','12121212-1212-1212-1212-121212121212'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee','18181818-1818-1818-1818-181818181818'),
('ffffffff-ffff-ffff-ffff-ffffffffffff','19191919-1919-1919-1919-191919191919'),
('12121212-1212-1212-1212-121212121212','20202020-2020-2020-2020-202020202020');

-- User watch history (sample)
INSERT INTO user_watch_history (user_id, video_id, count, is_liked, last_watched) VALUES
('11111111-1111-1111-1111-111111111111','vid-00001',3,true,CURRENT_TIMESTAMP - interval '30 days'),
('11111111-1111-1111-1111-111111111111','vid-00013',1,false,CURRENT_TIMESTAMP - interval '10 days'),
('22222222-2222-2222-2222-222222222222','vid-00002',12,true,CURRENT_TIMESTAMP - interval '40 days'),
('33333333-3333-3333-3333-333333333333','vid-00006',5,false,CURRENT_TIMESTAMP - interval '5 days'),
('44444444-4444-4444-4444-444444444444','vid-00004',2,true,CURRENT_TIMESTAMP - interval '3 days');

-- User tag history (sample)
INSERT INTO user_tag_history (user_id, tag_id, count, last_watched) VALUES
('11111111-1111-1111-1111-111111111111','20000000-0000-0000-0000-000000000001',4,CURRENT_TIMESTAMP - interval '30 days'),
('22222222-2222-2222-2222-222222222222','20000000-0000-0000-0000-000000000002',8,CURRENT_TIMESTAMP - interval '40 days'),
('33333333-3333-3333-3333-333333333333','20000000-0000-0000-0000-000000000005',2,CURRENT_TIMESTAMP - interval '5 days');

-- Verification queries (run after seeding)
-- SELECT count(*) FROM users;
-- SELECT count(*) FROM channels;
-- SELECT count(*) FROM videos;
-- SELECT count(*) FROM tags;
-- SELECT c.name, c.total_views, c.total_subs FROM channels c ORDER BY c.total_views DESC LIMIT 10;
