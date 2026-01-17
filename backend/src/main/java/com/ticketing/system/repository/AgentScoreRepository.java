package com.ticketing.system.repository;

import com.ticketing.system.model.AgentScore;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentScoreRepository extends MongoRepository<AgentScore, String> {

    // Find score for a specific agent and week
    Optional<AgentScore> findByAgentIdAndWeekStartDate(String agentId, LocalDate weekStartDate);

    // Find all scores for a specific week
    List<AgentScore> findByWeekStartDate(LocalDate weekStartDate);

    // Find all scores for a specific agent, ordered by week
    List<AgentScore> findByAgentIdOrderByWeekStartDateDesc(String agentId);

    // Find the most recent score for an agent
    Optional<AgentScore> findTopByAgentIdOrderByWeekStartDateDesc(String agentId);

    // Find all scores within a date range
    List<AgentScore> findByWeekStartDateBetween(LocalDate startDate, LocalDate endDate);

    // Delete old scores (for cleanup)
    void deleteByWeekStartDateBefore(LocalDate date);
}
