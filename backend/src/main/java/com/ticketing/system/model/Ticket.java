package com.ticketing.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {
    @Id
    private String id;

    private String title;

    private String description;

    private TicketStatus status;

    private Priority priority;

    private String assignedAgentId;

    private String assignedAgentName;

    private String customerEmail;

    private String customerName;

    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    private List<Activity> activities = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Timestamp when ticket was closed (RESOLVED or INVALID)
    private LocalDateTime closedAt;

    // Flag to indicate if ticket was auto-assigned
    private boolean autoAssigned;
}
