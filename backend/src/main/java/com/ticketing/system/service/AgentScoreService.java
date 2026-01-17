package com.ticketing.system.service;

import com.ticketing.system.model.*;
import com.ticketing.system.repository.AgentScoreRepository;
import com.ticketing.system.repository.TicketRepository;
import com.ticketing.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentScoreService {

    private final AgentScoreRepository agentScoreRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    /**
     * Calculate scores for all agents for a specific week.
     */
    public List<AgentScore> calculateScoresForWeek(LocalDate weekStartDate) {
        // Ensure weekStartDate is a Monday
        LocalDate monday = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime weekEnd = sunday.atTime(LocalTime.MAX);

        List<User> agents = userRepository.findByRole(Role.AGENT);
        List<AgentScore> scores = new ArrayList<>();

        for (User agent : agents) {
            AgentScore score = calculateAgentScore(agent, monday, sunday, weekStart, weekEnd);

            // Check if score already exists for this agent and week
            Optional<AgentScore> existingScore = agentScoreRepository
                    .findByAgentIdAndWeekStartDate(agent.getId(), monday);

            if (existingScore.isPresent()) {
                score.setId(existingScore.get().getId());
            }

            scores.add(agentScoreRepository.save(score));
        }

        log.info("Calculated scores for {} agents for week starting {}", scores.size(), monday);
        return scores;
    }

    /**
     * Calculate score for a single agent for a specific week.
     */
    private AgentScore calculateAgentScore(User agent, LocalDate monday, LocalDate sunday,
                                           LocalDateTime weekStart, LocalDateTime weekEnd) {
        // Count resolved tickets
        List<Ticket> resolvedTickets = ticketRepository
                .findByAssignedAgentIdAndStatusAndClosedAtBetween(
                        agent.getId(), TicketStatus.RESOLVED, weekStart, weekEnd);

        // Count invalid tickets
        List<Ticket> invalidTickets = ticketRepository
                .findByAssignedAgentIdAndStatusAndClosedAtBetween(
                        agent.getId(), TicketStatus.INVALID, weekStart, weekEnd);

        int ticketsResolved = resolvedTickets.size();
        int ticketsInvalid = invalidTickets.size();
        int ticketsClosed = ticketsResolved + ticketsInvalid;

        // Calculate productivity score
        // Resolved tickets are weighted more than invalid (resolved = 1.0, invalid = 0.5)
        double productivityScore = ticketsResolved * 1.0 + ticketsInvalid * 0.5;

        return AgentScore.builder()
                .agentId(agent.getId())
                .agentName(agent.getName())
                .agentEmail(agent.getEmail())
                .weekStartDate(monday)
                .weekEndDate(sunday)
                .ticketsClosed(ticketsClosed)
                .ticketsResolved(ticketsResolved)
                .ticketsInvalid(ticketsInvalid)
                .productivityScore(productivityScore)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get current week's start date (Monday).
     */
    public LocalDate getCurrentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Get previous week's start date.
     */
    public LocalDate getPreviousWeekStart() {
        return getCurrentWeekStart().minusWeeks(1);
    }

    /**
     * Get scores for the current week.
     */
    public List<AgentScore> getCurrentWeekScores() {
        return agentScoreRepository.findByWeekStartDate(getCurrentWeekStart());
    }

    /**
     * Get the most recent score for an agent.
     */
    public Optional<AgentScore> getLatestScoreForAgent(String agentId) {
        return agentScoreRepository.findTopByAgentIdOrderByWeekStartDateDesc(agentId);
    }

    /**
     * Get score history for an agent.
     */
    public List<AgentScore> getScoreHistoryForAgent(String agentId) {
        return agentScoreRepository.findByAgentIdOrderByWeekStartDateDesc(agentId);
    }

    /**
     * Get scores for the last N weeks.
     */
    public List<AgentScore> getScoresForLastNWeeks(int weeks) {
        LocalDate startDate = getCurrentWeekStart().minusWeeks(weeks - 1);
        LocalDate endDate = getCurrentWeekStart();
        return agentScoreRepository.findByWeekStartDateBetween(startDate, endDate);
    }

    /**
     * Scheduled task to calculate scores every Monday at 1:00 AM.
     * This calculates scores for the previous week.
     */
    @Scheduled(cron = "0 0 1 * * MON")
    public void scheduledScoreCalculation() {
        log.info("Running scheduled score calculation for previous week");
        LocalDate previousWeekStart = getPreviousWeekStart();
        calculateScoresForWeek(previousWeekStart);
    }

    /**
     * Calculate current week scores (can be called on demand).
     */
    public List<AgentScore> calculateCurrentWeekScores() {
        return calculateScoresForWeek(getCurrentWeekStart());
    }

    /**
     * Calculate previous week scores (for initial setup or recalculation).
     */
    public List<AgentScore> calculatePreviousWeekScores() {
        return calculateScoresForWeek(getPreviousWeekStart());
    }

    /**
     * Clean up old scores (older than specified weeks).
     */
    public void cleanupOldScores(int weeksToKeep) {
        LocalDate cutoffDate = getCurrentWeekStart().minusWeeks(weeksToKeep);
        agentScoreRepository.deleteByWeekStartDateBefore(cutoffDate);
        log.info("Cleaned up scores older than {}", cutoffDate);
    }
}
