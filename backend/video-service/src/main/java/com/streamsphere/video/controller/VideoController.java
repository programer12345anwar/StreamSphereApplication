package com.streamsphere.video.controller;

import java.util.UUID;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.streamsphere.video.dto.response.GeneralMessage;
import com.streamsphere.video.dto.response.VideoDetail;
import com.streamsphere.video.dto.response.ImageKitUploadAuthorization;
import com.streamsphere.video.dto.request.ImageKitUploadCompletionRequest;
import com.streamsphere.video.service.UploadService;
import com.streamsphere.video.service.VideoStorageService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/video")
@Slf4j
public class VideoController {

    @Autowired
    UploadService uploadService;
    
    @Autowired
    VideoStorageService videoStorageService;

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<GeneralMessage> rejectLegacyLocalUpload() {
        GeneralMessage message = new GeneralMessage();
        message.setMessage("Use the ImageKit authorization and completion upload endpoints.");
        return ResponseEntity.status(HttpStatus.GONE).body(message);
    }

    @PostMapping("/upload/authorization")
    public ResponseEntity<ImageKitUploadAuthorization> createUploadAuthorization(
            @RequestParam UUID channelId,
            @RequestParam String fileName,
            @RequestParam long fileSize,
            @RequestParam(required = false) String replaceVideoId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            return ResponseEntity.ok(
                    uploadService.createImageKitUploadAuthorization(
                            channelId, fileName, fileSize, replaceVideoId, authorization));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.error("ImageKit upload storage is not configured", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Could not verify channel upload permission", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping("/upload/complete")
    public ResponseEntity<VideoDetail> completeImageKitUpload(
            @jakarta.validation.Valid @RequestBody ImageKitUploadCompletionRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            VideoDetail video = uploadService.completeImageKitUpload(request, authorization);
            return ResponseEntity.status(request.isReplaceExisting()
                    ? HttpStatus.OK
                    : HttpStatus.CREATED).body(video);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (io.imagekit.sdk.exceptions.ForbiddenException
                 | io.imagekit.sdk.exceptions.TooManyRequestsException
                 | io.imagekit.sdk.exceptions.InternalServerException
                 | io.imagekit.sdk.exceptions.UnauthorizedException
                 | io.imagekit.sdk.exceptions.BadRequestException
                 | io.imagekit.sdk.exceptions.UnknownException
                 | org.springframework.web.client.RestClientException e) {
            log.error("Could not persist ImageKit video metadata", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/stream/{filename}")
    public ResponseEntity<ResourceRegion> streamVideo(@RequestHeader HttpHeaders headers, @PathVariable String filename) {
        try {
            Resource video = videoStorageService.loadVideoAsResource(filename);
            long contentLength = video.contentLength();
            List<HttpRange> ranges = headers.getRange();
            
            if (ranges.isEmpty()) {
                long rangeLength = Math.min(1024 * 1024, contentLength);
                ResourceRegion region = new ResourceRegion(video, 0, rangeLength);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(region);
            } else {
                HttpRange range = ranges.get(0);
                long start = range.getRangeStart(contentLength);
                long end = range.getRangeEnd(contentLength);
                long rangeLength = Math.min(1024 * 1024, end - start + 1);
                ResourceRegion region = new ResourceRegion(video, start, rangeLength);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(region);
            }
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to stream video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
