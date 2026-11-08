package com.streamsphere.central.dto.response;

import java.time.LocalDateTime;

public record ReportSubmissionResponseDTO(int id, String status, LocalDateTime createdAt) {}
