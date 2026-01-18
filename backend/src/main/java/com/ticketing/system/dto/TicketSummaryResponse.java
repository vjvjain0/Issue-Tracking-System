package com.ticketing.system.dto;

import com.ticketing.system.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight ticket summary for list views.
 * Does not include comments and activities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryResponse {
    private String id;
    private String title;
    private String description;
    private TicketStatus status;
    private String assignedAgentId;
    private String assignedAgentName;
    private String customerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
