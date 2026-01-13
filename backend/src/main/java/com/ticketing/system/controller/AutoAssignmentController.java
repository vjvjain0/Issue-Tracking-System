package com.ticketing.system.controller;

import com.ticketing.system.dto.*;
import com.ticketing.system.model.AgentScore;
import com.ticketing.system.model.AgentWorkload;
import com.ticketing.system.model.Ticket;
import com.ticketing.system.service.AgentScoreService;
import com.ticketing.system.service.TicketAutoAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager/auto-assign")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class AutoAssignmentController {

    private final TicketAutoAssignmentService autoAssignmentService;
    private final AgentScoreService agentScoreService;

    /**
     * Auto-assign all unassigned tickets.
     */
    @PostMapping("/all")
    public ResponseEntity<AutoAssignResponse> autoAssignAllTickets() {
        List<Ticket> assigned = autoAssignmentService.autoAssignAllUnassignedTickets();

        List<TicketResponse> ticketResponses = assigned.stream()
                .map(this::mapToTicketResponse)
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
     * Get agent workload information for auto-assignment.
     */
    @GetMapping("/workloads")
    public ResponseEntity<List<AgentWorkloadResponse>> getAgentWorkloads() {
        List<AgentWorkload> workloads = autoAssignmentService.getAgentWorkloads();

        List<AgentWorkloadResponse> responses = workloads.stream()
                .map(this::mapToWorkloadResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get assignment statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAssignmentStats() {
        Map<String, Object> stats = autoAssignmentService.getAssignmentStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get current week agent scores.
     */
    @GetMapping("/scores/current")
    public ResponseEntity<List<AgentScoreResponse>> getCurrentWeekScores() {
        List<AgentScore> scores = agentScoreService.getCurrentWeekScores();

        List<AgentScoreResponse> responses = scores.stream()
                .map(this::mapToScoreResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get score history for last N weeks.
     */
    @GetMapping("/scores/history")
    public ResponseEntity<List<AgentScoreResponse>> getScoreHistory(
            @RequestParam(defaultValue = "4") int weeks) {
        List<AgentScore> scores = agentScoreService.getScoresForLastNWeeks(weeks);

        List<AgentScoreResponse> responses = scores.stream()
                .map(this::mapToScoreResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    private TicketResponse mapToTicketResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .assignedAgentId(ticket.getAssignedAgentId())
                .assignedAgentName(ticket.getAssignedAgentName())
                .customerEmail(ticket.getCustomerEmail())
                .customerName(ticket.getCustomerName())
                .comments(ticket.getComments())
                .activities(ticket.getActivities())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .closedAt(ticket.getClosedAt())
                .autoAssigned(ticket.isAutoAssigned())
                .build();
    }

    private AgentWorkloadResponse mapToWorkloadResponse(AgentWorkload workload) {
        return AgentWorkloadResponse.builder()
                .agentId(workload.getAgentId())
                .agentName(workload.getAgentName())
                .agentEmail(workload.getAgentEmail())
                .notStartedCount(workload.getNotStartedCount())
                .inProgressCount(workload.getInProgressCount())
                .totalActiveTickets(workload.getTotalActiveTickets())
                .productivityScore(workload.getProductivityScore())
                .assignmentPriority(workload.getAssignmentPriority())
                .build();
    }

    private AgentScoreResponse mapToScoreResponse(AgentScore score) {
        return AgentScoreResponse.builder()
                .id(score.getId())
                .agentId(score.getAgentId())
                .agentName(score.getAgentName())
                .agentEmail(score.getAgentEmail())
                .weekStartDate(score.getWeekStartDate())
                .weekEndDate(score.getWeekEndDate())
                .ticketsClosed(score.getTicketsClosed())
                .ticketsResolved(score.getTicketsResolved())
                .ticketsInvalid(score.getTicketsInvalid())
                .productivityScore(score.getProductivityScore())
                .calculatedAt(score.getCalculatedAt())
                .build();
    }
}
