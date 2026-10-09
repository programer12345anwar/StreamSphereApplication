package com.streamsphere.central.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportDTO {
    private int id;
    private String videoId;
    private String videoName;
    private String reporterEmail;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
