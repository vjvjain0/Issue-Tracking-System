package com.ticketing.system.service;

import com.ticketing.system.dto.AddCommentRequest;
import com.ticketing.system.dto.AssignTicketRequest;
import com.ticketing.system.dto.CreateTicketRequest;
import com.ticketing.system.dto.TicketResponse;
import com.ticketing.system.dto.TicketSearchResponse;
import com.ticketing.system.dto.TicketSummaryResponse;
import com.ticketing.system.dto.UpdateStatusRequest;
import com.ticketing.system.exception.ApiException;
import com.ticketing.system.model.Activity;
import com.ticketing.system.model.Comment;
import com.ticketing.system.model.Role;
import com.ticketing.system.model.Ticket;
import com.ticketing.system.model.TicketStatus;
import com.ticketing.system.model.User;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketElasticsearchService ticketElasticsearchService;

    public TicketResponse createTicket(CreateTicketRequest request) {
        return createTicket(request, false);
    }

    public TicketResponse createTicket(CreateTicketRequest request, boolean autoAssign) {
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.NOT_STARTED)
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .comments(new ArrayList<>())
                .activities(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .autoAssigned(false)
                .build();

        Activity activity = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId("SYSTEM")
                .userName("System")
                .action("TICKET_CREATED")
                .details("Ticket created by customer: " + request.getCustomerName())
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activity);

        Ticket savedTicket = ticketRepository.save(ticket);
        ticketElasticsearchService.indexTicket(savedTicket);
        return mapToTicketResponse(savedTicket);
    }

    public TicketResponse assignTicket(String ticketId, AssignTicketRequest request, String managerId) {
        Ticket ticket = getTicketById(ticketId);
        User agent = userRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ApiException("Agent not found", HttpStatus.NOT_FOUND));

        if (agent.getRole() != Role.AGENT) {
            throw new ApiException("Can only assign tickets to agents", HttpStatus.BAD_REQUEST);
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ApiException("Manager not found", HttpStatus.NOT_FOUND));

        String previousAgent = ticket.getAssignedAgentName();
        ticket.setAssignedAgentId(agent.getId());
        ticket.setAssignedAgentName(agent.getName());
        ticket.setUpdatedAt(LocalDateTime.now());

        String details = previousAgent == null
                ? "Ticket assigned to " + agent.getName()
                : "Ticket reassigned from " + previousAgent + " to " + agent.getName();

        Activity activityLog = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId(managerId)
                .userName(manager.getName())
                .action("TICKET_ASSIGNED")
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activityLog);

        Ticket savedTicket = ticketRepository.save(ticket);
        ticketElasticsearchService.indexTicket(savedTicket);
        return mapToTicketResponse(savedTicket);
    }

    public TicketResponse updateStatus(String ticketId, UpdateStatusRequest request, String agentId) {
        Ticket ticket = getTicketById(ticketId);

        if (!agentId.equals(ticket.getAssignedAgentId())) {
            throw new ApiException("You are not authorized to update this ticket", HttpStatus.FORBIDDEN);
        }

        validateStatusTransition(ticket.getStatus(), request.getStatus());

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ApiException("Agent not found", HttpStatus.NOT_FOUND));

        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedAt(LocalDateTime.now());

        if (request.getStatus() == TicketStatus.RESOLVED || request.getStatus() == TicketStatus.INVALID) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        Activity activityLog = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId(agentId)
                .userName(agent.getName())
                .action("STATUS_CHANGED")
                .details("Status changed from " + previousStatus + " to " + request.getStatus())
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activityLog);

        Ticket savedTicket = ticketRepository.save(ticket);
        ticketElasticsearchService.indexTicket(savedTicket);
        return mapToTicketResponse(savedTicket);
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new ApiException("Ticket is already in " + currentStatus + " status", HttpStatus.BAD_REQUEST);
        }

        boolean isValidTransition = switch (currentStatus) {
            case NOT_STARTED -> newStatus == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == TicketStatus.INVALID || newStatus == TicketStatus.RESOLVED;
            case INVALID, RESOLVED -> false;
        };

        if (!isValidTransition) {
            throw new ApiException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    public TicketResponse addComment(String ticketId, AddCommentRequest request, String userId) {
        Ticket ticket = getTicketById(ticketId);

        if (!userId.equals(ticket.getAssignedAgentId())) {
            throw new ApiException("You are not authorized to comment on this ticket", HttpStatus.FORBIDDEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Comment comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .userName(user.getName())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        ticket.getComments().add(comment);
        ticket.setUpdatedAt(LocalDateTime.now());

        Activity activityLog = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .userName(user.getName())
                .action("COMMENT_ADDED")
                .details("Comment added: " + (request.getContent().length() > 50
                        ? request.getContent().substring(0, 50) + "..."
                        : request.getContent()))
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activityLog);

        Ticket savedTicket = ticketRepository.save(ticket);
        ticketElasticsearchService.indexTicket(savedTicket);
        return mapToTicketResponse(savedTicket);
    }

    public TicketResponse getTicketDetails(String ticketId, String userId) {
        Ticket ticket = getTicketById(ticketId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.getRole() == Role.AGENT && !userId.equals(ticket.getAssignedAgentId())) {
            throw new ApiException("You are not authorized to view this ticket", HttpStatus.FORBIDDEN);
        }

        return mapToTicketResponse(ticket);
    }

    // ==================== Summary Methods for List Views ====================

    public List<TicketSummaryResponse> getAllTicketSummaries() {
        return ticketRepository.findAll().stream()
                .map(this::mapToTicketSummary)
                .collect(Collectors.toList());
    }

    public List<TicketSummaryResponse> getUnassignedTicketSummaries() {
        return ticketRepository.findByAssignedAgentIdIsNull().stream()
                .map(this::mapToTicketSummary)
                .collect(Collectors.toList());
    }

    public List<TicketSummaryResponse> getTicketSummariesForAgent(String agentId) {
        return ticketRepository.findByAssignedAgentId(agentId).stream()
                .map(this::mapToTicketSummary)
                .collect(Collectors.toList());
    }

    public Map<String, List<TicketSummaryResponse>> getTicketSummariesGroupedByStatus(String agentId) {
        List<Ticket> tickets = ticketRepository.findByAssignedAgentId(agentId);

        Map<String, List<TicketSummaryResponse>> grouped = new LinkedHashMap<>();
        grouped.put("NOT_STARTED", new ArrayList<>());
        grouped.put("IN_PROGRESS", new ArrayList<>());
        grouped.put("RESOLVED", new ArrayList<>());
        grouped.put("INVALID", new ArrayList<>());

        for (Ticket ticket : tickets) {
            grouped.get(ticket.getStatus().name()).add(mapToTicketSummary(ticket));
        }

        return grouped;
    }

    public List<TicketSummaryResponse> autocompleteForAgentSummary(String agentId, String query, int limit) {
        return ticketElasticsearchService.fuzzySearchForAgent(agentId, query.trim(), limit);
    }

    public List<TicketSummaryResponse> autocompleteForManagerSummary(String query, int limit) {
        return ticketElasticsearchService.fuzzySearchAll(query.trim(), limit);
    }

    public TicketSearchResponse searchTicketsForAgent(String agentId, String query, int page, int size) {
        List<TicketSummaryResponse> tickets = ticketElasticsearchService.fuzzySearchForAgentPaged(agentId, query.trim(), page, size);
        long totalCount = ticketElasticsearchService.countFuzzySearchForAgent(agentId, query.trim());
        int totalPages = (int) Math.ceil((double) totalCount / size);

        return TicketSearchResponse.builder()
                .tickets(tickets)
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    public TicketSearchResponse searchAllTickets(String query, int page, int size) {
        List<TicketSummaryResponse> tickets = ticketElasticsearchService.fuzzySearchAllPaged(query.trim(), page, size);
        long totalCount = ticketElasticsearchService.countFuzzySearchAll(query.trim());
        int totalPages = (int) Math.ceil((double) totalCount / size);

        return TicketSearchResponse.builder()
                .tickets(tickets)
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    public long getSearchCountForAgent(String agentId, String query) {
        return ticketElasticsearchService.countFuzzySearchForAgent(agentId, query.trim());
    }

    public long getSearchCountForManager(String query) {
        return ticketElasticsearchService.countFuzzySearchAll(query.trim());
    }



    private Ticket getTicketById(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException("Ticket not found", HttpStatus.NOT_FOUND));
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
