import React, { useState, useEffect } from "react";
import { autoAssignAPI } from "../services/api";
import Navbar from "../components/Navbar";
import "./AgentsPage.css";

const AgentsPage = () => {
  const [workloads, setWorkloads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [autoAssigning, setAutoAssigning] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const response = await autoAssignAPI.getWorkloads();
      setWorkloads(response.data);
      setError("");
    } catch (err) {
      setError("Failed to load data");
    } finally {
      setLoading(false);
    }
  };

  const handleAutoAssignAll = async () => {
    try {
      setAutoAssigning(true);
      setSuccessMessage("");
      setError("");
      const response = await autoAssignAPI.autoAssignAll();
      setSuccessMessage(response.data.message);
      await fetchData();
    } catch (err) {
      setError("Failed to auto-assign tickets");
    } finally {
      setAutoAssigning(false);
    }
  };

  const getPriorityColor = (priority) => {
    if (priority >= 0.5) return "#2e7d32";
    if (priority >= 0.3) return "#ef6c00";
    return "#c62828";
  };

  const totalActiveTickets = workloads.reduce(
    (sum, w) => sum + w.totalActiveTickets,
    0,
  );

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="agents-container">
          <div className="loading-state">Loading...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="agents-container">
        <div className="page-header">
          <div className="header-content">
            <h1>Agents</h1>
            <p>
              View agent workload, productivity scores, and manage ticket
              distribution
            </p>
          </div>
          <button
            className="auto-assign-btn"
            onClick={handleAutoAssignAll}
            disabled={autoAssigning}
          >
            {autoAssigning
              ? "⏳ Assigning..."
              : "🎯 Auto-Assign Unassigned Tickets"}
          </button>
        </div>

        {error && <div className="error-banner">{error}</div>}
        {successMessage && (
          <div className="success-banner">{successMessage}</div>
        )}

        <div className="info-box">
          <h4>📌 Auto-Assignment Algorithm</h4>
          <p>
            Tickets are auto-assigned based on <strong>current workload</strong>{" "}
            (fewer active tickets = higher priority) and{" "}
            <strong>productivity score</strong> (more tickets closed weekly =
            higher priority). Scores are calculated automatically every week.
          </p>
        </div>

        <div className="summary-stats">
          <div className="summary-item">
            <span className="summary-value">{workloads.length}</span>
            <span className="summary-label">Total Agents</span>
          </div>
          <div className="summary-item">
            <span className="summary-value">{totalActiveTickets}</span>
            <span className="summary-label">Total Active Tickets</span>
          </div>
        </div>

        <div className="agents-grid">
          {workloads
            .sort((a, b) => b.assignmentPriority - a.assignmentPriority)
            .map((workload, index) => (
              <div key={workload.agentId} className="agent-card">
                <div className="agent-header">
                  <div className="rank-badge">#{index + 1}</div>
                  <div className="agent-avatar">
                    {workload.agentName.charAt(0)}
                  </div>
                  <div className="agent-info">
                    <h3>{workload.agentName}</h3>
                    <span className="agent-email">{workload.agentEmail}</span>
                  </div>
                  <div
                    className="priority-badge"
                    style={{
                      backgroundColor: getPriorityColor(
                        workload.assignmentPriority,
                      ),
                    }}
                  >
                    Priority: {workload.assignmentPriority.toFixed(2)}
                  </div>
                </div>

                <div className="stats-section">
                  <h4>Active Workload</h4>
                  <div className="workload-stats">
                    <div className="stat-box">
                      <span className="stat-number not-started">
                        {workload.notStartedCount}
                      </span>
                      <span className="stat-name">Not Started</span>
                    </div>
                    <div className="stat-box">
                      <span className="stat-number in-progress">
                        {workload.inProgressCount}
                      </span>
                      <span className="stat-name">In Progress</span>
                    </div>
                    <div className="stat-box">
                      <span className="stat-number total">
                        {workload.totalActiveTickets}
                      </span>
                      <span className="stat-name">Total Active</span>
                    </div>
                  </div>
                </div>

                <div className="stats-section">
                  <h4>Productivity Score</h4>
                  <div className="score-display">
                    <div className="current-score">
                      <span className="score-number">
                        {workload.productivityScore.toFixed(1)}
                      </span>
                      <span className="score-label">Weekly Score</span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
        </div>
      </div>
    </>
  );
};

export default AgentsPage;
