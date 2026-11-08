package com.streamsphere.central.service;

import com.streamsphere.central.dto.response.ReportSubmissionResponseDTO;
import com.streamsphere.central.entity.AppUser;
import com.streamsphere.central.entity.Video;
import com.streamsphere.central.entity.VideoReport;
import com.streamsphere.central.event.producer.RabbitMqService;
import com.streamsphere.central.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EngagementReportTest {
    private final VideoLikeRepo videoLikes = mock(VideoLikeRepo.class);
    private final CommentRepo comments = mock(CommentRepo.class);
    private final VideoRepo videos = mock(VideoRepo.class);
    private final AppUserRepo users = mock(AppUserRepo.class);
    private final VideoReportRepo reports = mock(VideoReportRepo.class);

    private EngagementService service() {
        return new EngagementService(
                videoLikes,
                comments,
                videos,
                users,
                mock(RabbitMqService.class),
                mock(StringRedisTemplate.class),
                reports,
                mock(NotificationService.class),
                mock(CommentLikeRepo.class));
    }

    @Test
    void reportsAreSavedForTheAuthenticatedUserAndReturnThePersistedId() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("reporter@example.com");
        Video video = new Video();
        video.setId("reported-video");
        when(users.findByEmail("reporter@example.com")).thenReturn(user);
        when(videos.findById("reported-video")).thenReturn(Optional.of(video));
        when(reports.saveAndFlush(any(VideoReport.class))).thenAnswer(invocation -> {
            VideoReport report = invocation.getArgument(0);
            report.setId(41);
            return report;
        });

        ReportSubmissionResponseDTO response = service().reportVideo(
                "reporter@example.com", "reported-video", "Spam or misleading", "Repeated links");

        assertEquals(41, response.id());
        assertEquals("PENDING", response.status());
        verify(reports).saveAndFlush(argThat(report ->
                report.getUser() == user
                        && report.getVideo() == video
                        && "Spam or misleading".equals(report.getReason())
                        && "Repeated links".equals(report.getDescription())
                        && "PENDING".equals(report.getStatus())));
    }

    @Test
    void rejectsBlankReasonsAndUnknownVideosWithoutPersisting() {
        when(users.findByEmail("reporter@example.com")).thenReturn(new AppUser());
        EngagementService service = service();

        assertThrows(IllegalArgumentException.class,
                () -> service.reportVideo("reporter@example.com", "video-1", "  ", null));
        when(videos.findById("missing-video")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.reportVideo("reporter@example.com", "missing-video", "Spam", null));
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void rejectsClientSuppliedIdentityThatDoesNotMatchTheAuthenticatedAccount() {
        UUID authenticatedUserId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(authenticatedUserId);
        when(users.findByEmail("reporter@example.com")).thenReturn(user);

        assertEquals(authenticatedUserId,
                service().getAuthenticatedUserId("reporter@example.com", authenticatedUserId));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service().getAuthenticatedUserId("reporter@example.com", UUID.randomUUID()));
    }
}
