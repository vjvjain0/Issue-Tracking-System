package com.ticketing.system.service;

import com.ticketing.system.dto.*;
import com.ticketing.system.exception.ApiException;
import com.ticketing.system.model.*;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.repository.UserRepository;
import com.ticketing.system.util.FuzzySearchUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

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

        // Add creation activity
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

        // Add assignment activity
        String details = previousAgent == null
                ? "Ticket assigned to " + agent.getName()
                : "Ticket reassigned from " + previousAgent + " to " + agent.getName();

        Activity activity = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId(managerId)
                .userName(manager.getName())
                .action("TICKET_ASSIGNED")
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activity);

        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToTicketResponse(savedTicket);
    }

    public TicketResponse updateStatus(String ticketId, UpdateStatusRequest request, String agentId) {
        Ticket ticket = getTicketById(ticketId);

        // Verify the agent is assigned to this ticket
        if (!agentId.equals(ticket.getAssignedAgentId())) {
            throw new ApiException("You are not authorized to update this ticket", HttpStatus.FORBIDDEN);
        }

        // Validate status transition
        validateStatusTransition(ticket.getStatus(), request.getStatus());

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ApiException("Agent not found", HttpStatus.NOT_FOUND));

        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedAt(LocalDateTime.now());

        // Set closedAt timestamp if ticket is being closed (RESOLVED or INVALID)
        if (request.getStatus() == TicketStatus.RESOLVED || request.getStatus() == TicketStatus.INVALID) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        // Add status change activity
        Activity activity = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId(agentId)
                .userName(agent.getName())
                .action("STATUS_CHANGED")
                .details("Status changed from " + previousStatus + " to " + request.getStatus())
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activity);

        Ticket savedTicket = ticketRepository.save(ticket);
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

        // Verify the user is assigned to this ticket
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

        // Add comment activity
        Activity activity = Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .userName(user.getName())
                .action("COMMENT_ADDED")
                .details("Comment added: " + (request.getContent().length() > 50
                        ? request.getContent().substring(0, 50) + "..."
                        : request.getContent()))
                .timestamp(LocalDateTime.now())
                .build();
        ticket.getActivities().add(activity);

        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToTicketResponse(savedTicket);
    }

    public List<TicketResponse> getTicketsForAgent(String agentId) {
        return ticketRepository.findByAssignedAgentId(agentId).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    public Map<String, List<TicketResponse>> getTicketsGroupedByStatus(String agentId) {
        List<Ticket> tickets = ticketRepository.findByAssignedAgentId(agentId);

        Map<String, List<TicketResponse>> grouped = new LinkedHashMap<>();
        grouped.put("NOT_STARTED", new ArrayList<>());
        grouped.put("IN_PROGRESS", new ArrayList<>());
        grouped.put("RESOLVED", new ArrayList<>());
        grouped.put("INVALID", new ArrayList<>());

        for (Ticket ticket : tickets) {
            grouped.get(ticket.getStatus().name()).add(mapToTicketResponse(ticket));
        }

        return grouped;
    }

    public TicketResponse getTicketDetails(String ticketId, String userId) {
        Ticket ticket = getTicketById(ticketId);

        // Check if user is assigned agent or manager
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.getRole() == Role.AGENT && !userId.equals(ticket.getAssignedAgentId())) {
            throw new ApiException("You are not authorized to view this ticket", HttpStatus.FORBIDDEN);
        }

        return mapToTicketResponse(ticket);
    }

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getUnassignedTickets() {
        return ticketRepository.findByAssignedAgentIdIsNull().stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    // Search tickets for agents (only their assigned tickets) with fuzzy matching
    public TicketSearchResponse searchTicketsForAgent(String agentId, String query, int page, int size) {
        String trimmedQuery = query.trim();
        String escapedQuery = escapeRegex(trimmedQuery);
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Collect all matching tickets
        Set<String> seenIds = new HashSet<>();
        List<Ticket> exactMatches = new ArrayList<>();
        List<Ticket> fuzzyMatches = new ArrayList<>();
        
        // 1. First, check for exact ID match
        ticketRepository.findByIdAndAssignedAgentId(trimmedQuery, agentId)
                .ifPresent(ticket -> {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                });
        
        // 2. Check for partial ID match (if query looks like part of an ID)
        if (isValidIdPattern(trimmedQuery)) {
            List<Ticket> agentTickets = ticketRepository.findByAssignedAgentId(agentId);
            for (Ticket ticket : agentTickets) {
                if (!seenIds.contains(ticket.getId()) && 
                    ticket.getId().toLowerCase().contains(trimmedQuery.toLowerCase())) {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        // 3. Search by title and description (exact regex match)
        List<Ticket> textResults = ticketRepository.searchByAgentIdTextFields(agentId, escapedQuery, pageable);
        for (Ticket ticket : textResults) {
            if (!seenIds.contains(ticket.getId())) {
                exactMatches.add(ticket);
                seenIds.add(ticket.getId());
            }
        }
        
        // 4. Fuzzy search - check remaining tickets for fuzzy matches
        double fuzzyThreshold = FuzzySearchUtil.getDefaultThreshold();
        List<Ticket> agentTickets = ticketRepository.findByAssignedAgentId(agentId);
        for (Ticket ticket : agentTickets) {
            if (!seenIds.contains(ticket.getId())) {
                boolean titleMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getTitle(), fuzzyThreshold);
                boolean descMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getDescription(), fuzzyThreshold);
                if (titleMatch || descMatch) {
                    fuzzyMatches.add(ticket);
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        // Sort fuzzy matches by relevance score
        fuzzyMatches.sort((a, b) -> {
            double scoreA = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, a.getTitle(), a.getDescription());
            double scoreB = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, b.getTitle(), b.getDescription());
            return Double.compare(scoreB, scoreA);
        });
        
        // Combine results: exact matches first (sorted by date), then fuzzy matches (sorted by relevance)
        exactMatches.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        List<Ticket> allResults = new ArrayList<>(exactMatches);
        allResults.addAll(fuzzyMatches);
        
        // Apply pagination
        long totalCount = allResults.size();
        int totalPages = (int) Math.ceil((double) totalCount / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allResults.size());
        
        List<Ticket> pagedResults = fromIndex < allResults.size() 
                ? allResults.subList(fromIndex, toIndex) 
                : new ArrayList<>();

        return TicketSearchResponse.builder()
                .tickets(pagedResults.stream().map(this::mapToTicketResponse).collect(Collectors.toList()))
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    // Search all tickets for managers with fuzzy matching
    public TicketSearchResponse searchAllTickets(String query, int page, int size) {
        String trimmedQuery = query.trim();
        String escapedQuery = escapeRegex(trimmedQuery);
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Collect all matching tickets
        Set<String> seenIds = new HashSet<>();
        List<Ticket> exactMatches = new ArrayList<>();
        List<Ticket> fuzzyMatches = new ArrayList<>();
        
        // 1. First, check for exact ID match
        ticketRepository.findById(trimmedQuery)
                .ifPresent(ticket -> {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                });
        
        // 2. Check for partial ID match (if query looks like part of an ID)
        if (isValidIdPattern(trimmedQuery)) {
            List<Ticket> allTickets = ticketRepository.findAll();
            for (Ticket ticket : allTickets) {
                if (!seenIds.contains(ticket.getId()) && 
                    ticket.getId().toLowerCase().contains(trimmedQuery.toLowerCase())) {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        // 3. Search by title and description (exact regex match)
        List<Ticket> textResults = ticketRepository.searchAllTicketsTextFields(escapedQuery, pageable);
        for (Ticket ticket : textResults) {
            if (!seenIds.contains(ticket.getId())) {
                exactMatches.add(ticket);
                seenIds.add(ticket.getId());
            }
        }
        
        // 4. Fuzzy search - check remaining tickets for fuzzy matches
        double fuzzyThreshold = FuzzySearchUtil.getDefaultThreshold();
        List<Ticket> allTickets = ticketRepository.findAll();
        for (Ticket ticket : allTickets) {
            if (!seenIds.contains(ticket.getId())) {
                boolean titleMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getTitle(), fuzzyThreshold);
                boolean descMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getDescription(), fuzzyThreshold);
                if (titleMatch || descMatch) {
                    fuzzyMatches.add(ticket);
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        // Sort fuzzy matches by relevance score
        fuzzyMatches.sort((a, b) -> {
            double scoreA = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, a.getTitle(), a.getDescription());
            double scoreB = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, b.getTitle(), b.getDescription());
            return Double.compare(scoreB, scoreA);
        });
        
        // Combine results: exact matches first, then fuzzy matches
        exactMatches.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        List<Ticket> allResults = new ArrayList<>(exactMatches);
        allResults.addAll(fuzzyMatches);
        
        // Apply pagination
        long totalCount = allResults.size();
        int totalPages = (int) Math.ceil((double) totalCount / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allResults.size());
        
        List<Ticket> pagedResults = fromIndex < allResults.size() 
                ? allResults.subList(fromIndex, toIndex) 
                : new ArrayList<>();

        return TicketSearchResponse.builder()
                .tickets(pagedResults.stream().map(this::mapToTicketResponse).collect(Collectors.toList()))
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    // Autocomplete search for agents (limited results) with fuzzy matching
    public List<TicketResponse> autocompleteForAgent(String agentId, String query, int limit) {
        String trimmedQuery = query.trim();
        String escapedQuery = escapeRegex(trimmedQuery);
        Pageable pageable = PageRequest.of(0, limit * 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Collect all matching tickets
        Set<String> seenIds = new HashSet<>();
        List<Ticket> exactMatches = new ArrayList<>();
        List<Ticket> fuzzyMatches = new ArrayList<>();
        
        // 1. First, check for exact ID match
        ticketRepository.findByIdAndAssignedAgentId(trimmedQuery, agentId)
                .ifPresent(ticket -> {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                });
        
        // 2. Check for partial ID match
        if (isValidIdPattern(trimmedQuery) && exactMatches.size() < limit) {
            List<Ticket> agentTickets = ticketRepository.findByAssignedAgentId(agentId);
            for (Ticket ticket : agentTickets) {
                if (!seenIds.contains(ticket.getId()) && 
                    ticket.getId().toLowerCase().contains(trimmedQuery.toLowerCase())) {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                    if (exactMatches.size() >= limit) break;
                }
            }
        }
        
        // 3. Search by title and description
        if (exactMatches.size() < limit) {
            List<Ticket> textResults = ticketRepository.searchByAgentIdTextFields(agentId, escapedQuery, pageable);
            for (Ticket ticket : textResults) {
                if (!seenIds.contains(ticket.getId())) {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                    if (exactMatches.size() >= limit) break;
                }
            }
        }
        
        // 4. Fuzzy search if we still need more results
        if (exactMatches.size() < limit) {
            double fuzzyThreshold = FuzzySearchUtil.getDefaultThreshold();
            List<Ticket> agentTickets = ticketRepository.findByAssignedAgentId(agentId);
            for (Ticket ticket : agentTickets) {
                if (!seenIds.contains(ticket.getId())) {
                    boolean titleMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getTitle(), fuzzyThreshold);
                    boolean descMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getDescription(), fuzzyThreshold);
                    if (titleMatch || descMatch) {
                        fuzzyMatches.add(ticket);
                        seenIds.add(ticket.getId());
                    }
                }
            }
            
            // Sort fuzzy matches by relevance
            fuzzyMatches.sort((a, b) -> {
                double scoreA = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, a.getTitle(), a.getDescription());
                double scoreB = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, b.getTitle(), b.getDescription());
                return Double.compare(scoreB, scoreA);
            });
        }
        
        // Combine and limit results
        List<Ticket> allResults = new ArrayList<>(exactMatches);
        allResults.addAll(fuzzyMatches);
        
        return allResults.stream()
                .limit(limit)
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    // Autocomplete search for managers (limited results) with fuzzy matching
    public List<TicketResponse> autocompleteForManager(String query, int limit) {
        String trimmedQuery = query.trim();
        String escapedQuery = escapeRegex(trimmedQuery);
        Pageable pageable = PageRequest.of(0, limit * 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Collect all matching tickets
        Set<String> seenIds = new HashSet<>();
        List<Ticket> exactMatches = new ArrayList<>();
        List<Ticket> fuzzyMatches = new ArrayList<>();
        
        // 1. First, check for exact ID match
        ticketRepository.findById(trimmedQuery)
                .ifPresent(ticket -> {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                });
        
        // 2. Check for partial ID match
        if (isValidIdPattern(trimmedQuery) && exactMatches.size() < limit) {
            List<Ticket> allTickets = ticketRepository.findAll();
            for (Ticket ticket : allTickets) {
                if (!seenIds.contains(ticket.getId()) && 
                    ticket.getId().toLowerCase().contains(trimmedQuery.toLowerCase())) {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                    if (exactMatches.size() >= limit) break;
                }
            }
        }
        
        // 3. Search by title and description
        if (exactMatches.size() < limit) {
            List<Ticket> textResults = ticketRepository.searchAllTicketsTextFields(escapedQuery, pageable);
            for (Ticket ticket : textResults) {
                if (!seenIds.contains(ticket.getId())) {
                    exactMatches.add(ticket);
                    seenIds.add(ticket.getId());
                    if (exactMatches.size() >= limit) break;
                }
            }
        }
        
        // 4. Fuzzy search if we still need more results
        if (exactMatches.size() < limit) {
            double fuzzyThreshold = FuzzySearchUtil.getDefaultThreshold();
            List<Ticket> allTickets = ticketRepository.findAll();
            for (Ticket ticket : allTickets) {
                if (!seenIds.contains(ticket.getId())) {
                    boolean titleMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getTitle(), fuzzyThreshold);
                    boolean descMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getDescription(), fuzzyThreshold);
                    if (titleMatch || descMatch) {
                        fuzzyMatches.add(ticket);
                        seenIds.add(ticket.getId());
                    }
                }
            }
            
            // Sort fuzzy matches by relevance
            fuzzyMatches.sort((a, b) -> {
                double scoreA = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, a.getTitle(), a.getDescription());
                double scoreB = FuzzySearchUtil.calculateRelevanceScore(trimmedQuery, b.getTitle(), b.getDescription());
                return Double.compare(scoreB, scoreA);
            });
        }
        
        // Combine and limit results
        List<Ticket> allResults = new ArrayList<>(exactMatches);
        allResults.addAll(fuzzyMatches);
        
        return allResults.stream()
                .limit(limit)
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    // Get count of matching tickets for autocomplete (includes fuzzy matches)
    public long getSearchCountForAgent(String agentId, String query) {
        String trimmedQuery = query.trim();
        String escapedQuery = escapeRegex(trimmedQuery);
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Set<String> seenIds = new HashSet<>();
        
        // 1. Check for exact ID match
        ticketRepository.findByIdAndAssignedAgentId(trimmedQuery, agentId)
                .ifPresent(ticket -> seenIds.add(ticket.getId()));
        
        // 2. Check for partial ID match
        if (isValidIdPattern(trimmedQuery)) {
            List<Ticket> agentTickets = ticketRepository.findByAssignedAgentId(agentId);
            for (Ticket ticket : agentTickets) {
                if (ticket.getId().toLowerCase().contains(trimmedQuery.toLowerCase())) {
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        // 3. Add text field matches to seenIds (to avoid double counting)
        List<Ticket> textResults = ticketRepository.searchByAgentIdTextFields(agentId, escapedQuery, pageable);
        for (Ticket ticket : textResults) {
            seenIds.add(ticket.getId());
        }
        
        // 4. Add fuzzy matches
        double fuzzyThreshold = FuzzySearchUtil.getDefaultThreshold();
        List<Ticket> agentTickets = ticketRepository.findByAssignedAgentId(agentId);
        for (Ticket ticket : agentTickets) {
            if (!seenIds.contains(ticket.getId())) {
                boolean titleMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getTitle(), fuzzyThreshold);
                boolean descMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getDescription(), fuzzyThreshold);
                if (titleMatch || descMatch) {
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        return seenIds.size();
    }

    public long getSearchCountForManager(String query) {
        String trimmedQuery = query.trim();
        String escapedQuery = escapeRegex(trimmedQuery);
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Set<String> seenIds = new HashSet<>();
        
        // 1. Check for exact ID match
        ticketRepository.findById(trimmedQuery)
                .ifPresent(ticket -> seenIds.add(ticket.getId()));
        
        // 2. Check for partial ID match
        if (isValidIdPattern(trimmedQuery)) {
            List<Ticket> allTickets = ticketRepository.findAll();
            for (Ticket ticket : allTickets) {
                if (ticket.getId().toLowerCase().contains(trimmedQuery.toLowerCase())) {
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        // 3. Add text field matches to seenIds (to avoid double counting)
        List<Ticket> textResults = ticketRepository.searchAllTicketsTextFields(escapedQuery, pageable);
        for (Ticket ticket : textResults) {
            seenIds.add(ticket.getId());
        }
        
        // 4. Add fuzzy matches
        double fuzzyThreshold = FuzzySearchUtil.getDefaultThreshold();
        List<Ticket> allTickets = ticketRepository.findAll();
        for (Ticket ticket : allTickets) {
            if (!seenIds.contains(ticket.getId())) {
                boolean titleMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getTitle(), fuzzyThreshold);
                boolean descMatch = FuzzySearchUtil.fuzzyMatches(trimmedQuery, ticket.getDescription(), fuzzyThreshold);
                if (titleMatch || descMatch) {
                    seenIds.add(ticket.getId());
                }
            }
        }
        
        return seenIds.size();
    }

    // Check if query looks like a valid MongoDB ObjectId pattern (hexadecimal)
    private boolean isValidIdPattern(String query) {
        // MongoDB ObjectIds are 24 hex characters
        // Allow partial matches (at least 3 characters that are hex)
        return query.length() >= 3 && query.matches("^[a-fA-F0-9]+$");
    }

    // Escape special regex characters for safe querying
    private String escapeRegex(String query) {
        return Pattern.quote(query);
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
}
