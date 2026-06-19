package com.youtube.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSummaryDTO {
    private UUID id;
    private String name;
    private String description;
    private int totalSubs;
    private int totalViews;
}
