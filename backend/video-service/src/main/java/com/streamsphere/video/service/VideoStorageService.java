package com.streamsphere.video.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface VideoStorageService {
    String storeVideo(MultipartFile file) throws IOException;
    Resource loadVideoAsResource(String filename);
    void deleteVideo(String filename);
}
