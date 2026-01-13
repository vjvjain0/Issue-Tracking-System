package com.ticketing.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_scores")
@CompoundIndex(name = "agent_week_idx", def = "{'agentId': 1, 'weekStartDate': 1}", unique = true)
public class AgentScore {
    @Id
    private String id;

    private String agentId;

    private String agentName;

    private String agentEmail;

    // Start date of the week (Monday)
    private LocalDate weekStartDate;

    // End date of the week (Sunday)
    private LocalDate weekEndDate;

    // Number of tickets closed (RESOLVED + INVALID) during this week
    private int ticketsClosed;

    // Number of tickets resolved during this week
    private int ticketsResolved;

    // Number of tickets marked invalid during this week
    private int ticketsInvalid;

    // Productivity score (can be weighted or same as ticketsClosed)
    private double productivityScore;

    private LocalDateTime calculatedAt;
}
