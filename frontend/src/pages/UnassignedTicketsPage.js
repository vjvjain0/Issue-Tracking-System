import React, { useState, useEffect } from "react";
import { ticketAPI, agentAPI } from "../services/api";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import "./UnassignedTicketsPage.css";

const UnassignedTicketsPage = () => {
  const [tickets, setTickets] = useState([]);
  const [agents, setAgents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [selectedAgent, setSelectedAgent] = useState("");
  const [assigning, setAssigning] = useState(false);
  const [autoAssigning, setAutoAssigning] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [ticketsRes, agentsRes] = await Promise.all([
        ticketAPI.getUnassignedTickets(),
        agentAPI.getAgents(),
      ]);
      setTickets(ticketsRes.data);
      setAgents(agentsRes.data);
      setError("");
    } catch (err) {
      setError("Failed to load data");
    } finally {
      setLoading(false);
    }
  };

  const handleAssign = async () => {
    if (!selectedTicket || !selectedAgent) return;

    try {
      setAssigning(true);
      await ticketAPI.assignTicket(selectedTicket, selectedAgent);
      setSelectedTicket(null);
      setSelectedAgent("");
      await fetchData();
    } catch (err) {
      setError("Failed to assign ticket");
    } finally {
      setAssigning(false);
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

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="unassigned-container">
          <div className="loading-state">Loading...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="unassigned-container">
        <div className="page-header">
          <div className="header-content">
            <h1>Unassigned Tickets</h1>
            <p>Assign tickets to agents for processing</p>
          </div>
          {tickets.length > 0 && (
            <button
              className="auto-assign-all-btn"
              onClick={handleAutoAssignAll}
              disabled={autoAssigning}
            >
              {autoAssigning ? "⏳ Auto-Assigning..." : "🎯 Auto-Assign All"}
            </button>
          )}
        </div>

        {error && <div className="error-banner">{error}</div>}
        {successMessage && (
          <div className="success-banner">{successMessage}</div>
        )}

        {tickets.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">✅</span>
            <h3>All tickets are assigned</h3>
            <p>There are no unassigned tickets at the moment</p>
          </div>
        ) : (
          <div className="tickets-table-container">
            <table className="tickets-table">
              <thead>
                <tr>
                  <th>Ticket</th>
                  <th>Customer</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>
                      <div className="ticket-info">
                        <span className="ticket-title">{ticket.title}</span>
                        <span className="ticket-desc">
                          {ticket.description && ticket.description.length > 60
                            ? ticket.description.substring(0, 60) + "..."
                            : ticket.description}
                        </span>
                      </div>
                    </td>
                    <td>
                      <div className="customer-info">
                        <span>{ticket.customerName}</span>
                      </div>
                    </td>
                    <td>
                      <StatusBadge status={ticket.status} size="small" />
                    </td>
                    <td>{formatDate(ticket.createdAt)}</td>
                    <td>
                      {selectedTicket === ticket.id ? (
                        <div className="assign-controls">
                          <select
                            value={selectedAgent}
                            onChange={(e) => setSelectedAgent(e.target.value)}
                          >
                            <option value="">Select Agent</option>
                            {agents.map((agent) => (
                              <option key={agent.id} value={agent.id}>
                                {agent.name}
                              </option>
                            ))}
                          </select>
                          <button
                            className="confirm-btn"
                            onClick={handleAssign}
                            disabled={!selectedAgent || assigning}
                          >
                            {assigning ? "..." : "✓"}
                          </button>
                          <button
                            className="cancel-btn"
                            onClick={() => {
                              setSelectedTicket(null);
                              setSelectedAgent("");
                            }}
                          >
                            ✕
                          </button>
                        </div>
                      ) : (
                        <button
                          className="assign-btn"
                          onClick={() => setSelectedTicket(ticket.id)}
                        >
                          Assign
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
};

export default UnassignedTicketsPage;
