package com.youtube.video_service.service;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.youtube.video_service.dto.VideoDetail;
import com.youtube.video_service.dto.VideoDetailRequestBody;
import com.youtube.video_service.dto.VideoDetailsDTO;
import com.youtube.video_service.exception.InvalidFileType;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.exceptions.BadRequestException;
import io.imagekit.sdk.exceptions.ForbiddenException;
import io.imagekit.sdk.exceptions.InternalServerException;
import io.imagekit.sdk.exceptions.TooManyRequestsException;
import io.imagekit.sdk.exceptions.UnauthorizedException;
import io.imagekit.sdk.exceptions.UnknownException;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UploadService {
   @Autowired
    ImageKit imageKit;

    @Autowired
    CentralApiConnectionService centralApiConnectionService;

    public boolean isVideoFile(MultipartFile file){
        String contentType = file.getContentType();
        boolean res = contentType != null && contentType.startsWith("video/");  //for image upload "image/"
        return res;
    }

    public VideoDetail uploadVideo(MultipartFile video,
                                   UUID channelId,
                                   VideoDetailRequestBody videoDetails) throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {
        log.info("called to service later to upload video over channel");
        boolean isVideo = isVideoFile(video);
        if(!isVideo){
            throw new InvalidFileType("File uploaded is not video");
        }

            // If file is of type video then we need to convert it in byte array
            byte [] videoBytes = video.getBytes(); // so to pass over the network we are converting our multipart file to videobytes
            // We need to create one request which we will upload it to imagekit.io
            FileCreateRequest videoRequest = new FileCreateRequest(videoBytes, video.getOriginalFilename());
            videoRequest.setUseUniqueFileName(true);
            log.info("called to imagekit server to upload video over image kit to return video id and link");
            Result result = imageKit.upload(videoRequest); // By this line video will get uploaded over image kit server.
            log.info("this is the result"+result);
            String videoId = result.getFileId();
            String videoUrl = result.getUrl();
            VideoDetail videoDetail = new VideoDetail();
            videoDetail.setVideoId(videoId);
            videoDetail.setVideoUrl(videoUrl);
            log.info("after uploading video over imagekit"+" "+videoId+" "+videoUrl);
            // We need to make a call to central api to save video details in video detail table.
            VideoDetailsDTO videoDetailsDTO = new VideoDetailsDTO();
            videoDetailsDTO.setVideoLink(videoUrl);
            videoDetailsDTO.setId(videoId);
            videoDetailsDTO.setTags(videoDetails.getTags());
            videoDetailsDTO.setUploadDateTime(LocalDateTime.now());
            videoDetailsDTO.setUpdatedAt(LocalDateTime.now());
            videoDetailsDTO.setName(videoDetails.getName());
            videoDetailsDTO.setDescription(videoDetails.getDescription());
             log.info("this is videoDetailsDTO"+videoDetailsDTO);
            centralApiConnectionService.saveVideoDetails(channelId, videoDetailsDTO);
            return videoDetail;
    }

    //for test purposes to upload video to the imagekit
    /* 

    public VideoDetail uploadVideo(MultipartFile video) throws IOException, Exception, BadRequestException, UnknownException, ForbiddenException, TooManyRequestsException, UnauthorizedException {
        boolean isVideo = isVideoFile(video);
        if(!isVideo){
            throw new InvalidFileType("File uploaded is not video");
        }

            // If file is of type video then we need to convert it in byte array
            byte [] videoBytes = video.getBytes(); // so to pass over the network we are converting our multipart file to videobytes
            // We need to create one request which we will upload it to imagekit.io
            FileCreateRequest videoRequest = new FileCreateRequest(videoBytes, video.getOriginalFilename());
            videoRequest.setUseUniqueFileName(true);

            log.info("Video request is {}", videoRequest);

            Result result = imageKit.upload(videoRequest); // By this line video will get uploaded over image kit server.

            

            String videoId = result.getFileId();
            String videoUrl = result.getUrl();

            // log.info(videoUrl, videoId, videoUrl);

            VideoDetail videoDetail = new VideoDetail();
            videoDetail.setVideoId(videoId);
            videoDetail.setVideoUrl(videoUrl);
            // log.info(videoUrl, videoDetail);

            return videoDetail;
    }
            */
}
