package com.ticketing.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWorkloadResponse {
    private String agentId;
    private String agentName;
    private String agentEmail;
    private int notStartedCount;
    private int inProgressCount;
    private int totalActiveTickets;
    private int highPriorityCount;
    private int mediumPriorityCount;
    private int lowPriorityCount;
    private double workloadScore;
}
