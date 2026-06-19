package com.youtube.video_service.security;

import com.youtube.video_service.service.CentralApiConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    @Autowired
    CentralApiConnectionService centralApiConnectionService;
    public boolean isValidToken(String token) {
        return centralApiConnectionService.isValidToken(token);
    }

    public String decryptToken(String token) {
        return centralApiConnectionService.getCredentialFromToken(token);
    }
}
