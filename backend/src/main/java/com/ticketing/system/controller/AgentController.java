package com.ticketing.system.controller;

import com.ticketing.system.dto.AgentDetailResponse;
import com.ticketing.system.dto.AgentWorkloadResponse;
import com.ticketing.system.dto.UserResponse;
import com.ticketing.system.model.AgentWorkload;
import com.ticketing.system.service.TicketAutoAssignmentService;
import com.ticketing.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class AgentController {

    private final TicketAutoAssignmentService autoAssignmentService;
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



    private AgentWorkloadResponse mapToWorkloadResponse(AgentWorkload workload) {
        return AgentWorkloadResponse.builder()
                .agentId(workload.getAgentId())
                .agentName(workload.getAgentName())
                .agentEmail(workload.getAgentEmail())
                .notStartedCount(workload.getNotStartedCount())
                .inProgressCount(workload.getInProgressCount())
                .totalActiveTickets(workload.getTotalActiveTickets())
                .highPriorityCount(workload.getHighPriorityCount())
                .mediumPriorityCount(workload.getMediumPriorityCount())
                .lowPriorityCount(workload.getLowPriorityCount())
                .workloadScore(workload.getWorkloadScore())
                .build();
    }
}
