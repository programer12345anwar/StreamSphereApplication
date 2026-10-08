package com.streamsphere.central.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCredentialDTO {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}


