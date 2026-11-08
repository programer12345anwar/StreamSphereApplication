package com.streamsphere.central.controller;

import com.streamsphere.central.dto.response.CommentResponseDTO;
import com.streamsphere.central.dto.response.ReportSubmissionResponseDTO;
import com.streamsphere.central.dto.response.CommentLikeResponseDTO;
import com.streamsphere.central.dto.response.VideoEngagementResponseDTO;
import com.streamsphere.central.service.EngagementService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/central/engagement")
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @PostMapping("/video/{videoId}/like")
    public ResponseEntity<Void> toggleLike(@RequestParam UUID userId,
                                           @PathVariable String videoId,
                                           @RequestParam boolean isLike,
                                           Principal principal) {
        UUID authenticatedUserId = engagementService.getAuthenticatedUserId(principal.getName(), userId);
        engagementService.toggleLike(authenticatedUserId, videoId, isLike);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/video/{videoId}/engagement")
    public ResponseEntity<VideoEngagementResponseDTO> getVideoEngagement(
            @PathVariable String videoId,
            Principal principal) {
        return ResponseEntity.ok(engagementService.getVideoEngagement(
                videoId, principal == null ? null : principal.getName()));
    }

    @PostMapping("/video/{videoId}/comment")
    public ResponseEntity<CommentResponseDTO> addComment(@RequestParam UUID userId,
                                              @PathVariable String videoId,
                                              @RequestBody Map<String, String> payload,
                                              Principal principal) {
        UUID authenticatedUserId = engagementService.getAuthenticatedUserId(principal.getName(), userId);
        String text = payload.get("text");
        String parentIdStr = payload.get("parentId");
        UUID parentId = parentIdStr != null ? UUID.fromString(parentIdStr) : null;
        
        CommentResponseDTO comment = engagementService.addComment(authenticatedUserId, videoId, text, parentId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/video/{videoId}/comments")
    public ResponseEntity<Page<CommentResponseDTO>> getVideoComments(@PathVariable String videoId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          Principal principal) {
        return ResponseEntity.ok(engagementService.getVideoComments(
                videoId, page, size, principal == null ? null : principal.getName()));
    }

    @GetMapping("/comment/{parentId}/replies")
    public ResponseEntity<Page<CommentResponseDTO>> getCommentReplies(@PathVariable UUID parentId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           Principal principal) {
        return ResponseEntity.ok(engagementService.getCommentReplies(
                parentId, page, size, principal == null ? null : principal.getName()));
    }

    @PostMapping("/comment/{commentId}/like")
    public ResponseEntity<CommentLikeResponseDTO> toggleCommentLike(
            @PathVariable UUID commentId,
            @RequestParam(required = false) UUID userId,
            Principal principal) {
        UUID authenticatedUserId = engagementService.getAuthenticatedUserId(principal.getName(), userId);
        return ResponseEntity.ok(engagementService.toggleCommentLike(authenticatedUserId, commentId));
    }

    @PostMapping("/video/{videoId}/view")
    public ResponseEntity<Void> recordView(@PathVariable String videoId, HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        engagementService.incrementViewCount(videoId, clientIp);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/video/{videoId}/report")
    public ResponseEntity<ReportSubmissionResponseDTO> reportVideo(@PathVariable String videoId,
                                                                   @RequestBody Map<String, String> payload,
                                                                   Principal principal) {
        ReportSubmissionResponseDTO report = engagementService.reportVideo(
                principal.getName(),
                videoId,
                payload.get("reason"),
                payload.get("description"));
        return ResponseEntity.ok(report);
    }
}
