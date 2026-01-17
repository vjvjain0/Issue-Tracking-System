package com.ticketing.system.service;

import com.ticketing.system.model.*;
import com.ticketing.system.repository.AgentScoreRepository;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketAutoAssignmentService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AgentScoreRepository agentScoreRepository;
    private final AgentScoreService agentScoreService;

    // Weight factors for auto-assignment algorithm
    private static final double WORKLOAD_WEIGHT = 0.6;  // Higher weight = workload matters more
    private static final double SCORE_WEIGHT = 0.4;     // Higher weight = productivity matters more
    private static final double BASE_SCORE = 1.0;       // Base score for agents with no history

    /**
     * Get workload information for all agents.
     */
    public List<AgentWorkload> getAgentWorkloads() {
        List<User> agents = userRepository.findByRole(Role.AGENT);
        List<TicketStatus> activeStatuses = Arrays.asList(TicketStatus.NOT_STARTED, TicketStatus.IN_PROGRESS);

        return agents.stream().map(agent -> {
            List<Ticket> activeTickets = ticketRepository
                    .findByAssignedAgentIdAndStatusIn(agent.getId(), activeStatuses);

            int notStarted = (int) activeTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.NOT_STARTED).count();
            int inProgress = (int) activeTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS).count();

            // Get latest productivity score
            double productivityScore = agentScoreRepository
                    .findTopByAgentIdOrderByWeekStartDateDesc(agent.getId())
                    .map(AgentScore::getProductivityScore)
                    .orElse(BASE_SCORE);

            int totalActive = notStarted + inProgress;
            double assignmentPriority = calculateAssignmentPriority(totalActive, productivityScore);

            return AgentWorkload.builder()
                    .agentId(agent.getId())
                    .agentName(agent.getName())
                    .agentEmail(agent.getEmail())
                    .notStartedCount(notStarted)
                    .inProgressCount(inProgress)
                    .totalActiveTickets(totalActive)
                    .productivityScore(productivityScore)
                    .assignmentPriority(assignmentPriority)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Calculate assignment priority for an agent.
     * Higher priority = should receive more tickets.
     *
     * Formula: priority = (scoreWeight * normalizedScore) - (workloadWeight * normalizedWorkload)
     *
     * The idea is:
     * - Lower workload = higher priority (agent has capacity)
     * - Higher productivity score = higher priority (agent is efficient)
     */
    private double calculateAssignmentPriority(int totalActiveTickets, double productivityScore) {
        // Normalize workload (inverse - lower workload = higher value)
        // Using 1 / (1 + workload) to get a value between 0 and 1
        double normalizedWorkload = 1.0 / (1.0 + totalActiveTickets);

        // Normalize score (assuming max practical score is around 20 tickets/week)
        double normalizedScore = Math.min(productivityScore / 20.0, 1.0);

        // Combine with a base factor to ensure new agents (no score) still get assigned
        double priority = (WORKLOAD_WEIGHT * normalizedWorkload) +
                         (SCORE_WEIGHT * (normalizedScore + 0.1)); // +0.1 base to not penalize new agents

        return Math.round(priority * 1000.0) / 1000.0; // Round to 3 decimal places
    }

    /**
     * Find the best agent to assign a ticket to.
     */
    public Optional<User> findBestAgentForAssignment() {
        List<AgentWorkload> workloads = getAgentWorkloads();

        if (workloads.isEmpty()) {
            return Optional.empty();
        }

        // Sort by assignment priority (descending - highest priority first)
        workloads.sort((a, b) -> Double.compare(b.getAssignmentPriority(), a.getAssignmentPriority()));

        String bestAgentId = workloads.get(0).getAgentId();
        return userRepository.findById(bestAgentId);
    }

    /**
     * Auto-assign a single ticket to the best available agent.
     */
    public Ticket autoAssignTicket(Ticket ticket) {
        Optional<User> bestAgent = findBestAgentForAssignment();

        if (bestAgent.isEmpty()) {
            log.warn("No agents available for auto-assignment");
            return ticket;
        }

        User agent = bestAgent.get();
        ticket.setAssignedAgentId(agent.getId());
        ticket.setAssignedAgentName(agent.getName());
        ticket.setAutoAssigned(true);
        ticket.setUpdatedAt(LocalDateTime.now());

        // Add activity
        Activity activity = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId("SYSTEM")
                .userName("System")
                .action("TICKET_AUTO_ASSIGNED")
                .details("Ticket auto-assigned to " + agent.getName() + " based on workload and productivity score")
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activity);

        log.info("Auto-assigned ticket {} to agent {} ({})", ticket.getId(), agent.getName(), agent.getEmail());
        return ticketRepository.save(ticket);
    }

    /**
     * Auto-assign all unassigned tickets.
     */
    public List<Ticket> autoAssignAllUnassignedTickets() {
        List<Ticket> unassignedTickets = ticketRepository.findByAssignedAgentIdIsNull();
        List<Ticket> assignedTickets = new ArrayList<>();

        for (Ticket ticket : unassignedTickets) {
            Ticket assigned = autoAssignTicket(ticket);
            if (assigned.getAssignedAgentId() != null) {
                assignedTickets.add(assigned);
            }
        }

        log.info("Auto-assigned {} tickets out of {} unassigned tickets",
                assignedTickets.size(), unassignedTickets.size());
        return assignedTickets;
    }

    /**
     * Get assignment statistics for dashboard.
     */
    public Map<String, Object> getAssignmentStats() {
        List<AgentWorkload> workloads = getAgentWorkloads();
        long unassignedCount = ticketRepository.findByAssignedAgentIdIsNull().size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("agentWorkloads", workloads);
        stats.put("unassignedTicketsCount", unassignedCount);
        stats.put("totalAgents", workloads.size());
        stats.put("totalActiveTickets", workloads.stream().mapToInt(AgentWorkload::getTotalActiveTickets).sum());

        return stats;
    }
}
