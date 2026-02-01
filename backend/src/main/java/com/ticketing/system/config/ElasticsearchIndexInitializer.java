package com.ticketing.system.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.service.TicketElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after DataInitializer (which is @Order(1) by default)
public class ElasticsearchIndexInitializer implements CommandLineRunner {

    private final ElasticsearchClient elasticsearchClient;
    private final TicketRepository ticketRepository;
    private final TicketElasticsearchService ticketElasticsearchService;

    @Override
    public void run(String... args) {
        log.info("Checking Elasticsearch index status...");

        if (shouldReindexTickets()) {
            log.info("Reindexing tickets to Elasticsearch...");
            ticketElasticsearchService.reindexAllTickets();
        } else {
            log.info("Elasticsearch index is up to date");
        }
    }

    private boolean shouldReindexTickets() {
        try {
            // Check if index exists
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index("tickets"));
            boolean indexExists = elasticsearchClient.indices().exists(existsRequest).value();

            if (!indexExists) {
                log.info("Tickets index does not exist, reindexing required");
                return true;
            }

            // Check if index has the correct number of documents
            co.elastic.clients.elasticsearch.core.CountRequest countRequest = co.elastic.clients.elasticsearch.core.CountRequest.of(c -> c
                    .index("tickets")
            );
            long esCount = elasticsearchClient.count(countRequest).count();
            long dbCount = ticketRepository.count();

            if (esCount != dbCount) {
                log.info("Document count mismatch: ES has {}, DB has {}, reindexing required", esCount, dbCount);
                return true;
            }

            log.info("Tickets index exists with {} documents, matches DB count", esCount);
            return false;

        } catch (Exception e) {
            log.warn("Error checking if reindexing is needed, will reindex to be safe", e);
            return true;
        }
    }
}