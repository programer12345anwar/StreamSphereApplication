package com.streamsphere.central.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChannelRequestBody {
    @NotBlank
    @Email
    private String userEmail;

    @NotBlank
    @Size(max = 100)
    private String channelName;

    @Size(max = 1000)
    private String description;
}


