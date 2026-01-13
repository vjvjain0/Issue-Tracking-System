package com.ticketing.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoAssignResponse {
    private int ticketsAssigned;
    private int ticketsFailed;
    private List<TicketResponse> assignedTickets;
    private String message;
}
