package com.streamsphere.central.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.streamsphere.central.dto.response.AdminReportDTO;
import com.streamsphere.central.entity.VideoReport;
import com.streamsphere.central.repository.AppUserRepo;
import com.streamsphere.central.repository.ChannelRepo;
import com.streamsphere.central.repository.VideoRepo;
import com.streamsphere.central.repository.VideoReportRepo;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class AdminReportsServiceTest {

    @Test
    void includesLegacyReportsWithMissingReporterOrVideoInsteadOfFailingThePage() {
        VideoReport legacyReport = new VideoReport();
        legacyReport.setId(12);
        legacyReport.setReason("Spam");
        legacyReport.setStatus("PENDING");
        legacyReport.setCreatedAt(LocalDateTime.now());

        VideoReportRepo reports = mock(VideoReportRepo.class);
        when(reports.searchReports(isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(legacyReport)));

        AdminService service = new AdminService(
                mock(AppUserRepo.class),
                mock(VideoRepo.class),
                mock(ChannelRepo.class),
                reports);

        AdminReportDTO result = service.getReports(0, 10, null, null).getContent().get(0);

        assertEquals(12, result.getId());
        assertEquals("Spam", result.getReason());
        assertNull(result.getVideoId());
        assertNull(result.getReporterName());
        assertNull(result.getReporterEmail());
    }
}
