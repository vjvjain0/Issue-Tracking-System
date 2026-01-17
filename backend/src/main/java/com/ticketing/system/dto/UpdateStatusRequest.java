package com.ticketing.system.dto;

import com.ticketing.system.model.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private TicketStatus status;
}
