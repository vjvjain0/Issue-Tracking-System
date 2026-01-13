package com.ticketing.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    private String id;
    private String userId;
    private String userName;
    private String action;
    private String details;
    private LocalDateTime timestamp;
}
