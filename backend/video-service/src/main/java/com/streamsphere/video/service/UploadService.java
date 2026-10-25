package com.streamsphere.video.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Pattern;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.streamsphere.video.dto.response.VideoDetail;
import com.streamsphere.video.dto.response.ImageKitUploadAuthorization;
import com.streamsphere.video.dto.request.VideoDetailsDTO;
import com.streamsphere.video.dto.request.ImageKitUploadCompletionRequest;
import com.streamsphere.video.client.central.CentralApiConnectionService;
import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.results.Result;

@Service
public class UploadService {

    @Autowired
    CentralApiConnectionService centralApiConnectionService;
    
    @Autowired
    ImageKit imageKit;

    @Value("${video.upload.max-bytes:524288000}")
    long maxUploadBytes;

    private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.[a-zA-Z0-9]{1,8}");
    public ImageKitUploadAuthorization createImageKitUploadAuthorization(
            UUID channelId, String originalFilename, long fileSize, String authorization) {
        if (fileSize <= 0 || fileSize > maxUploadBytes) {
            throw new IllegalArgumentException("Video file size must be between 1 byte and "
                    + maxUploadBytes + " bytes.");
        }
        if (!centralApiConnectionService.canUploadToChannel(channelId, authorization)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not allowed to upload to this channel.");
        }
        Configuration configuration = imageKit.getConfig();
        if (configuration == null || configuration.getPublicKey() == null
                || configuration.getPublicKey().isBlank()) {
            throw new IllegalStateException("ImageKit upload storage is not configured.");
        }

        String videoId = UUID.randomUUID().toString();
        int extensionIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        String extension = extensionIndex > 0 ? originalFilename.substring(extensionIndex) : "";
        if (!SAFE_EXTENSION.matcher(extension).matches()) {
            extension = ".mp4";
        }
        String fileName = videoId + extension.toLowerCase(Locale.ROOT);
        String folder = "/streamsphere/videos/" + channelId;

        return new ImageKitUploadAuthorization(
                configuration.getPublicKey(),
                videoId,
                fileName,
                folder,
                imageKit.getAuthenticationParameters());
    }

    public VideoDetail completeImageKitUpload(
            ImageKitUploadCompletionRequest request, String authorization)
            throws io.imagekit.sdk.exceptions.ForbiddenException,
            io.imagekit.sdk.exceptions.TooManyRequestsException,
            io.imagekit.sdk.exceptions.InternalServerException,
            io.imagekit.sdk.exceptions.UnauthorizedException,
            io.imagekit.sdk.exceptions.BadRequestException,
            io.imagekit.sdk.exceptions.UnknownException {
        if (!centralApiConnectionService.canUploadToChannel(request.getChannelId(), authorization)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not allowed to upload to this channel.");
        }

        Result asset = imageKit.getFileDetail(request.getFileId());
        String requiredFolder = "/streamsphere/videos/" + request.getChannelId() + "/";
        if (asset == null
                || asset.getUrl() == null
                || asset.getFilePath() == null
                || !asset.getFilePath().startsWith(requiredFolder)
                || asset.getName() == null
                || !asset.getName().startsWith(request.getVideoId())
                || asset.getSize() <= 0
                || asset.getSize() > maxUploadBytes
                || asset.getMime() == null
                || !asset.getMime().toLowerCase(Locale.ROOT).startsWith("video/")) {
            throw new IllegalArgumentException("Uploaded video does not match the authorized channel and upload.");
        }
        URI assetUri = URI.create(asset.getUrl());
        URI endpointUri = URI.create(imageKit.getConfig().getUrlEndpoint());
        if (!"https".equalsIgnoreCase(assetUri.getScheme())
                || !endpointUri.getHost().equalsIgnoreCase(assetUri.getHost())) {
            throw new IllegalArgumentException("Uploaded video URL is not hosted by the configured ImageKit account.");
        }

        VideoDetailsDTO details = new VideoDetailsDTO();
        details.setId(request.getVideoId());
        details.setName(request.getName());
        details.setDescription(request.getDescription());
        details.setTags(request.getTags());
        details.setShort(request.isShort());
        details.setVideoLink(asset.getUrl());
        details.setUploadDateTime(LocalDateTime.now());
        details.setUpdatedAt(LocalDateTime.now());
        details.setStatus("PUBLISHED");
        details.setVisibility("PUBLIC");
        centralApiConnectionService.saveVideoDetails(request.getChannelId(), details, authorization);

        VideoDetail response = new VideoDetail();
        response.setVideoId(request.getVideoId());
        response.setVideoUrl(asset.getUrl());
        return response;
    }

}
