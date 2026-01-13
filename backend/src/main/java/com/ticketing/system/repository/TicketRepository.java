package com.ticketing.system.repository;

import com.ticketing.system.model.Ticket;
import com.ticketing.system.model.TicketStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}
