package com.ticketing.system.controller;

import com.ticketing.system.dto.*;
import com.ticketing.system.security.UserPrincipal;
import com.ticketing.system.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // Public endpoint for customer app integration
    @PostMapping("/create")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.ok(response);
    }

    // Get all tickets assigned to the current agent
    @GetMapping("/my-tickets")
    public ResponseEntity<List<TicketResponse>> getMyTickets(@AuthenticationPrincipal UserPrincipal principal) {
        List<TicketResponse> tickets = ticketService.getTicketsForAgent(principal.getId());
        return ResponseEntity.ok(tickets);
    }

    // Get tickets grouped by status for the current agent
    @GetMapping("/my-tickets/grouped")
    public ResponseEntity<Map<String, List<TicketResponse>>> getMyTicketsGrouped(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, List<TicketResponse>> tickets = ticketService.getTicketsGroupedByStatus(principal.getId());
        return ResponseEntity.ok(tickets);
    }

    // Get single ticket details
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketDetails(
            @PathVariable String ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.getTicketDetails(ticketId, principal.getId());
        return ResponseEntity.ok(ticket);
    }

    // Update ticket status (agent only for their assigned tickets)
    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.updateStatus(ticketId, request, principal.getId());
        return ResponseEntity.ok(ticket);
    }

    // Add comment to ticket (agent only for their assigned tickets)
    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable String ticketId,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.addComment(ticketId, request, principal.getId());
        return ResponseEntity.ok(ticket);
    }
}
