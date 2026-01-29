package com.ticketing.system.repository;

import com.ticketing.system.model.TicketDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketDocumentRepository extends ElasticsearchRepository<TicketDocument, String> {

    List<TicketDocument> findByAssignedAgentId(String assignedAgentId);

    List<TicketDocument> findByAssignedAgentIdAndTitleContainingOrDescriptionContaining(String assignedAgentId, String title, String description);

    // For fuzzy search, we'll use custom queries
}