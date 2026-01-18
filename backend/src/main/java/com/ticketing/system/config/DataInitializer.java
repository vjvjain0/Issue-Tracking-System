package com.ticketing.system.config;

import com.ticketing.system.model.*;
import com.ticketing.system.repository.AgentScoreRepository;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.repository.UserRepository;
import com.ticketing.system.service.AgentScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AgentScoreRepository agentScoreRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized. Skipping data initialization.");
            return;
        }

        log.info("Initializing test data...");

        // Create Manager
        User manager = createUser("Sarah Manager", "manager@company.com", "manager123", Role.MANAGER, "MGR-001", "+1-555-100-0001");
        log.info("Created Manager: {} / {}", manager.getEmail(), "manager123");

        // Create 5 Agents
        List<User> agents = new ArrayList<>();
        String[][] agentData = {
            {"John Smith", "john.smith@company.com", "agent123", "AGT-001", "+1-555-200-0001"},
            {"Emily Johnson", "emily.johnson@company.com", "agent123", "AGT-002", "+1-555-200-0002"},
            {"Michael Brown", "michael.brown@company.com", "agent123", "AGT-003", "+1-555-200-0003"},
            {"Jessica Davis", "jessica.davis@company.com", "agent123", "AGT-004", "+1-555-200-0004"},
            {"David Wilson", "david.wilson@company.com", "agent123", "AGT-005", "+1-555-200-0005"}
        };

        for (String[] data : agentData) {
            User agent = createUser(data[0], data[1], data[2], Role.AGENT, data[3], data[4]);
            agents.add(agent);
            log.info("Created Agent: {} / {}", agent.getEmail(), data[2]);
        }

        // Create 50 Tickets
        String[] ticketTitles = {
            "Cannot login to account", "Payment failed", "App crashing on startup",
            "Password reset not working", "Unable to update profile", "Slow loading times",
            "Error 500 on checkout", "Missing order confirmation", "Refund not processed",
            "Cannot change email address", "Two-factor auth issues", "Account locked",
            "Billing discrepancy", "Feature request: Dark mode", "Bug in search functionality",
            "Mobile app not syncing", "Notifications not received", "Data export failing",
            "Integration not working", "API rate limit issues", "Session timeout too short",
            "Cannot delete account", "Duplicate charges", "Voucher code not working",
            "Subscription cancellation failed", "Missing transaction history", "UI display issues",
            "File upload not working", "Video playback error", "Chat support not loading",
            "Language settings not saving", "Timezone incorrect", "Report generation slow",
            "Dashboard not updating", "Email verification failed", "Cannot add payment method",
            "Order tracking not working", "Shipping address update failed", "Wishlist sync issue",
            "Reviews not posting", "Filters not working", "Sort functionality broken",
            "Print function not working", "Export to PDF failing", "Calendar integration broken",
            "Reminder notifications missing", "Auto-save not working", "Draft messages lost",
            "Attachment download failed", "Link sharing not working"
        };

        String[] customerNames = {
            "Alice Cooper", "Bob Martin", "Carol White", "Dan Brown", "Eve Black",
            "Frank Green", "Grace Lee", "Henry Taylor", "Ivy Chen", "Jack Wilson"
        };

        TicketStatus[] statuses = TicketStatus.values();

        for (int i = 0; i < 50; i++) {
            User assignedAgent = agents.get(random.nextInt(agents.size()));
            String customerName = customerNames[random.nextInt(customerNames.length)];
            String customerEmail = customerName.toLowerCase().replace(" ", ".") + "@email.com";
            TicketStatus status = statuses[random.nextInt(statuses.length)];

            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(30));
            LocalDateTime updatedAt = LocalDateTime.now().minusDays(random.nextInt(7));
            LocalDateTime closedAt = null;

            // Set closedAt for RESOLVED and INVALID tickets
            if (status == TicketStatus.RESOLVED || status == TicketStatus.INVALID) {
                closedAt = LocalDateTime.now().minusDays(random.nextInt(5));
            }

            Ticket ticket = Ticket.builder()
                    .title(ticketTitles[i])
                    .description("Detailed description for issue: " + ticketTitles[i] +
                            ". Customer is experiencing this problem and needs assistance.")
                    .status(status)
                    .assignedAgentId(assignedAgent.getId())
                    .assignedAgentName(assignedAgent.getName())
                    .customerEmail(customerEmail)
                    .customerName(customerName)
                    .comments(generateComments(assignedAgent, status))
                    .activities(generateActivities(assignedAgent, manager, status))
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .closedAt(closedAt)
                    .autoAssigned(false)
                    .build();

            ticketRepository.save(ticket);
        }

        log.info("Created 50 tickets with various statuses");

        // Generate initial agent scores for current and previous week
        generateInitialScores(agents);
        log.info("Data initialization complete!");
        log.info("");
        log.info("===========================================");
        log.info("LOGIN CREDENTIALS:");
        log.info("===========================================");
        log.info("MANAGER:");
        log.info("  Email: manager@company.com");
        log.info("  Password: manager123");
        log.info("");
        log.info("AGENTS:");
        log.info("  Email: john.smith@company.com     | Password: agent123");
        log.info("  Email: emily.johnson@company.com  | Password: agent123");
        log.info("  Email: michael.brown@company.com  | Password: agent123");
        log.info("  Email: jessica.davis@company.com  | Password: agent123");
        log.info("  Email: david.wilson@company.com   | Password: agent123");
        log.info("===========================================");
    }

    private User createUser(String name, String email, String password, Role role, String employeeId, String phoneNumber) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .employeeId(employeeId)
                .phoneNumber(phoneNumber)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private List<Comment> generateComments(User agent, TicketStatus status) {
        List<Comment> comments = new ArrayList<>();

        if (status == TicketStatus.IN_PROGRESS || status == TicketStatus.RESOLVED || status == TicketStatus.INVALID) {
            comments.add(Comment.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(agent.getId())
                    .userName(agent.getName())
                    .content("I'm looking into this issue now.")
                    .createdAt(LocalDateTime.now().minusDays(random.nextInt(5) + 1))
                    .build());
        }

        if (status == TicketStatus.RESOLVED) {
            comments.add(Comment.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(agent.getId())
                    .userName(agent.getName())
                    .content("Issue has been resolved. Please verify and let us know if you need further assistance.")
                    .createdAt(LocalDateTime.now().minusDays(random.nextInt(2)))
                    .build());
        }

        if (status == TicketStatus.INVALID) {
            comments.add(Comment.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(agent.getId())
                    .userName(agent.getName())
                    .content("After investigation, this appears to be a duplicate or not a valid issue.")
                    .createdAt(LocalDateTime.now().minusDays(random.nextInt(2)))
                    .build());
        }

        return comments;
    }

    private List<Activity> generateActivities(User agent, User manager, TicketStatus status) {
        List<Activity> activities = new ArrayList<>();

        // Creation activity
        activities.add(Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId("SYSTEM")
                .userName("System")
                .action("TICKET_CREATED")
                .details("Ticket created by customer")
                .timestamp(LocalDateTime.now().minusDays(random.nextInt(30) + 7))
                .build());

        // Assignment activity
        activities.add(Activity.builder()
                .id(UUID.randomUUID().toString())
                .userId(manager.getId())
                .userName(manager.getName())
                .action("TICKET_ASSIGNED")
                .details("Ticket assigned to " + agent.getName())
                .timestamp(LocalDateTime.now().minusDays(random.nextInt(20) + 5))
                .build());

        if (status == TicketStatus.IN_PROGRESS || status == TicketStatus.RESOLVED || status == TicketStatus.INVALID) {
            activities.add(Activity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(agent.getId())
                    .userName(agent.getName())
                    .action("STATUS_CHANGED")
                    .details("Status changed from NOT_STARTED to IN_PROGRESS")
                    .timestamp(LocalDateTime.now().minusDays(random.nextInt(10) + 3))
                    .build());
        }

        if (status == TicketStatus.RESOLVED) {
            activities.add(Activity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(agent.getId())
                    .userName(agent.getName())
                    .action("STATUS_CHANGED")
                    .details("Status changed from IN_PROGRESS to RESOLVED")
                    .timestamp(LocalDateTime.now().minusDays(random.nextInt(3)))
                    .build());
        }

        if (status == TicketStatus.INVALID) {
            activities.add(Activity.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(agent.getId())
                    .userName(agent.getName())
                    .action("STATUS_CHANGED")
                    .details("Status changed from IN_PROGRESS to INVALID")
                    .timestamp(LocalDateTime.now().minusDays(random.nextInt(3)))
                    .build());
        }

        return activities;
    }

    private void generateInitialScores(List<User> agents) {
        LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);
        LocalDate twoWeeksAgoStart = currentWeekStart.minusWeeks(2);

        // Generate scores for last 3 weeks with varying productivity
        List<LocalDate> weeks = Arrays.asList(twoWeeksAgoStart, previousWeekStart, currentWeekStart);

        for (LocalDate weekStart : weeks) {
            LocalDate weekEnd = weekStart.plusDays(6);

            for (User agent : agents) {
                // Generate random but realistic scores
                int ticketsResolved = random.nextInt(8) + 2; // 2-9 resolved
                int ticketsInvalid = random.nextInt(3);      // 0-2 invalid
                int ticketsClosed = ticketsResolved + ticketsInvalid;
                double productivityScore = ticketsResolved * 1.0 + ticketsInvalid * 0.5;

                AgentScore score = AgentScore.builder()
                        .agentId(agent.getId())
                        .agentName(agent.getName())
                        .agentEmail(agent.getEmail())
                        .weekStartDate(weekStart)
                        .weekEndDate(weekEnd)
                        .ticketsClosed(ticketsClosed)
                        .ticketsResolved(ticketsResolved)
                        .ticketsInvalid(ticketsInvalid)
                        .productivityScore(productivityScore)
                        .calculatedAt(LocalDateTime.now())
                        .build();

                agentScoreRepository.save(score);
            }
        }

        log.info("Generated initial agent scores for last 3 weeks");
    }
}
