package com.ticketing.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDetailResponse {
    private String agentId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String employeeId;
    
    // Epoch milliseconds for timezone-safe handling
    private Long lastActiveAt;
    
    // Ticket stats
    private int notStartedCount;
    private int inProgressCount;
    private int closedCount;  // resolved + invalid
    
    private double productivityScore;
}
