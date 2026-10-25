package com.streamsphere.video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ImageKitUploadAuthorization {
    private String publicKey;
    private String videoId;
    private String fileName;
    private String folder;
    private Map<String, String> authentication;
    private boolean replacement;
}
