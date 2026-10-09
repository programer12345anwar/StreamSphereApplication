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
import org.springframework.web.multipart.MultipartFile;

import com.streamsphere.video.dto.response.GeneralMessage;
import com.streamsphere.video.dto.response.VideoDetail;
import com.streamsphere.video.dto.request.VideoDetailRequestBody;
import com.streamsphere.video.exception.InvalidFileType;
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
    public ResponseEntity uploadVideo(@RequestPart("videoFile")MultipartFile video,
                                      @RequestParam UUID channelId,
                                      @RequestPart(value = "videodetails", required = false) VideoDetailRequestBody videoDetails,
                                      @RequestPart(value = "videoDetails", required = false) VideoDetailRequestBody videoDetailsAlt){

        try{
            log.info("received call to upload video at video controller");
            VideoDetailRequestBody payload = videoDetails != null ? videoDetails : videoDetailsAlt;
            if (payload == null) {
                GeneralMessage generalMessage = new GeneralMessage();
                generalMessage.setMessage("Missing video details payload.");
                return new ResponseEntity(generalMessage, HttpStatus.BAD_REQUEST);
            }
            if (payload.getName() == null || payload.getName().isBlank()) {
                GeneralMessage generalMessage = new GeneralMessage();
                generalMessage.setMessage("Video name is required.");
                return new ResponseEntity(generalMessage, HttpStatus.BAD_REQUEST);
            }
            VideoDetail videoDetail = uploadService.uploadVideo(video, channelId, payload);
            return new ResponseEntity(videoDetail, HttpStatus.CREATED); 
        }catch (InvalidFileType invalidFileType){
            GeneralMessage generalMessage = new GeneralMessage();
            generalMessage.setMessage(invalidFileType.getMessage());
            return new ResponseEntity(generalMessage, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            log.error("Video upload failed", e);
            GeneralMessage generalMessage = new GeneralMessage();
            generalMessage.setMessage("Video upload failed");
            return new ResponseEntity(generalMessage, HttpStatus.INTERNAL_SERVER_ERROR);
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
        } catch (Exception e) {
            log.error("Failed to stream video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
