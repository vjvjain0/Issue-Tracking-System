package com.ticketing.system.repository;

import com.ticketing.system.model.Ticket;
import com.ticketing.system.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByAssignedAgentId(String agentId);
    List<Ticket> findByAssignedAgentIdAndStatus(String agentId, TicketStatus status);
    List<Ticket> findByStatus(TicketStatus status);
    List<Ticket> findByAssignedAgentIdIsNull();

    // Find tickets closed by an agent within a date range
    List<Ticket> findByAssignedAgentIdAndClosedAtBetween(String agentId, LocalDateTime start, LocalDateTime end);

    // Find tickets closed by an agent with specific status within a date range
    List<Ticket> findByAssignedAgentIdAndStatusAndClosedAtBetween(
            String agentId, TicketStatus status, LocalDateTime start, LocalDateTime end);

    // Count active tickets (NOT_STARTED or IN_PROGRESS) for an agent
    long countByAssignedAgentIdAndStatusIn(String agentId, List<TicketStatus> statuses);

    // Find all tickets with specific statuses for an agent
    List<Ticket> findByAssignedAgentIdAndStatusIn(String agentId, List<TicketStatus> statuses);

    // Find ticket by ID and assigned agent (for ID search verification)
    Optional<Ticket> findByIdAndAssignedAgentId(String id, String agentId);

    // Search tickets by title or description for agents - list
    @Query("{ 'assignedAgentId': ?0, $or: [ { 'title': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }")
    List<Ticket> searchByAgentIdTextFields(String agentId, String searchQuery, Pageable pageable);

    // Count search results for agents (title/description only)
    @Query(value = "{ 'assignedAgentId': ?0, $or: [ { 'title': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }", count = true)
    long countSearchByAgentIdTextFields(String agentId, String searchQuery);

    // Search all tickets by title or description for managers - list
    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    List<Ticket> searchAllTicketsTextFields(String searchQuery, Pageable pageable);

    // Count search results for managers (title/description only)
    @Query(value = "{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }", count = true)
    long countSearchAllTicketsTextFields(String searchQuery);

    // Find tickets by ID pattern (for ID-based search) - checks if ID contains the query
    @Query("{ '_id': ?0 }")
    Optional<Ticket> findByExactId(String id);

    // Find all tickets with IDs that start with a pattern (for managers)
    @Query("{ '_id': { $regex: ?0, $options: 'i' } }")
    List<Ticket> findByIdStartingWith(String idPrefix, Pageable pageable);
}
