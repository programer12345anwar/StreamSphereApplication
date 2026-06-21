package com.youtube.central.config;

import com.youtube.central.models.AppUser;
import com.youtube.central.models.Channel;
import com.youtube.central.models.PlayList;
import com.youtube.central.models.Tag;
import com.youtube.central.models.UserTagHistory;
import com.youtube.central.models.UserWatchHistory;
import com.youtube.central.models.Video;
import com.youtube.central.repository.AppUserRepo;
import com.youtube.central.repository.ChannelRepo;
import com.youtube.central.repository.PlayListRepo;
import com.youtube.central.repository.TagRepo;
import com.youtube.central.repository.UserTagHistoryRepo;
import com.youtube.central.repository.UserWatchHistoryRepo;
import com.youtube.central.repository.VideoRepo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private AppUserRepo appUserRepo;

    @Autowired
    private ChannelRepo channelRepo;

    @Autowired
    private VideoRepo videoRepo;

    @Autowired
    private TagRepo tagRepo;

    @Autowired
    private PlayListRepo playListRepo;

    @Autowired
    private UserWatchHistoryRepo userWatchHistoryRepo;

    @Autowired
    private UserTagHistoryRepo userTagHistoryRepo;

    @Autowired
    private Environment env;

    private final Random random = new Random(42);

    @Override
    public void run(String... args) throws Exception {
        // Only seed when explicitly enabled or running with the 'local' profile
        boolean seedEnabled = Boolean.parseBoolean(env.getProperty("app.data.seed", "false"));
        boolean localProfile = Arrays.asList(env.getActiveProfiles()).contains("local");
        if (!seedEnabled && !localProfile) {
            log.info("Data seeding disabled (set app.data.seed=true or use 'local' profile to enable)");
            return;
        }

        // idempotent: skip if videos already present
        if (videoRepo.count() > 0) {
            log.info("Seed data present, skipping DataSeeder");
            return;
        }

        log.info("Seeding initial data for StreamSphere central service...");

        // 1) Tags
        List<String> tagNames = Arrays.asList(
                "Java Programming", "Spring Boot", "Microservices", "Docker", "Kubernetes",
                "System Design", "React", "AI/ML", "DevOps", "Interview Preparation",
                "Docker Compose", "Spring Security", "Concurrency", "Data Structures", "Hibernate",
                "Design Patterns", "Testing", "CI/CD", "Cloud", "Performance Tuning"
        );

        List<Tag> tags = tagNames.stream().map(name -> {
            Tag t = new Tag();
            t.setName(name);
            return t;
        }).collect(Collectors.toList());
        tagRepo.saveAll(tags);

        // 2) Users
        List<String> userNames = Arrays.asList(
                "Alex Johnson","Emily Davis","Michael Smith","Sarah Lee","David Brown",
                "Laura Wilson","James Miller","Olivia Taylor","Daniel Anderson","Sophia Thomas",
                "Ethan Martinez","Ava Robinson","William Clark","Isabella Walker","Joseph Hall",
                "Mia Allen","Liam Young","Charlotte Hernandez","Noah King","Amelia Wright"
        );

        List<AppUser> users = new ArrayList<>();
        for (int i = 0; i < userNames.size(); i++) {
            AppUser u = new AppUser();
            u.setName(userNames.get(i));
            u.setEmail(userNames.get(i).toLowerCase().replace(" ", ".") + "@example.com");
            u.setPassword("password");
            u.setPhoneNumber(9000000000L + i + 1);
            u.setDob(LocalDate.now().minusYears(20 + (i % 15)).minusDays(i * 7));
            u.setGender((i % 2 == 0) ? "Male" : "Female");
            u.setCountry("USA");
            LocalDateTime created = LocalDateTime.now().minusDays(random.nextInt(400));
            u.setCreatedAt(created);
            u.setUpdatedAt(created.plusDays(random.nextInt(30)));
            users.add(u);
        }
        appUserRepo.saveAll(users);

        // 3) Channels (10)
        List<String> channelNames = Arrays.asList(
                "CodeCrafters","SpringInsights","MicroserviceMasters","DockerDive","KubeCast",
                "SystemDesignPro","ReactRocket","AIMinds","DevOpsDaily","InterviewAce"
        );

        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < channelNames.size(); i++) {
            Channel c = new Channel();
            c.setName(channelNames.get(i));
            // owner is first 10 users
            c.setUser(users.get(i));
            c.setDescription("Official channel for " + channelNames.get(i));
            c.setWatchHours(0.0);
            c.setMonetized(i % 3 == 0);
            c.setTotalViews(0);
            c.setTotalLikeCount(0);
            c.setTotalSubs(0);
            c.setSubscribers(new ArrayList<>());
            c.setVideos(new ArrayList<>());
            c.setPlayLists(new ArrayList<>());
            LocalDateTime created = LocalDateTime.now().minusDays(200 + random.nextInt(400));
            c.setCreatedAt(created);
            c.setUpdatedAt(created.plusDays(random.nextInt(100)));
            channels.add(c);
        }
        channelRepo.saveAll(channels);

        // 4) Videos (50)
        List<String> videoTitles = generateVideoTitles();
        List<Video> videos = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Video v = new Video();
            String vid = "vid-" + String.format("%05d", i + 1);
            v.setId(vid);
            v.setName(videoTitles.get(i % videoTitles.size()));
            v.setDescription("Hands-on tutorial: " + v.getName() + " — covers examples and best practices. Duration: " + (5 + random.nextInt(55)) + " minutes.");
            LocalDateTime uploaded = LocalDateTime.now().minusDays(random.nextInt(720));
            v.setUploadDateTime(uploaded);
            v.setUpdatedAt(uploaded.plusDays(random.nextInt(30)));
            v.setVideoLink("https://cdn.streamsphere.dev/videos/" + vid);
            v.setThumbnailLink("https://cdn.streamsphere.dev/thumbs/" + vid + ".jpg");
            v.setViews(100 + random.nextInt(1000000));
            // distribute videos across channels
            Channel owner = channels.get(random.nextInt(channels.size()));
            v.setChannel(owner);
            // assign 1-4 tags
            Collections.shuffle(tags, random);
            int tagCount = 1 + random.nextInt(Math.min(4, tags.size()));
            List<Tag> videoTags = new ArrayList<>();
            for (int t = 0; t < tagCount; t++) videoTags.add(tags.get(t));
            v.setTags(videoTags);
            videos.add(v);
        }
        videoRepo.saveAll(videos);

        // update channel video lists and totalViews
        for (Video v : videos) {
            Channel c = v.getChannel();
            if (c.getVideos() == null) c.setVideos(new ArrayList<>());
            c.getVideos().add(v);
            c.setTotalViews(c.getTotalViews() + v.getViews());
        }
        channelRepo.saveAll(channels);

        // 5) Playlists (15)
        List<PlayList> playLists = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            PlayList p = new PlayList();
            p.setName((i % 2 == 0 ? "Top Tutorials " : "Deep Dive ") + (i + 1));
            Channel c = channels.get(random.nextInt(channels.size()));
            p.setChannel(c);
            // pick 3-8 videos
            Collections.shuffle(videos, random);
            int vcount = 3 + random.nextInt(6);
            p.setVideos(new ArrayList<>(videos.subList(0, Math.min(vcount, videos.size()))));
            playLists.add(p);
        }
        playListRepo.saveAll(playLists);

        // update channel playlists
        for (PlayList p : playLists) {
            Channel c = p.getChannel();
            if (c.getPlayLists() == null) c.setPlayLists(new ArrayList<>());
            c.getPlayLists().add(p);
        }
        channelRepo.saveAll(channels);

        // 6) Subscribers: assign random subscribers to channels
        for (Channel c : channels) {
            int subs = 5 + random.nextInt(30);
            List<AppUser> possible = new ArrayList<>(users);
            Collections.shuffle(possible, random);
            List<AppUser> chosen = possible.subList(0, Math.min(subs, possible.size()));
            c.setSubscribers(new ArrayList<>(chosen));
            c.setTotalSubs(c.getSubscribers().size());
        }
        channelRepo.saveAll(channels);

        // 7) UserWatchHistory: add records for users
        List<UserWatchHistory> watchHistory = new ArrayList<>();
        for (AppUser u : users) {
            int records = 3 + random.nextInt(10);
            Collections.shuffle(videos, random);
            for (int r = 0; r < records; r++) {
                Video vv = videos.get(r % videos.size());
                UserWatchHistory wh = new UserWatchHistory();
                wh.setUserId(u.getId());
                wh.setVideoId(vv.getId());
                wh.setCount(1 + random.nextInt(50));
                wh.setLiked(random.nextBoolean());
                wh.setLastWatched(LocalDateTime.now().minusDays(random.nextInt(365)));
                watchHistory.add(wh);
            }
        }
        userWatchHistoryRepo.saveAll(watchHistory);

        // 8) UserTagHistory: summarize tag interest per user
        List<UserTagHistory> tagHistories = new ArrayList<>();
        for (AppUser u : users) {
            // pick 3 tags per user
            Collections.shuffle(tags, random);
            for (int t = 0; t < 3; t++) {
                UserTagHistory uth = new UserTagHistory();
                uth.setUserId(u.getId());
                uth.setTagId(tags.get(t).getId());
                uth.setCount(1 + random.nextInt(30));
                uth.setLastWatched(LocalDateTime.now().minusDays(random.nextInt(365)));
                tagHistories.add(uth);
            }
        }
        userTagHistoryRepo.saveAll(tagHistories);

        log.info("Seeding complete: {} users, {} channels, {} tags, {} videos, {} playlists",
                users.size(), channels.size(), tags.size(), videos.size(), playLists.size());
    }

    private List<String> generateVideoTitles() {
        return Arrays.asList(
                "Intro to Java Streams",
                "Spring Boot Microservices Tutorial",
                "Building Resilient APIs with Spring",
                "Containerizing Java Apps with Docker",
                "Kubernetes for Beginners",
                "System Design: Scalable Architectures",
                "React Hooks Deep Dive",
                "Intro to Neural Networks (AI/ML)",
                "DevOps Fundamentals: CI/CD",
                "Cracking the Coding Interview - Tips",
                "Docker Compose in Practice",
                "Spring Security Essentials",
                "Java Concurrency Patterns",
                "Data Structures in Java - Arrays & Lists",
                "Hibernate Best Practices",
                "Design Patterns Explained",
                "Unit Testing with JUnit and Mockito",
                "CI/CD Pipelines with GitHub Actions",
                "Deploying to Cloud Providers",
                "Performance Tuning Java Apps",
                "Reactive Spring Boot Applications",
                "Advanced Microservices Patterns",
                "Kubernetes Networking Deep Dive",
                "Stateful vs Stateless Systems",
                "React Performance Optimization",
                "Practical Machine Learning Workflows",
                "Docker Security Guidelines",
                "Building a REST Gateway with Spring",
                "System Design: Caching Strategies",
                "Interview Prep: System Design Questions",
                "Spring Boot Actuator Monitoring",
                "Event-Driven Microservices",
                "GraphQL with Java and Spring",
                "API Versioning Strategies",
                "Testing Microservices",
                "Distributed Tracing with OpenTelemetry",
                "Design Patterns: Singleton to Factory",
                "React + TypeScript Crash Course",
                "ML Model Deployment Best Practices",
                "DevSecOps: Security in CI",
                "Performance Profiling Java",
                "Building a Video Upload Service",
                "Kubernetes Operators Intro",
                "End-to-End Microservices Example",
                "Spring Cloud Gateway Patterns",
                "Container Orchestration Essentials",
                "Scaling Databases for High Traffic",
                "System Design: Consistency Models",
                "Designing for High Availability"
        );
    }
}
