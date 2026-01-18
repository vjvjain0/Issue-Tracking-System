package com.ticketing.system.controller;

import com.ticketing.system.dto.*;
import com.ticketing.system.model.Ticket;
import com.ticketing.system.security.UserPrincipal;
import com.ticketing.system.service.TicketAutoAssignmentService;
import com.ticketing.system.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketAutoAssignmentService autoAssignmentService;

    /**
     * Create a new ticket (public endpoint for customer app integration)
     * POST /api/v1/tickets
     */
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get tickets - behavior based on role and query parameters
     * GET /api/v1/tickets - Get all tickets (manager) or assigned tickets (agent)
     * GET /api/v1/tickets?assigned=false - Get unassigned tickets (manager only)
     * GET /api/v1/tickets?query=searchText - Search tickets
     * GET /api/v1/tickets?grouped=true - Get tickets grouped by status (agent only)
     * 
     * Returns lightweight TicketSummaryResponse for list views
     */
    @GetMapping
    public ResponseEntity<?> getTickets(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean assigned,
            @RequestParam(required = false) Boolean grouped,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        boolean isManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        // Search tickets
        if (query != null && !query.trim().isEmpty()) {
            if (isManager) {
                TicketSearchResponse response = ticketService.searchAllTickets(query, page, size);
                return ResponseEntity.ok(response);
            } else {
                TicketSearchResponse response = ticketService.searchTicketsForAgent(principal.getId(), query, page, size);
                return ResponseEntity.ok(response);
            }
        }

        // Get unassigned tickets (manager only)
        if (assigned != null && !assigned) {
            if (!isManager) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            List<TicketSummaryResponse> tickets = ticketService.getUnassignedTicketSummaries();
            return ResponseEntity.ok(tickets);
        }

        // Get tickets grouped by status (agent only)
        if (grouped != null && grouped && !isManager) {
            Map<String, List<TicketSummaryResponse>> tickets = ticketService.getTicketSummariesGroupedByStatus(principal.getId());
            return ResponseEntity.ok(tickets);
        }

        // Get all tickets based on role
        if (isManager) {
            List<TicketSummaryResponse> tickets = ticketService.getAllTicketSummaries();
            return ResponseEntity.ok(tickets);
        } else {
            List<TicketSummaryResponse> tickets = ticketService.getTicketSummariesForAgent(principal.getId());
            return ResponseEntity.ok(tickets);
        }
    }

    /**
     * Autocomplete search for tickets
     * GET /api/v1/tickets/autocomplete?query=searchText
     * 
     * Returns lightweight TicketSummaryResponse for dropdown
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<Map<String, Object>> autocompleteSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        boolean isManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        List<TicketSummaryResponse> tickets;
        long totalCount;

        if (isManager) {
            tickets = ticketService.autocompleteForManagerSummary(query, limit);
            totalCount = ticketService.getSearchCountForManager(query);
        } else {
            tickets = ticketService.autocompleteForAgentSummary(principal.getId(), query, limit);
            totalCount = ticketService.getSearchCountForAgent(principal.getId(), query);
        }

        return ResponseEntity.ok(Map.of(
            "tickets", tickets,
            "totalCount", totalCount
        ));
    }

    /**
     * Auto-assign all unassigned tickets (manager only)
     * POST /api/v1/tickets/auto-assign
     */
    @PostMapping("/auto-assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AutoAssignResponse> autoAssignAllTickets() {
        List<Ticket> assigned = autoAssignmentService.autoAssignAllUnassignedTickets();

        List<TicketSummaryResponse> ticketResponses = assigned.stream()
                .map(this::mapToTicketSummary)
                .collect(Collectors.toList());

        AutoAssignResponse response = AutoAssignResponse.builder()
                .ticketsAssigned(assigned.size())
                .ticketsFailed(0)
                .assignedTickets(ticketResponses)
                .message(assigned.isEmpty()
                        ? "No unassigned tickets to assign"
                        : "Successfully auto-assigned " + assigned.size() + " tickets")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get single ticket details (full details including comments and activities)
     * GET /api/v1/tickets/{ticketId}
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketDetails(
            @PathVariable String ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.getTicketDetails(ticketId, principal.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Update ticket status (agent only for their assigned tickets)
     * PATCH /api/v1/tickets/{ticketId}/status
     */
    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.updateStatus(ticketId, request, principal.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Add comment to ticket (agent only for their assigned tickets)
     * POST /api/v1/tickets/{ticketId}/comments
     */
    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable String ticketId,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.addComment(ticketId, request, principal.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Assign ticket to agent (manager only)
     * PATCH /api/v1/tickets/{ticketId}/assign
     */
    @PatchMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.assignTicket(ticketId, request, principal.getId());
        return ResponseEntity.ok(ticket);
    }

    private TicketSummaryResponse mapToTicketSummary(Ticket ticket) {
        return TicketSummaryResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .assignedAgentId(ticket.getAssignedAgentId())
                .assignedAgentName(ticket.getAssignedAgentName())
                .customerName(ticket.getCustomerName())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
