package com.streamsphere.video.service;

import com.streamsphere.video.dto.request.VideoDetailsDTO;
import com.streamsphere.video.client.central.CentralApiConnectionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AsyncVideoProcessor {

    private final CentralApiConnectionService centralApiConnectionService;
    private final String storageLocation;

    public AsyncVideoProcessor(CentralApiConnectionService centralApiConnectionService, 
                               @org.springframework.beans.factory.annotation.Value("${video.storage.location:uploads/videos}") String storageLocation) {
        this.centralApiConnectionService = centralApiConnectionService;
        this.storageLocation = storageLocation;
    }

    @Async
    public void processVideo(UUID channelId, String videoId, VideoDetailsDTO videoDetailsDTO, String storedFilename) {
        log.info("Starting asynchronous processing for video: {}", videoId);
        
        try {
            // Generate Thumbnail
            String thumbnailFilename = generateThumbnail(storedFilename, videoId);
            if (thumbnailFilename != null) {
                String thumbnailUrl = "/api/v1/video/stream/" + thumbnailFilename;
                videoDetailsDTO.setThumbnailLink(thumbnailUrl);
                centralApiConnectionService.updateVideoDetails(videoId, videoDetailsDTO);
            }

            // Mock video processing (e.g. transcode wait)
            Thread.sleep(3000); 

            // Update status to PUBLISHED
            videoDetailsDTO.setStatus("PUBLISHED");
            log.info("Video processing complete. Updating status to PUBLISHED for video: {}", videoId);
            centralApiConnectionService.updateVideoStatus(videoId, "PUBLISHED");
        } catch (InterruptedException e) {
            log.error("Video processing interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error processing video", e);
        }
    }

    private String generateThumbnail(String videoFilename, String videoId) {
        try {
            Path videoPath = Paths.get(storageLocation).resolve(videoFilename).normalize().toAbsolutePath();
            File videoFile = videoPath.toFile();
            
            if (!videoFile.exists()) {
                log.warn("Video file not found for thumbnail generation: {}", videoFile);
                return null;
            }

            // Grab a frame at 1 second mark
            Picture picture = FrameGrab.getFrameFromFile(videoFile, 30); // 30th frame ~ 1s in 30fps
            if (picture == null) {
                log.warn("Could not extract frame from video");
                return null;
            }

            BufferedImage img = AWTUtil.toBufferedImage(picture);
            
            String thumbnailFilename = videoId + "_thumb.jpg";
            File thumbnailFile = Paths.get(storageLocation).resolve(thumbnailFilename).normalize().toAbsolutePath().toFile();
            
            ImageIO.write(img, "jpg", thumbnailFile);
            log.info("Successfully generated thumbnail: {}", thumbnailFilename);
            return thumbnailFilename;
            
        } catch (IOException | JCodecException | IllegalArgumentException e) {
            log.error("Failed to generate thumbnail for video {}", videoFilename, e);
            return null;
        }
    }
}
