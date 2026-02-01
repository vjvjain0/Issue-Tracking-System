package com.ticketing.system.service;

import com.ticketing.system.model.Activity;
import com.ticketing.system.model.Priority;
import com.ticketing.system.model.Ticket;
import com.ticketing.system.model.TicketStatus;
import com.ticketing.system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaEscalationService {

    private final TicketRepository ticketRepository;
    private final TicketElasticsearchService ticketElasticsearchService;

    // Run every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void escalateOverdueTickets() {
        log.info("Starting SLA escalation check");

        List<Ticket> activeTickets = ticketRepository.findByStatusNotIn(List.of(TicketStatus.RESOLVED, TicketStatus.INVALID));

        int escalatedCount = 0;
        for (Ticket ticket : activeTickets) {
            if (escalateIfNeeded(ticket)) {
                escalatedCount++;
            }
        }

        log.info("SLA escalation check completed. {} tickets escalated", escalatedCount);
    }

    private boolean escalateIfNeeded(Ticket ticket) {
        Priority currentPriority = ticket.getPriority();
        if (currentPriority == null) {
            // Skip tickets without priority
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long daysSinceCreation = java.time.Duration.between(ticket.getCreatedAt(), now).toDays();

        Priority newPriority = null;
        String escalationReason = null;

        switch (currentPriority) {
            case LOW:
                if (daysSinceCreation >= 7) {
                    newPriority = Priority.MEDIUM;
                    escalationReason = "SLA breach: LOW priority ticket not closed within 7 days";
                }
                break;
            case MEDIUM:
                if (daysSinceCreation >= 3) {
                    newPriority = Priority.HIGH;
                    escalationReason = "SLA breach: MEDIUM priority ticket not closed within 3 days";
                }
                break;
            case HIGH:
                // Already highest priority, no escalation
                break;
        }

        if (newPriority != null) {
            ticket.setPriority(newPriority);
            ticket.setUpdatedAt(now);

            // Add activity
            Activity activity = Activity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId("SYSTEM")
                    .userName("System")
                    .action("SLA_ESCALATION")
                    .details(escalationReason + ". Priority escalated from " + currentPriority + " to " + newPriority)
                    .timestamp(now)
                    .build();
            ticket.getActivities().add(activity);

            ticketRepository.save(ticket);
            ticketElasticsearchService.indexTicket(ticket);

            log.info("Escalated ticket {} from {} to {} due to SLA breach", ticket.getId(), currentPriority, newPriority);
            return true;
        }

        return false;
    }
}