package com.ticketing.system.dto;

import com.ticketing.system.model.Activity;
import com.ticketing.system.model.Comment;
import com.ticketing.system.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private String id;
    private String title;
    private String description;
    private TicketStatus status;
    private String assignedAgentId;
    private String assignedAgentName;
    private String customerEmail;
    private String customerName;
    private List<Comment> comments;
    private List<Activity> activities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private boolean autoAssigned;
}
