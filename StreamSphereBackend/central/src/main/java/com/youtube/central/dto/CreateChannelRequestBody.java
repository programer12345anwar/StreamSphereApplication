package com.youtube.central.dto;

import lombok.Data;

@Data
public class CreateChannelRequestBody {
    private String userEmail;
    private String channelName;
    private String description;
}
