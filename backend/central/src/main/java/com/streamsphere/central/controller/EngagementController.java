package com.streamsphere.central.controller;

import com.streamsphere.central.entity.Comment;
import com.streamsphere.central.service.EngagementService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

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
                                           @RequestParam boolean isLike) {
        engagementService.toggleLike(userId, videoId, isLike);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/video/{videoId}/comment")
    public ResponseEntity<Comment> addComment(@RequestParam UUID userId,
                                              @PathVariable String videoId,
                                              @RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        String parentIdStr = payload.get("parentId");
        UUID parentId = parentIdStr != null ? UUID.fromString(parentIdStr) : null;
        
        Comment comment = engagementService.addComment(userId, videoId, text, parentId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/video/{videoId}/comments")
    public ResponseEntity<Page<Comment>> getVideoComments(@PathVariable String videoId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(engagementService.getVideoComments(videoId, page, size));
    }

    @GetMapping("/comment/{parentId}/replies")
    public ResponseEntity<Page<Comment>> getCommentReplies(@PathVariable UUID parentId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(engagementService.getCommentReplies(parentId, page, size));
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
    public void reportVideo(@PathVariable(name="videoId") String videoId,
                            @RequestParam(name="userId") UUID userId,
                            @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        engagementService.reportVideo(userId, videoId, reason);
    }
}
