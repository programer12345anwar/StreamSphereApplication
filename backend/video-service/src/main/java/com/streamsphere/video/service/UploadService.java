package com.streamsphere.video.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.streamsphere.video.dto.response.VideoDetail;
import com.streamsphere.video.dto.request.VideoDetailRequestBody;
import com.streamsphere.video.dto.request.VideoDetailsDTO;
import com.streamsphere.video.exception.InvalidFileType;
import com.streamsphere.video.client.central.CentralApiConnectionService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UploadService {

    @Autowired
    CentralApiConnectionService centralApiConnectionService;
    
    @Autowired
    VideoStorageService videoStorageService;

    @Autowired
    AsyncVideoProcessor asyncVideoProcessor;

    public boolean isVideoFile(MultipartFile file){
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }

    public VideoDetail uploadVideo(MultipartFile video,
                                   UUID channelId,
                                   VideoDetailRequestBody videoDetails) throws IOException {
        log.info("uploadVideo called in UploadService");
        if(!isVideoFile(video)){
            throw new InvalidFileType("File uploaded is not video");
        }

        // Phase 3: Store video locally using stream transfer, bypassing memory limits
        String storedFilename = videoStorageService.storeVideo(video);
        
        String videoUrl = "/api/v1/video/stream/" + storedFilename;
        String videoId = UUID.randomUUID().toString(); 

        VideoDetail videoDetail = new VideoDetail();
        videoDetail.setVideoId(videoId);
        videoDetail.setVideoUrl(videoUrl);

        VideoDetailsDTO videoDetailsDTO = new VideoDetailsDTO();
        videoDetailsDTO.setVideoLink(videoUrl);
        videoDetailsDTO.setId(videoId);
        videoDetailsDTO.setTags(videoDetails.getTags());
        videoDetailsDTO.setUploadDateTime(LocalDateTime.now());
        videoDetailsDTO.setUpdatedAt(LocalDateTime.now());
        videoDetailsDTO.setName(videoDetails.getName());
        videoDetailsDTO.setDescription(videoDetails.getDescription());
        videoDetailsDTO.setStatus("PROCESSING"); 
        videoDetailsDTO.setVisibility("PUBLIC");
        
        log.info("Saving initial video details to central service with status PROCESSING");
        centralApiConnectionService.saveVideoDetails(channelId, videoDetailsDTO);
        
        // Trigger asynchronous processing workflow
        asyncVideoProcessor.processVideo(channelId, videoId, videoDetailsDTO, storedFilename);
        
        return videoDetail;
    }
}
