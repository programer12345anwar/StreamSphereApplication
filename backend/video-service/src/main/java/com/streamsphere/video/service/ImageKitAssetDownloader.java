package com.streamsphere.video.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ImageKitAssetDownloader {
    public void downloadTo(URI assetUri, Path destination, long maximumBytes, int timeoutMillis) {
        if (!"https".equalsIgnoreCase(assetUri.getScheme())
                || assetUri.getHost() == null
                || assetUri.getUserInfo() != null
                || (assetUri.getPort() != -1 && assetUri.getPort() != 443)) {
            throw new IllegalArgumentException("Only public HTTPS ImageKit assets can be downloaded.");
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) assetUri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new ThumbnailProcessingException("ImageKit asset download did not return HTTP 200.");
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > maximumBytes) {
                throw new ThumbnailProcessingException("ImageKit asset exceeds the configured size limit.");
            }

            long downloadedBytes = 0;
            try (InputStream input = connection.getInputStream();
                 var output = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    downloadedBytes += count;
                    if (downloadedBytes > maximumBytes) {
                        throw new ThumbnailProcessingException("ImageKit asset exceeds the configured size limit.");
                    }
                    output.write(buffer, 0, count);
                }
            }
            if (downloadedBytes == 0) {
                throw new ThumbnailProcessingException("ImageKit returned an empty asset.");
            }
        } catch (IOException exception) {
            throw new ThumbnailProcessingException("Could not download ImageKit asset.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
