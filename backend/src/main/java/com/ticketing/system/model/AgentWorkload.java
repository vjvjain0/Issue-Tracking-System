package com.ticketing.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the current workload of an agent for auto-assignment calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWorkload {
    private String agentId;
    private String agentName;
    private String agentEmail;

    // Current number of NOT_STARTED tickets
    private int notStartedCount;

    // Current number of IN_PROGRESS tickets
    private int inProgressCount;

    // Total active tickets (NOT_STARTED + IN_PROGRESS)
    private int totalActiveTickets;

    // Agent's current productivity score (from current week or last available week)
    private double productivityScore;

    // Assignment priority score (higher = should be assigned more tickets)
    private double assignmentPriority;
}
