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
public class TicketSearchResponse {
    private List<TicketResponse> tickets;
    private long totalCount;
    private int page;
    private int size;
    private int totalPages;
}
