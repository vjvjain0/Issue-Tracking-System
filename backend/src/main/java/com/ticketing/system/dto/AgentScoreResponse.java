package com.ticketing.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentScoreResponse {
    private String id;
    private String agentId;
    private String agentName;
    private String agentEmail;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private int ticketsClosed;
    private int ticketsResolved;
    private int ticketsInvalid;
    private double productivityScore;
    private LocalDateTime calculatedAt;
}
