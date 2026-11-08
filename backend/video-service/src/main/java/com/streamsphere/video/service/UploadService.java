package com.streamsphere.video.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.net.URI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Autowired
    FfmpegMediaProcessor mediaProcessor;

    @Autowired
    ImageKitAssetDownloader assetDownloader;

    @Value("${video.upload.max-bytes:104857600}")
    long maxUploadBytes;

    @Value("${video.thumbnail.max-bytes:5242880}")
    long maxThumbnailBytes;

    @Value("${video.thumbnail.min-width:160}")
    int minimumThumbnailWidth;

    @Value("${video.thumbnail.min-height:90}")
    int minimumThumbnailHeight;

    @Value("${video.thumbnail.max-width:4096}")
    int maximumThumbnailWidth;

    @Value("${video.thumbnail.max-height:4096}")
    int maximumThumbnailHeight;

    @Value("${video.thumbnail.max-pixels:16777216}")
    long maximumThumbnailPixels;

    @Value("${video.thumbnail.download-timeout-ms:10000}")
    int thumbnailDownloadTimeoutMillis;

    private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.[a-zA-Z0-9]{1,8}");
    private static final Pattern THUMBNAIL_EXTENSION = Pattern.compile("(?i)\\.(jpg|jpeg|png|webp)");
    private static final Duration IMAGEKIT_AUTHORIZATION_LIFETIME = Duration.ofMinutes(30);

    public ImageKitUploadAuthorization createImageKitUploadAuthorization(
            UUID channelId,
            String originalFilename,
            long fileSize,
            String replaceVideoId,
            String authorization) {
        if (fileSize <= 0 || fileSize > maxUploadBytes) {
            throw new IllegalArgumentException("Video file size must be between 1 byte and "
                    + maxUploadBytes + " bytes.");
        }
        if (replaceVideoId != null && !replaceVideoId.matches("^[0-9a-fA-F-]{36}$")) {
            throw new IllegalArgumentException("Replacement video ID is invalid.");
        }
        if (!centralApiConnectionService.canUploadToChannel(channelId, replaceVideoId, authorization)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not allowed to upload to this channel.");
        }
        Configuration configuration = getConfiguredImageKit();

        String videoId = replaceVideoId == null ? UUID.randomUUID().toString() : replaceVideoId;
        int extensionIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        String extension = extensionIndex > 0 ? originalFilename.substring(extensionIndex) : "";
        if (!SAFE_EXTENSION.matcher(extension).matches()) {
            extension = ".mp4";
        }
        String replacementSuffix = replaceVideoId == null ? "" : "_" + UUID.randomUUID();
        String fileName = videoId + replacementSuffix + extension.toLowerCase(Locale.ROOT);
        String folder = "/streamsphere/videos/" + channelId;

        return createAuthorization(configuration, videoId, fileName, folder, replaceVideoId != null);
    }

    public ImageKitUploadAuthorization createThumbnailUploadAuthorization(
            UUID channelId,
            String videoId,
            String originalFilename,
            long fileSize,
            boolean replacingExisting,
            String authorization) {
        if (videoId == null || !videoId.matches("^[0-9a-fA-F-]{36}$")) {
            throw new IllegalArgumentException("Video ID is invalid.");
        }
        if (fileSize <= 0 || fileSize > maxThumbnailBytes) {
            throw new IllegalArgumentException(
                    "Thumbnail file size must be between 1 byte and " + maxThumbnailBytes + " bytes.");
        }
        String extension = originalFilename == null || originalFilename.lastIndexOf('.') < 0
                ? ""
                : originalFilename.substring(originalFilename.lastIndexOf('.'));
        if (!THUMBNAIL_EXTENSION.matcher(extension).matches()) {
            throw new IllegalArgumentException("Thumbnail must be a JPEG, PNG, or WebP image.");
        }
        if (!centralApiConnectionService.canUploadToChannel(
                channelId, replacingExisting ? videoId : null, authorization)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not allowed to upload a thumbnail to this channel.");
        }

        Configuration configuration = getConfiguredImageKit();
        String fileName = videoId + "_custom_" + UUID.randomUUID() + extension.toLowerCase(Locale.ROOT);
        String folder = "/streamsphere/thumbnails/" + channelId;
        return createAuthorization(configuration, videoId, fileName, folder, replacingExisting);
    }

    public VideoDetail completeImageKitUpload(
            ImageKitUploadCompletionRequest request, String authorization)
            throws io.imagekit.sdk.exceptions.ForbiddenException,
            io.imagekit.sdk.exceptions.TooManyRequestsException,
            io.imagekit.sdk.exceptions.InternalServerException,
            io.imagekit.sdk.exceptions.UnauthorizedException,
            io.imagekit.sdk.exceptions.BadRequestException,
            io.imagekit.sdk.exceptions.UnknownException {
        if (!centralApiConnectionService.canUploadToChannel(
                request.getChannelId(),
                request.isReplaceExisting() ? request.getVideoId() : null,
                authorization)) {
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

        String customThumbnailUrl = request.getThumbnailFileId() == null
                ? null
                : validateCustomThumbnail(request.getThumbnailFileId(), request.getChannelId(), request.getVideoId());

        if (request.isReplaceExisting()) {
            centralApiConnectionService.replaceVideoMedia(
                    request.getChannelId(),
                    request.getVideoId(),
                    asset.getUrl(),
                    customThumbnailUrl,
                    authorization);
        } else {
            VideoDetailsDTO details = new VideoDetailsDTO();
            details.setId(request.getVideoId());
            details.setName(request.getName());
            details.setDescription(request.getDescription());
            details.setTags(request.getTags());
            details.setShort(request.isShort());
            details.setVideoLink(asset.getUrl());
            details.setThumbnailLink(customThumbnailUrl);
            details.setThumbnailSource(customThumbnailUrl == null ? null : "CUSTOM");
            details.setUploadDateTime(LocalDateTime.now());
            details.setUpdatedAt(LocalDateTime.now());
            details.setStatus("PROCESSING");
            details.setVisibility("PUBLIC");
            centralApiConnectionService.saveVideoDetails(request.getChannelId(), details, authorization);
        }

        VideoDetail response = new VideoDetail();
        response.setVideoId(request.getVideoId());
        response.setVideoUrl(asset.getUrl());
        return response;
    }

    private ImageKitUploadAuthorization createAuthorization(
            Configuration configuration,
            String videoId,
            String fileName,
            String folder,
            boolean replacement) {
        long expiresAt = Instant.now().plus(IMAGEKIT_AUTHORIZATION_LIFETIME).getEpochSecond();
        return new ImageKitUploadAuthorization(
                configuration.getPublicKey(),
                videoId,
                fileName,
                folder,
                imageKit.getAuthenticationParameters(null, expiresAt),
                replacement);
    }

    private Configuration getConfiguredImageKit() {
        Configuration configuration = imageKit.getConfig();
        if (configuration == null || configuration.getPublicKey() == null
                || configuration.getPublicKey().isBlank()
                || configuration.getUrlEndpoint() == null
                || configuration.getUrlEndpoint().isBlank()) {
            throw new IllegalStateException("ImageKit upload storage is not configured.");
        }
        return configuration;
    }

    private String validateCustomThumbnail(String fileId, UUID channelId, String videoId)
            throws io.imagekit.sdk.exceptions.ForbiddenException,
            io.imagekit.sdk.exceptions.TooManyRequestsException,
            io.imagekit.sdk.exceptions.InternalServerException,
            io.imagekit.sdk.exceptions.UnauthorizedException,
            io.imagekit.sdk.exceptions.BadRequestException,
            io.imagekit.sdk.exceptions.UnknownException {
        Result asset = imageKit.getFileDetail(fileId);
        String expectedFolder = "/streamsphere/thumbnails/" + channelId + "/";
        if (asset == null
                || asset.getUrl() == null
                || asset.getFilePath() == null
                || !asset.getFilePath().startsWith(expectedFolder)
                || asset.getName() == null
                || !asset.getName().startsWith(videoId + "_custom_")
                || asset.getSize() <= 0
                || asset.getSize() > maxThumbnailBytes
                || asset.getMime() == null
                || !asset.getMime().toLowerCase(Locale.ROOT).matches("image/(jpeg|png|webp)")) {
            throw new IllegalArgumentException("Uploaded thumbnail does not match the authorized image.");
        }

        URI assetUri = validateImageKitAssetUrl(asset.getUrl());
        Path temporaryImage;
        try {
            temporaryImage = Files.createTempFile("streamsphere-custom-thumbnail-", ".image");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create temporary thumbnail validation file.", exception);
        }
        try {
            assetDownloader.downloadTo(
                    assetUri, temporaryImage, maxThumbnailBytes, thumbnailDownloadTimeoutMillis);
            FfmpegMediaProcessor.ImageMetadata image;
            try {
                image = mediaProcessor.inspectImage(temporaryImage);
            } catch (com.streamsphere.video.service.ThumbnailProcessingException exception) {
                throw new IllegalArgumentException("Thumbnail file contents are not a supported image.", exception);
            }
            String expectedMime = switch (image.format()) {
                case "mjpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                default -> throw new IllegalArgumentException("Thumbnail must be JPEG, PNG, or WebP.");
            };
            if (!expectedMime.equalsIgnoreCase(asset.getMime())) {
                throw new IllegalArgumentException("Thumbnail extension and image content do not match.");
            }
            if (image.width() < minimumThumbnailWidth
                    || image.height() < minimumThumbnailHeight
                    || image.width() > maximumThumbnailWidth
                    || image.height() > maximumThumbnailHeight
                    || (long) image.width() * image.height() > maximumThumbnailPixels) {
                throw new IllegalArgumentException("Thumbnail dimensions are outside the allowed range.");
            }
            return assetUri.toString();
        } finally {
            try {
                Files.deleteIfExists(temporaryImage);
            } catch (IOException exception) {
                org.slf4j.LoggerFactory.getLogger(UploadService.class)
                        .warn("Could not remove temporary thumbnail validation file {}", temporaryImage, exception);
            }
        }
    }

    private URI validateImageKitAssetUrl(String value) {
        try {
            URI asset = URI.create(value);
            URI endpoint = URI.create(getConfiguredImageKit().getUrlEndpoint());
            if (!"https".equalsIgnoreCase(asset.getScheme())
                    || asset.getHost() == null
                    || asset.getUserInfo() != null
                    || (asset.getPort() != -1 && asset.getPort() != 443)
                    || endpoint.getHost() == null
                    || !endpoint.getHost().equalsIgnoreCase(asset.getHost())) {
                throw new IllegalArgumentException("ImageKit asset URL is not hosted by the configured account.");
            }
            return asset;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("ImageKit returned an invalid thumbnail URL.", exception);
        }
    }

}
