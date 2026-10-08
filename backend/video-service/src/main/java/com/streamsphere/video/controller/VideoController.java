package com.streamsphere.video.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.streamsphere.video.dto.response.GeneralMessage;
import com.streamsphere.video.dto.response.VideoDetail;
import com.streamsphere.video.dto.request.VideoDetailRequestBody;
import com.streamsphere.video.exception.InvalidFileType;
import com.streamsphere.video.service.UploadService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/video")
@Slf4j
public class VideoController {

  @Autowired
    UploadService uploadService;


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
            return new ResponseEntity(videoDetail, HttpStatus.CREATED); // 201
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

}

