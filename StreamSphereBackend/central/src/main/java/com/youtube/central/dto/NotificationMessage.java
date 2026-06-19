package com.youtube.central.dto;

import lombok.Data;

@Data
public class NotificationMessage {
    private String type; // user-registration, video-upload-notification, new-video-available
    private String email; // user we need to send notification
    private String name; // name of the user.
}
