package com.streamsphere.video.service;

public class ThumbnailProcessingException extends RuntimeException {
    public ThumbnailProcessingException(String message) {
        super(message);
    }

    public ThumbnailProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
