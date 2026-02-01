import React, { useState, useEffect } from "react";
import { ticketAPI, agentAPI } from "../services/api";
import Navbar from "../components/Navbar";
import "./AgentsPage.css";

const AgentsPage = () => {
  const [workloads, setWorkloads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [autoAssigning, setAutoAssigning] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  
  // Agent detail modal state
  const [selectedAgent, setSelectedAgent] = useState(null);
  const [agentDetails, setAgentDetails] = useState(null);
  const [loadingDetails, setLoadingDetails] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const response = await agentAPI.getWorkloads();
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
      const response = await ticketAPI.autoAssignAll();
      setSuccessMessage(response.data.message);
      await fetchData();
    } catch (err) {
      setError("Failed to auto-assign tickets");
    } finally {
      setAutoAssigning(false);
    }
  };

  const handleAgentClick = async (agentId) => {
    try {
      setSelectedAgent(agentId);
      setLoadingDetails(true);
      const response = await agentAPI.getAgentDetails(agentId);
      setAgentDetails(response.data);
    } catch (err) {
      setError("Failed to load agent details");
      setSelectedAgent(null);
    } finally {
      setLoadingDetails(false);
    }
  };

  const closeModal = () => {
    setSelectedAgent(null);
    setAgentDetails(null);
  };

  // Format epoch milliseconds to relative time
  const formatLastActive = (epochMs) => {
    if (!epochMs) return "Never";
    
    const date = new Date(epochMs);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return "Just now";
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  const getWorkloadColor = (score) => {
    if (score <= 1) return "#2e7d32"; // Low workload - good
    if (score <= 3) return "#ef6c00"; // Medium workload
    return "#c62828"; // High workload - needs attention
  };

  const getWorkloadLabel = (score) => {
    if (score <= 1) return "Low";
    if (score <= 3) return "Medium";
    return "High";
  };

  const totalActiveTickets = workloads.reduce(
    (sum, w) => sum + w.totalActiveTickets,
    0,
  );

  // Sort by lowest workload first (agents with less work get priority)
  const sortedWorkloads = [...workloads].sort(
    (a, b) => a.workloadScore - b.workloadScore
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
              View agent workloads and manage ticket distribution.
              <span className="sort-info">Sorted by lowest workload first.</span>
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

        <div className="agents-list">
          {sortedWorkloads.map((workload, index) => (
            <div 
              key={workload.agentId} 
              className="agent-list-item"
              onClick={() => handleAgentClick(workload.agentId)}
            >
              <div className="agent-rank">#{index + 1}</div>
              <div className="agent-avatar">
                {workload.agentName.charAt(0)}
              </div>
              <div className="agent-main-info">
                <h3>{workload.agentName}</h3>
                <span className="agent-email">{workload.agentEmail}</span>
              </div>
              <div className="agent-active-tickets">
                <div className="ticket-breakdown">
                  <span className="ticket-count">{workload.totalActiveTickets}</span>
                  <span className="ticket-label">Active Tickets</span>
                  <div className="priority-breakdown">
                    <span className="priority-high">H:{workload.highPriorityCount}</span>
                    <span className="priority-medium">M:{workload.mediumPriorityCount}</span>
                    <span className="priority-low">L:{workload.lowPriorityCount}</span>
                  </div>
                </div>
              </div>
              <div
                className="agent-workload"
                style={{
                  backgroundColor: `${getWorkloadColor(workload.workloadScore)}15`,
                  borderColor: getWorkloadColor(workload.workloadScore)
                }}
              >
                <span
                  className="workload-score"
                  style={{ color: getWorkloadColor(workload.workloadScore) }}
                >
                  {workload.workloadScore.toFixed(1)}
                </span>
                <span
                  className="workload-label"
                  style={{ color: getWorkloadColor(workload.workloadScore) }}
                >
                  {getWorkloadLabel(workload.workloadScore)} Workload
                </span>
              </div>
              <div className="agent-arrow">→</div>
            </div>
          ))}
        </div>

        {/* Agent Detail Modal */}
        {selectedAgent && (
          <div className="modal-overlay" onClick={closeModal}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
              <button className="modal-close" onClick={closeModal}>×</button>
              
              {loadingDetails ? (
                <div className="modal-loading">Loading agent details...</div>
              ) : agentDetails ? (
                <>
                  <div className="modal-header">
                    <div className="modal-avatar">
                      {agentDetails.fullName?.charAt(0)}
                    </div>
                    <div className="modal-title">
                      <h2>{agentDetails.fullName}</h2>
                      <span className="modal-role">Support Agent</span>
                    </div>
                  </div>

                  <div className="modal-section">
                    <h3>Contact Information</h3>
                    <div className="detail-grid">
                      <div className="detail-item">
                        <span className="detail-icon">📧</span>
                        <div className="detail-content">
                          <span className="detail-label">Email</span>
                          <span className="detail-value">{agentDetails.email}</span>
                        </div>
                      </div>
                      <div className="detail-item">
                        <span className="detail-icon">📞</span>
                        <div className="detail-content">
                          <span className="detail-label">Phone Number</span>
                          <span className="detail-value">{agentDetails.phoneNumber || 'Not set'}</span>
                        </div>
                      </div>
                      <div className="detail-item">
                        <span className="detail-icon">🪪</span>
                        <div className="detail-content">
                          <span className="detail-label">Employee ID</span>
                          <span className="detail-value">{agentDetails.employeeId || 'Not assigned'}</span>
                        </div>
                      </div>
                      <div className="detail-item">
                        <span className="detail-icon">🕐</span>
                        <div className="detail-content">
                          <span className="detail-label">Last Active</span>
                          <span className="detail-value">{formatLastActive(agentDetails.lastActiveAt)}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="modal-section">
                    <h3>Ticket Statistics</h3>
                    <div className="stats-grid">
                      <div className="stat-card stat-not-started">
                        <span className="stat-value">{agentDetails.notStartedCount}</span>
                        <span className="stat-label">Not Started</span>
                      </div>
                      <div className="stat-card stat-in-progress">
                        <span className="stat-value">{agentDetails.inProgressCount}</span>
                        <span className="stat-label">In Progress</span>
                      </div>
                      <div className="stat-card stat-closed">
                        <span className="stat-value">{agentDetails.closedCount}</span>
                        <span className="stat-label">Closed</span>
                      </div>
                    </div>
                  </div>

                  <div className="modal-section">
                    <h3>Workload Score</h3>
                    <div className="workload-display">
                      <div
                        className="workload-score-large"
                        style={{ color: getWorkloadColor(agentDetails.workloadScore || 0) }}
                      >
                        {(agentDetails.workloadScore || 0).toFixed(1)}
                      </div>
                      <div className="workload-info">
                        <span
                          className="workload-level"
                          style={{ color: getWorkloadColor(agentDetails.workloadScore || 0) }}
                        >
                          {getWorkloadLabel(agentDetails.workloadScore || 0)} Workload
                        </span>
                        <span className="workload-desc">
                          Current workload based on active tickets and priorities
                        </span>
                      </div>
                    </div>
                  </div>
                </>
              ) : null}
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default AgentsPage;
