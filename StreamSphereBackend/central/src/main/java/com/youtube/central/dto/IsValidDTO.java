package com.youtube.central.dto;

import lombok.Data;

@Data
public class IsValidDTO {
    private boolean success;
    private String credentials;
}
