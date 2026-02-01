package com.ticketing.system.dto;

import com.ticketing.system.model.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePriorityRequest {
    @NotNull(message = "Priority is required")
    private Priority priority;
}