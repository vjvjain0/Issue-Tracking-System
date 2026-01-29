package com.ticketing.system.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import com.ticketing.system.dto.TicketSummaryResponse;
import com.ticketing.system.model.Ticket;
import com.ticketing.system.model.TicketDocument;
import com.ticketing.system.repository.TicketDocumentRepository;
import com.ticketing.system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketElasticsearchService {

    private final TicketDocumentRepository ticketDocumentRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final TicketRepository ticketRepository;

    public void indexTicket(Ticket ticket) {
        log.info("Indexing ticket with ID: {}", ticket.getId());
        try {
            // Format LocalDateTime to ISO string for ES indexing
            String createdAt = ticket.getCreatedAt() != null ? ticket.getCreatedAt().truncatedTo(ChronoUnit.MILLIS).toString() : null;
            String updatedAt = ticket.getUpdatedAt() != null ? ticket.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS).toString() : null;
            String closedAt = ticket.getClosedAt() != null ? ticket.getClosedAt().truncatedTo(ChronoUnit.MILLIS).toString() : null;

            TicketDocument doc = TicketDocument.builder()
                    .id(ticket.getId())
                    .title(ticket.getTitle())
                    .description(ticket.getDescription())
                    .status(ticket.getStatus().name())
                    .assignedAgentId(ticket.getAssignedAgentId())
                    .assignedAgentName(ticket.getAssignedAgentName())
                    .customerEmail(ticket.getCustomerEmail())
                    .customerName(ticket.getCustomerName())
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .closedAt(closedAt)
                    .autoAssigned(ticket.isAutoAssigned())
                    .build();
            ticketDocumentRepository.save(doc);
            log.info("Successfully indexed ticket with ID: {}", ticket.getId());
        } catch (Exception e) {
            log.error("Error indexing ticket with ID: {}", ticket.getId(), e);
            throw e;
        }
    }

    public void deleteTicket(String id) {
        log.info("Deleting ticket document with ID: {}", id);
        try {
            ticketDocumentRepository.deleteById(id);
            log.info("Successfully deleted ticket document with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting ticket document with ID: {}", id, e);
            throw e;
        }
    }

    public void deleteIndex() {
        log.info("Deleting tickets index");
        try {
            DeleteIndexRequest deleteRequest = DeleteIndexRequest.of(d -> d.index("tickets"));
            elasticsearchClient.indices().delete(deleteRequest);
            log.info("Successfully deleted tickets index");
        } catch (Exception e) {
            log.warn("Error deleting tickets index (might not exist)", e);
        }
    }

    public void reindexAllTickets() {
        log.info("Starting reindexing of all tickets");
        try {
            deleteIndex(); // Delete index to ensure fresh mapping
            List<Ticket> allTickets = ticketRepository.findAll();
            for (Ticket ticket : allTickets) {
                indexTicket(ticket);
            }
            log.info("Successfully reindexed {} tickets", allTickets.size());
        } catch (Exception e) {
            log.error("Error reindexing all tickets", e);
            throw e;
        }
    }

    public List<TicketSummaryResponse> fuzzySearchForAgent(String agentId, String query, int limit) {
        log.info("Fuzzy search for agent: {}, query: '{}', limit: {}", agentId, query, limit);
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.must(TermQuery.of(t -> t.field("assignedAgentId").value(agentId))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("title").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("description").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.minimumShouldMatch("1");

            // If query looks like a valid ticket ID, also search by ID
            if (isValidIdPattern(query)) {
                boolBuilder.should(TermQuery.of(t -> t.field("id").value(query))._toQuery());
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("tickets")
                    .query(boolBuilder.build()._toQuery())
                    .size(limit)
                    .sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)))
            );

            SearchResponse<TicketDocument> response = elasticsearchClient.search(searchRequest, TicketDocument.class);
            List<TicketSummaryResponse> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .map(this::mapToSummary)
                    .collect(Collectors.toList());

            log.info("Fuzzy search for agent returned {} results", results.size());
            return results;
        } catch (IOException e) {
            log.error("Error performing fuzzy search for agent: {}, query: '{}'", agentId, query, e);
            throw new RuntimeException("Error searching tickets", e);
        }
    }

    public List<TicketSummaryResponse> fuzzySearchAll(String query, int limit) {
        log.info("Fuzzy search all tickets, query: '{}', limit: {}", query, limit);
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.should(MatchQuery.of(m -> m.field("title").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("description").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.minimumShouldMatch("1");

            // If query looks like a valid ticket ID, also search by ID
            if (isValidIdPattern(query)) {
                boolBuilder.should(TermQuery.of(t -> t.field("id").value(query))._toQuery());
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("tickets")
                    .query(boolBuilder.build()._toQuery())
                    .size(limit)
                    .sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)))
            );

            SearchResponse<TicketDocument> response = elasticsearchClient.search(searchRequest, TicketDocument.class);
            List<TicketSummaryResponse> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .map(this::mapToSummary)
                    .collect(Collectors.toList());

            log.info("Fuzzy search all returned {} results", results.size());
            return results;
        } catch (IOException e) {
            log.error("Error performing fuzzy search all tickets, query: '{}'", query, e);
            throw new RuntimeException("Error searching tickets", e);
        }
    }

    public List<TicketSummaryResponse> fuzzySearchForAgentPaged(String agentId, String query, int page, int size) {
        log.info("Paged fuzzy search for agent: {}, query: '{}', page: {}, size: {}", agentId, query, page, size);
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.must(TermQuery.of(t -> t.field("assignedAgentId").value(agentId))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("title").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("description").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.minimumShouldMatch("1");

            // If query looks like a valid ticket ID, also search by ID
            if (isValidIdPattern(query)) {
                boolBuilder.should(TermQuery.of(t -> t.field("id").value(query))._toQuery());
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("tickets")
                    .query(boolBuilder.build()._toQuery())
                    .from(page * size)
                    .size(size)
                    .sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)))
            );

            SearchResponse<TicketDocument> response = elasticsearchClient.search(searchRequest, TicketDocument.class);
            List<TicketSummaryResponse> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .map(this::mapToSummary)
                    .collect(Collectors.toList());

            log.info("Paged fuzzy search for agent returned {} results", results.size());
            return results;
        } catch (IOException e) {
            log.error("Error performing paged fuzzy search for agent: {}, query: '{}', page: {}", agentId, query, page, e);
            throw new RuntimeException("Error searching tickets", e);
        }
    }

    public List<TicketSummaryResponse> fuzzySearchAllPaged(String query, int page, int size) {
        log.info("Paged fuzzy search all tickets, query: '{}', page: {}, size: {}", query, page, size);
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.should(MatchQuery.of(m -> m.field("title").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("description").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.minimumShouldMatch("1");

            // If query looks like a valid ticket ID, also search by ID
            if (isValidIdPattern(query)) {
                boolBuilder.should(TermQuery.of(t -> t.field("id").value(query))._toQuery());
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("tickets")
                    .query(boolBuilder.build()._toQuery())
                    .from(page * size)
                    .size(size)
                    .sort(so -> so.field(f -> f.field("createdAt").order(SortOrder.Desc)))
            );

            SearchResponse<TicketDocument> response = elasticsearchClient.search(searchRequest, TicketDocument.class);
            List<TicketSummaryResponse> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .map(this::mapToSummary)
                    .collect(Collectors.toList());

            log.info("Paged fuzzy search all returned {} results", results.size());
            return results;
        } catch (IOException e) {
            log.error("Error performing paged fuzzy search all tickets, query: '{}', page: {}", query, page, e);
            throw new RuntimeException("Error searching tickets", e);
        }
    }

    public long countFuzzySearchForAgent(String agentId, String query) {
        log.info("Counting fuzzy search for agent: {}, query: '{}'", agentId, query);
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.must(TermQuery.of(t -> t.field("assignedAgentId").value(agentId))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("title").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("description").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.minimumShouldMatch("1");

            // If query looks like a valid ticket ID, also search by ID
            if (isValidIdPattern(query)) {
                boolBuilder.should(TermQuery.of(t -> t.field("id").value(query))._toQuery());
            }

            co.elastic.clients.elasticsearch.core.CountRequest countRequest = co.elastic.clients.elasticsearch.core.CountRequest.of(c -> c
                    .index("tickets")
                    .query(boolBuilder.build()._toQuery())
            );

            co.elastic.clients.elasticsearch.core.CountResponse countResponse = elasticsearchClient.count(countRequest);
            long count = countResponse.count();
            log.info("Count fuzzy search for agent returned {}", count);
            return count;
        } catch (IOException e) {
            log.error("Error counting fuzzy search for agent: {}, query: '{}'", agentId, query, e);
            throw new RuntimeException("Error counting tickets", e);
        }
    }

    public long countFuzzySearchAll(String query) {
        log.info("Counting fuzzy search all tickets, query: '{}'", query);
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolBuilder.should(MatchQuery.of(m -> m.field("title").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.should(MatchQuery.of(m -> m.field("description").query(query).fuzziness("AUTO"))._toQuery());
            boolBuilder.minimumShouldMatch("1");

            // If query looks like a valid ticket ID, also search by ID
            if (isValidIdPattern(query)) {
                boolBuilder.should(TermQuery.of(t -> t.field("id").value(query))._toQuery());
            }

            co.elastic.clients.elasticsearch.core.CountRequest countRequest = co.elastic.clients.elasticsearch.core.CountRequest.of(c -> c
                    .index("tickets")
                    .query(boolBuilder.build()._toQuery())
            );

            co.elastic.clients.elasticsearch.core.CountResponse countResponse = elasticsearchClient.count(countRequest);
            long count = countResponse.count();
            log.info("Count fuzzy search all returned {}", count);
            return count;
        } catch (IOException e) {
            log.error("Error counting fuzzy search all tickets, query: '{}'", query, e);
            throw new RuntimeException("Error counting tickets", e);
        }
    }

    private boolean isValidIdPattern(String query) {
        return query.length() >= 3 && query.matches("^[a-fA-F0-9]+$");
    }

    private TicketSummaryResponse mapToSummary(TicketDocument doc) {
        return TicketSummaryResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .status(com.ticketing.system.model.TicketStatus.valueOf(doc.getStatus()))
                .assignedAgentId(doc.getAssignedAgentId())
                .assignedAgentName(doc.getAssignedAgentName())
                .customerName(doc.getCustomerName())
                .createdAt(doc.getCreatedAt() != null ? LocalDateTime.parse(doc.getCreatedAt()) : null)
                .updatedAt(doc.getUpdatedAt() != null ? LocalDateTime.parse(doc.getUpdatedAt()) : null)
                .build();
    }
}