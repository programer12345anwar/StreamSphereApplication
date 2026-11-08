package com.streamsphere.video.service;

import com.streamsphere.video.events.VideoThumbnailRequestedEvent;
import com.streamsphere.video.events.VideoThumbnailResultEvent;
import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@Slf4j
public class ThumbnailProcessingService {
    private final FfmpegMediaProcessor mediaProcessor;
    private final ImageKitAssetDownloader assetDownloader;
    private final ImageKit imageKit;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String resultRoutingKey;
    private final int maximumAttempts;
    private final long maximumVideoBytes;
    private final int downloadTimeoutMillis;

    public ThumbnailProcessingService(
            FfmpegMediaProcessor mediaProcessor,
            ImageKitAssetDownloader assetDownloader,
            ImageKit imageKit,
            RabbitTemplate rabbitTemplate,
            @Value("${thumbnail.events.exchange}") String exchange,
            @Value("${thumbnail.events.result-routing-key}") String resultRoutingKey,
            @Value("${video.thumbnail.max-attempts:3}") int maximumAttempts,
            @Value("${video.upload.max-bytes:104857600}") long maximumVideoBytes,
            @Value("${video.thumbnail.download-timeout-ms:10000}") int downloadTimeoutMillis) {
        this.mediaProcessor = mediaProcessor;
        this.assetDownloader = assetDownloader;
        this.imageKit = imageKit;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.resultRoutingKey = resultRoutingKey;
        this.maximumAttempts = maximumAttempts;
        this.maximumVideoBytes = maximumVideoBytes;
        this.downloadTimeoutMillis = downloadTimeoutMillis;
    }

    @RabbitListener(queues = "${thumbnail.events.request-queue}")
    public void processVideoThumbnail(VideoThumbnailRequestedEvent event) {
        VideoThumbnailResultEvent result = processWithRetry(event);
        publishResult(result);
    }

    private VideoThumbnailResultEvent processWithRetry(VideoThumbnailRequestedEvent event) {
        if (event == null || event.eventId() == null || event.videoId() == null || event.videoUrl() == null) {
            throw new IllegalArgumentException("Thumbnail processing event is incomplete.");
        }

        ThumbnailProcessingException lastFailure = null;
        for (int attempt = 1; attempt <= maximumAttempts; attempt++) {
            try {
                return process(event);
            } catch (ThumbnailProcessingException exception) {
                lastFailure = exception;
                log.warn(
                        "Thumbnail processing attempt {}/{} failed for video {}",
                        attempt,
                        maximumAttempts,
                        event.videoId(),
                        exception);
                if (attempt < maximumAttempts) {
                    pauseBeforeRetry(attempt);
                }
            }
        }

        log.error("Thumbnail processing failed after {} attempts for video {}; using the branded fallback",
                maximumAttempts, event.videoId(), lastFailure);
        return new VideoThumbnailResultEvent(
                event.eventId(),
                event.videoId(),
                null,
                "DEFAULT",
                "FALLBACK",
                null);
    }

    private VideoThumbnailResultEvent process(VideoThumbnailRequestedEvent event) {
        URI videoUri = validateImageKitUrl(event.videoUrl());
        URI customThumbnailUri = event.customThumbnailUrl() == null || event.customThumbnailUrl().isBlank()
                ? null
                : validateImageKitUrl(event.customThumbnailUrl());
        Path temporaryDirectory;
        try {
            temporaryDirectory = Files.createTempDirectory("streamsphere-thumbnail-");
        } catch (ThumbnailProcessingException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ThumbnailProcessingException("Could not create temporary thumbnail workspace.", exception);
        }

        try {
            Path videoFile = temporaryDirectory.resolve("source-video");
            try {
                assetDownloader.downloadTo(
                        videoUri, videoFile, maximumVideoBytes, downloadTimeoutMillis);
            } catch (ThumbnailProcessingException exception) {
                if (customThumbnailUri != null) {
                    log.warn("Could not fetch video {} for duration; preserving its custom thumbnail",
                            event.videoId(), exception);
                    return customResult(event, customThumbnailUri, null);
                }
                throw exception;
            }

            Integer durationSeconds;
            try {
                durationSeconds = mediaProcessor.probeVideo(videoFile).durationSeconds();
            } catch (ThumbnailProcessingException exception) {
                if (customThumbnailUri != null) {
                    log.warn("Could not read duration for video {}; preserving its custom thumbnail",
                            event.videoId(), exception);
                    return customResult(event, customThumbnailUri, null);
                }
                throw exception;
            }
            if (customThumbnailUri != null) {
                return customResult(event, customThumbnailUri, durationSeconds);
            }

            FfmpegMediaProcessor.GeneratedThumbnail thumbnail =
                    mediaProcessor.generateThumbnail(videoFile, durationSeconds, temporaryDirectory);
            Result uploaded = uploadGeneratedThumbnail(event.videoId(), thumbnail.bytes());
            URI thumbnailUri = validateImageKitUrl(uploaded.getUrl());
            return new VideoThumbnailResultEvent(
                    event.eventId(),
                    event.videoId(),
                    thumbnailUri.toString(),
                    "GENERATED",
                    "READY",
                    durationSeconds);
        } finally {
            removeTemporaryDirectory(temporaryDirectory);
        }
    }

    private VideoThumbnailResultEvent customResult(
            VideoThumbnailRequestedEvent event, URI thumbnailUri, Integer durationSeconds) {
        return new VideoThumbnailResultEvent(
                event.eventId(),
                event.videoId(),
                thumbnailUri.toString(),
                "CUSTOM",
                "READY",
                durationSeconds);
    }

    private Result uploadGeneratedThumbnail(String videoId, byte[] thumbnailBytes) {
        try {
            FileCreateRequest request = new FileCreateRequest(thumbnailBytes, videoId + ".jpg");
            request.setFolder("/streamsphere/thumbnails/generated");
            request.setUseUniqueFileName(false);
            request.setOverwriteFile(true);
            Result result = imageKit.upload(request);
            if (result == null || result.getUrl() == null || result.getUrl().isBlank()) {
                throw new ThumbnailProcessingException("ImageKit did not return a thumbnail URL.");
            }
            return result;
        } catch (io.imagekit.sdk.exceptions.ForbiddenException
                 | io.imagekit.sdk.exceptions.TooManyRequestsException
                 | io.imagekit.sdk.exceptions.InternalServerException
                 | io.imagekit.sdk.exceptions.UnauthorizedException
                 | io.imagekit.sdk.exceptions.BadRequestException
                 | io.imagekit.sdk.exceptions.UnknownException exception) {
            throw new ThumbnailProcessingException("Could not upload the generated thumbnail to ImageKit.", exception);
        }
    }

    private URI validateImageKitUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new ThumbnailProcessingException("ImageKit asset URL is missing.");
        }
        try {
            URI asset = URI.create(value);
            URI endpoint = URI.create(imageKit.getConfig().getUrlEndpoint());
            if (!"https".equalsIgnoreCase(asset.getScheme())
                    || asset.getHost() == null
                    || asset.getUserInfo() != null
                    || (asset.getPort() != -1 && asset.getPort() != 443)
                    || endpoint.getHost() == null
                    || !endpoint.getHost().equalsIgnoreCase(asset.getHost())) {
                throw new ThumbnailProcessingException(
                        "Thumbnail processing accepts assets only from the configured ImageKit host.");
            }
            return asset;
        } catch (IllegalArgumentException exception) {
            throw new ThumbnailProcessingException("ImageKit returned an invalid asset URL.", exception);
        }
    }

    private void publishResult(VideoThumbnailResultEvent event) {
        CorrelationData correlation = new CorrelationData(event.eventId().toString());
        rabbitTemplate.convertAndSend(exchange, resultRoutingKey, event, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ did not confirm the thumbnail result event.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing thumbnail result.", exception);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("Could not confirm thumbnail result publication.", exception);
        }
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            TimeUnit.SECONDS.sleep(Math.min(attempt, 5));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thumbnail retry was interrupted.", exception);
        }
    }

    private void removeTemporaryDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    log.warn("Could not remove temporary thumbnail file {}", path, exception);
                }
            });
        } catch (IOException exception) {
            log.warn("Could not inspect temporary thumbnail workspace {}", directory, exception);
        }
    }
}
