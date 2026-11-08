package com.streamsphere.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class FfmpegMediaProcessor {
    private static final long MAX_PROCESS_OUTPUT_BYTES = 64 * 1024;
    private final String ffmpegPath;
    private final String ffprobePath;
    private final Duration timeout;
    private final long maximumThumbnailBytes;

    public FfmpegMediaProcessor(
            @Value("${video.thumbnail.ffmpeg-path:ffmpeg}") String ffmpegPath,
            @Value("${video.thumbnail.ffprobe-path:ffprobe}") String ffprobePath,
            @Value("${video.thumbnail.process-timeout-seconds:60}") long timeoutSeconds,
            @Value("${video.thumbnail.max-bytes:5242880}") long maximumThumbnailBytes) {
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.maximumThumbnailBytes = maximumThumbnailBytes;
    }

    public VideoMetadata probeVideo(Path videoFile) {
        String output = run(List.of(
                ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoFile.toAbsolutePath().toString()));
        try {
            double duration = Double.parseDouble(output.trim());
            if (!Double.isFinite(duration) || duration <= 0 || duration > Integer.MAX_VALUE) {
                throw new NumberFormatException("duration is outside the supported range");
            }
            return new VideoMetadata((int) Math.ceil(duration));
        } catch (NumberFormatException exception) {
            throw new ThumbnailProcessingException("FFprobe returned an invalid video duration.", exception);
        }
    }

    public ImageMetadata inspectImage(Path imagePath) {
        String output = run(List.of(
                ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name,width,height",
                "-of", "csv=p=0:s=,",
                imagePath.toAbsolutePath().toString()));
        String[] parts = output.trim().split(",", -1);
        if (parts.length != 3) {
            throw new ThumbnailProcessingException("FFprobe did not recognize the thumbnail image.");
        }
        try {
            String format = parts[0].toLowerCase(Locale.ROOT);
            if (!Set.of("mjpeg", "png", "webp").contains(format)) {
                throw new ThumbnailProcessingException("Thumbnail must contain JPEG, PNG, or WebP image data.");
            }
            int width = Integer.parseInt(parts[1]);
            int height = Integer.parseInt(parts[2]);
            run(List.of(
                    ffmpegPath,
                    "-nostdin",
                    "-v", "error",
                    "-xerror",
                    "-i", imagePath.toAbsolutePath().toString(),
                    "-frames:v", "1",
                    "-f", "null",
                    "-"));
            return new ImageMetadata(format, width, height);
        } catch (NumberFormatException exception) {
            throw new ThumbnailProcessingException("FFprobe returned invalid thumbnail dimensions.", exception);
        }
    }

    public GeneratedThumbnail generateThumbnail(Path videoFile, int durationSeconds, Path temporaryDirectory) {
        Set<Double> candidateTimes = new LinkedHashSet<>();
        candidateTimes.add(Math.min(1.0, durationSeconds / 2.0));
        candidateTimes.add(durationSeconds * 0.1);
        candidateTimes.add(durationSeconds * 0.25);
        candidateTimes.add(durationSeconds * 0.5);
        candidateTimes.add(durationSeconds * 0.75);

        Path bestFrame = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (double timestamp : candidateTimes) {
            Path candidate;
            try {
                candidate = Files.createTempFile(temporaryDirectory, "candidate-", ".jpg");
                Files.deleteIfExists(candidate);
                run(List.of(
                        ffmpegPath,
                        "-nostdin",
                        "-hide_banner",
                        "-loglevel", "error",
                        "-y",
                        "-rw_timeout", "15000000",
                        "-ss", String.format(Locale.ROOT, "%.3f", Math.max(0, timestamp)),
                        "-i", videoFile.toAbsolutePath().toString(),
                        "-frames:v", "1",
                        "-vf", "scale=1280:720:force_original_aspect_ratio=increase,crop=1280:720",
                        "-q:v", "3",
                        candidate.toAbsolutePath().toString()));
                if (!Files.isRegularFile(candidate)
                        || Files.size(candidate) == 0
                        || Files.size(candidate) > maximumThumbnailBytes) {
                    continue;
                }
                double score = visualScore(candidate);
                if (score > bestScore) {
                    bestScore = score;
                    bestFrame = candidate;
                }
            } catch (ThumbnailProcessingException | IOException exception) {
                log.debug("Could not extract a thumbnail candidate at {} seconds", timestamp, exception);
            }
        }

        if (bestFrame == null) {
            throw new ThumbnailProcessingException("No suitable frame could be extracted from the video.");
        }
        try {
            return new GeneratedThumbnail(Files.readAllBytes(bestFrame));
        } catch (IOException exception) {
            throw new ThumbnailProcessingException("Could not read the generated thumbnail.", exception);
        }
    }

    private double visualScore(Path imagePath) {
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (image == null) {
                return Double.NEGATIVE_INFINITY;
            }
            double sum = 0;
            double squareSum = 0;
            long samples = 0;
            for (int y = 0; y < image.getHeight(); y += 16) {
                for (int x = 0; x < image.getWidth(); x += 16) {
                    int color = image.getRGB(x, y);
                    double luminance = (0.2126 * ((color >> 16) & 0xff)
                            + 0.7152 * ((color >> 8) & 0xff)
                            + 0.0722 * (color & 0xff)) / 255.0;
                    sum += luminance;
                    squareSum += luminance * luminance;
                    samples++;
                }
            }
            double average = sum / samples;
            if (average < 0.035) {
                return Double.NEGATIVE_INFINITY;
            }
            double variance = Math.max(0, squareSum / samples - average * average);
            return average * 0.25 + Math.sqrt(variance) * 0.75;
        } catch (IOException exception) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    private String run(List<String> command) {
        Process process;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AtomicReference<IOException> readFailure = new AtomicReference<>();
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            Thread outputReader = new Thread(() -> {
                try (var stream = process.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        int remaining = (int) Math.max(0, MAX_PROCESS_OUTPUT_BYTES - output.size());
                        if (remaining > 0) {
                            output.write(buffer, 0, Math.min(count, remaining));
                        }
                    }
                } catch (IOException exception) {
                    readFailure.set(exception);
                }
            }, "streamsphere-media-output-reader");
            outputReader.setDaemon(true);
            outputReader.start();
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                    outputReader.join();
                    throw new ThumbnailProcessingException(
                            "FFmpeg/FFprobe exceeded its configured timeout.");
                }
                outputReader.join();
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new ThumbnailProcessingException(
                        "FFmpeg/FFprobe execution was interrupted.", exception);
            }
            if (readFailure.get() != null) {
                throw readFailure.get();
            }
            String text = output.toString(StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new ThumbnailProcessingException("FFmpeg/FFprobe failed: " + text.trim());
            }
            return text;
        } catch (IOException exception) {
            throw new ThumbnailProcessingException(
                    "Could not execute configured FFmpeg/FFprobe binary.", exception);
        }
    }

    public record VideoMetadata(int durationSeconds) {}

    public record ImageMetadata(String format, int width, int height) {}

    public record GeneratedThumbnail(byte[] bytes) {}
}
