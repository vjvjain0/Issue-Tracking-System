package com.ticketing.system.controller;

import com.ticketing.system.dto.*;
import com.ticketing.system.security.UserPrincipal;
import com.ticketing.system.service.TicketService;
import com.ticketing.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final TicketService ticketService;
    private final UserService userService;

    // Get all tickets
    @GetMapping("/tickets")
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    // Get unassigned tickets
    @GetMapping("/tickets/unassigned")
    public ResponseEntity<List<TicketResponse>> getUnassignedTickets() {
        List<TicketResponse> tickets = ticketService.getUnassignedTickets();
        return ResponseEntity.ok(tickets);
    }

    // Assign ticket to agent
    @PatchMapping("/tickets/{ticketId}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.assignTicket(ticketId, request, principal.getId());
        return ResponseEntity.ok(ticket);
    }

    // Get all agents
    @GetMapping("/agents")
    public ResponseEntity<List<UserResponse>> getAllAgents() {
        List<UserResponse> agents = userService.getAllAgents();
        return ResponseEntity.ok(agents);
    }

    // Get ticket details
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketDetails(
            @PathVariable String ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketResponse ticket = ticketService.getTicketDetails(ticketId, principal.getId());
        return ResponseEntity.ok(ticket);
    }
}
