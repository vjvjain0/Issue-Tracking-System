package com.ticketing.system.service;

import com.ticketing.system.model.Activity;
import com.ticketing.system.model.AgentWorkload;
import com.ticketing.system.model.Priority;
import com.ticketing.system.model.Role;
import com.ticketing.system.model.Ticket;
import com.ticketing.system.model.TicketStatus;
import com.ticketing.system.model.User;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketAutoAssignmentService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    // Priority weights for workload calculation
    private static final double HIGH_PRIORITY_WEIGHT = 0.5;
    private static final double MEDIUM_PRIORITY_WEIGHT = 0.3;
    private static final double LOW_PRIORITY_WEIGHT = 0.2;

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

            // Count tickets by priority
            int highPriorityCount = (int) activeTickets.stream()
                    .filter(t -> t.getPriority() == Priority.HIGH).count();
            int mediumPriorityCount = (int) activeTickets.stream()
                    .filter(t -> t.getPriority() == Priority.MEDIUM).count();
            int lowPriorityCount = (int) activeTickets.stream()
                    .filter(t -> t.getPriority() == Priority.LOW).count();

            int totalActive = notStarted + inProgress;
            double workloadScore = calculateWorkloadScore(highPriorityCount, mediumPriorityCount, lowPriorityCount);

            return AgentWorkload.builder()
                    .agentId(agent.getId())
                    .agentName(agent.getName())
                    .agentEmail(agent.getEmail())
                    .notStartedCount(notStarted)
                    .inProgressCount(inProgress)
                    .totalActiveTickets(totalActive)
                    .highPriorityCount(highPriorityCount)
                    .mediumPriorityCount(mediumPriorityCount)
                    .lowPriorityCount(lowPriorityCount)
                    .workloadScore(workloadScore)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Calculate workload score for an agent.
     * Lower score = should receive more tickets (less workload).
     *
     * Formula: score = 0.5*high + 0.3*medium + 0.2*low
     */
    private double calculateWorkloadScore(int highCount, int mediumCount, int lowCount) {
        return (HIGH_PRIORITY_WEIGHT * highCount) +
               (MEDIUM_PRIORITY_WEIGHT * mediumCount) +
               (LOW_PRIORITY_WEIGHT * lowCount);
    }

    /**
     * Find the best agent to assign a ticket to (for single ticket assignment).
     */
    public Optional<User> findBestAgentForAssignment() {
        List<AgentWorkload> workloads = getAgentWorkloads();

        if (workloads.isEmpty()) {
            return Optional.empty();
        }

        // Sort by workload score (ascending - lowest workload first)
        workloads.sort(Comparator.comparingDouble(AgentWorkload::getWorkloadScore));

        String bestAgentId = workloads.get(0).getAgentId();
        return userRepository.findById(bestAgentId);
    }

    /**
     * Auto-assign a single ticket to the best available agent.
     */
    public Ticket autoAssignTicket(Ticket ticket) {
        if (ticket.getPriority() == null) {
            log.warn("Cannot auto-assign ticket {} without priority", ticket.getId());
            return ticket;
        }

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
                .details("Ticket auto-assigned to " + agent.getName() + " based on current workload")
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activity);

        log.info("Auto-assigned ticket {} to agent {} ({})", ticket.getId(), agent.getName(), agent.getEmail());
        return ticketRepository.save(ticket);
    }

    /**
     * Auto-assign all unassigned tickets by priority order.
     */
    public List<Ticket> autoAssignAllUnassignedTickets() {
        List<Ticket> unassignedTickets = ticketRepository.findByAssignedAgentIdIsNull()
                .stream()
                .filter(ticket -> ticket.getPriority() != null)
                .collect(Collectors.toList());

        // Group tickets by priority
        Map<Priority, List<Ticket>> ticketsByPriority = unassignedTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getPriority));

        List<Ticket> assignedTickets = new ArrayList<>();

        // Assign in priority order: HIGH, MEDIUM, LOW
        Priority[] priorityOrder = {Priority.HIGH, Priority.MEDIUM, Priority.LOW};

        for (Priority priority : priorityOrder) {
            List<Ticket> priorityTickets = ticketsByPriority.getOrDefault(priority, new ArrayList<>());
            if (!priorityTickets.isEmpty()) {
                List<Ticket> assigned = assignTicketsByPriority(priorityTickets);
                assignedTickets.addAll(assigned);
            }
        }

        log.info("Auto-assigned {} tickets out of {} unassigned tickets with priorities",
                assignedTickets.size(), unassignedTickets.size());
        return assignedTickets;
    }

    /**
     * Assign tickets of a specific priority to agents with lowest workload first.
     */
    private List<Ticket> assignTicketsByPriority(List<Ticket> tickets) {
        List<Ticket> assignedTickets = new ArrayList<>();
        List<AgentWorkload> agentWorkloads = getAgentWorkloads();

        if (agentWorkloads.isEmpty()) {
            log.warn("No agents available for auto-assignment");
            return assignedTickets;
        }

        // Sort agents by workload score (lowest first)
        agentWorkloads.sort(Comparator.comparingDouble(AgentWorkload::getWorkloadScore));

        int agentIndex = 0;
        for (Ticket ticket : tickets) {
            if (agentIndex >= agentWorkloads.size()) {
                // Reset to first agent if we've cycled through all
                agentIndex = 0;
            }

            AgentWorkload workload = agentWorkloads.get(agentIndex);
            Optional<User> agentOpt = userRepository.findById(workload.getAgentId());

            if (agentOpt.isPresent()) {
                User agent = agentOpt.get();
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
                        .details("Ticket auto-assigned to " + agent.getName() + " based on current workload")
                        .timestamp(LocalDateTime.now())
                        .build();
                ticket.getActivities().add(activity);

                ticketRepository.save(ticket);
                assignedTickets.add(ticket);

                log.info("Auto-assigned {} priority ticket {} to agent {} (workload: {})",
                        ticket.getPriority(), ticket.getId(), agent.getName(), workload.getWorkloadScore());

                // Move to next agent for round-robin distribution
                agentIndex++;
            }
        }

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
