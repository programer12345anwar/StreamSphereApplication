package com.youtube.video_service.dto;
import lombok.Data;

@Data
public class IsValidDTO {
    boolean success;
    String credentials;
}