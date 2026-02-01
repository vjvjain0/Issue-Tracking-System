package com.ticketing.system.service;

import com.ticketing.system.dto.AgentDetailResponse;
import com.ticketing.system.dto.UserResponse;
import com.ticketing.system.exception.ApiException;
import com.ticketing.system.model.*;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    // Priority weights for workload calculation
    private static final double HIGH_PRIORITY_WEIGHT = 0.5;
    private static final double MEDIUM_PRIORITY_WEIGHT = 0.3;
    private static final double LOW_PRIORITY_WEIGHT = 0.2;

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    public List<UserResponse> getAllAgents() {
        return userRepository.findByRole(Role.AGENT).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getCurrentUser(String userId) {
        User user = getUserById(userId);
        return mapToUserResponse(user);
    }

    public void updateHeartbeat(String userId) {
        User user = getUserById(userId);
        user.setLastActiveAt(System.currentTimeMillis());
        userRepository.save(user);
    }

    public AgentDetailResponse getAgentDetails(String agentId) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ApiException("Agent not found", HttpStatus.NOT_FOUND));

        if (agent.getRole() != Role.AGENT) {
            throw new ApiException("User is not an agent", HttpStatus.BAD_REQUEST);
        }

        // Get ticket counts
        List<TicketStatus> activeStatuses = Arrays.asList(TicketStatus.NOT_STARTED, TicketStatus.IN_PROGRESS);
        List<TicketStatus> closedStatuses = Arrays.asList(TicketStatus.RESOLVED, TicketStatus.INVALID);

        List<Ticket> activeTickets = ticketRepository
                .findByAssignedAgentIdAndStatusIn(agentId, activeStatuses);
        List<Ticket> closedTickets = ticketRepository
                .findByAssignedAgentIdAndStatusIn(agentId, closedStatuses);

        int notStarted = (int) activeTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.NOT_STARTED).count();
        int inProgress = (int) activeTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS).count();
        int closed = closedTickets.size();

        // Calculate priority-based workload score
        int highPriorityCount = (int) activeTickets.stream()
                .filter(t -> t.getPriority() == Priority.HIGH).count();
        int mediumPriorityCount = (int) activeTickets.stream()
                .filter(t -> t.getPriority() == Priority.MEDIUM).count();
        int lowPriorityCount = (int) activeTickets.stream()
                .filter(t -> t.getPriority() == Priority.LOW).count();

        double workloadScore = (HIGH_PRIORITY_WEIGHT * highPriorityCount) +
                              (MEDIUM_PRIORITY_WEIGHT * mediumPriorityCount) +
                              (LOW_PRIORITY_WEIGHT * lowPriorityCount);

        return AgentDetailResponse.builder()
                .agentId(agent.getId())
                .fullName(agent.getName())
                .email(agent.getEmail())
                .phoneNumber(agent.getPhoneNumber())
                .employeeId(agent.getEmployeeId())
                .lastActiveAt(agent.getLastActiveAt())
                .notStartedCount(notStarted)
                .inProgressCount(inProgress)
                .closedCount(closed)
                .workloadScore(workloadScore)
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .employeeId(user.getEmployeeId())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
