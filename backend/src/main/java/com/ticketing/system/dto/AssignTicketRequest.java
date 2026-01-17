package com.ticketing.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignTicketRequest {
    @NotBlank(message = "Agent ID is required")
    private String agentId;
}
