package com.ticketing.system.controller;

import com.ticketing.system.dto.*;
import com.ticketing.system.model.AgentScore;
import com.ticketing.system.model.AgentWorkload;
import com.ticketing.system.service.AgentScoreService;
import com.ticketing.system.service.TicketAutoAssignmentService;
import com.ticketing.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class AgentController {

    private final TicketAutoAssignmentService autoAssignmentService;
    private final AgentScoreService agentScoreService;
    private final UserService userService;

    /**
     * Get all agents
     * GET /api/v1/agents
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllAgents() {
        List<UserResponse> agents = userService.getAllAgents();
        return ResponseEntity.ok(agents);
    }

    /**
     * Get detailed information for a specific agent
     * GET /api/v1/agents/{agentId}
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentDetailResponse> getAgentDetails(@PathVariable String agentId) {
        AgentDetailResponse agentDetails = userService.getAgentDetails(agentId);
        return ResponseEntity.ok(agentDetails);
    }

    /**
     * Get workload information for all agents
     * GET /api/v1/agents/workloads
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
     * Get workload information for a specific agent
     * GET /api/v1/agents/{agentId}/workload
     */
    @GetMapping("/{agentId}/workload")
    public ResponseEntity<AgentWorkloadResponse> getAgentWorkload(@PathVariable String agentId) {
        List<AgentWorkload> workloads = autoAssignmentService.getAgentWorkloads();

        Optional<AgentWorkload> agentWorkload = workloads.stream()
                .filter(w -> w.getAgentId().equals(agentId))
                .findFirst();

        if (agentWorkload.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mapToWorkloadResponse(agentWorkload.get()));
    }

    /**
     * Get scores for all agents (current week or history)
     * GET /api/v1/agents/scores
     * GET /api/v1/agents/scores?weeks=4 - Get score history for last N weeks
     */
    @GetMapping("/scores")
    public ResponseEntity<List<AgentScoreResponse>> getAgentScores(
            @RequestParam(required = false, defaultValue = "1") int weeks) {
        List<AgentScore> scores;
        
        if (weeks <= 1) {
            scores = agentScoreService.getCurrentWeekScores();
        } else {
            scores = agentScoreService.getScoresForLastNWeeks(weeks);
        }

        List<AgentScoreResponse> responses = scores.stream()
                .map(this::mapToScoreResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get score for a specific agent
     * GET /api/v1/agents/{agentId}/score
     */
    @GetMapping("/{agentId}/score")
    public ResponseEntity<AgentScoreResponse> getAgentScore(@PathVariable String agentId) {
        Optional<AgentScore> score = agentScoreService.getLatestScoreForAgent(agentId);

        if (score.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mapToScoreResponse(score.get()));
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
