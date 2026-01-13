import React, { useState, useEffect } from 'react';
import { ticketAPI } from '../services/api';
import TicketCard from '../components/TicketCard';
import Navbar from '../components/Navbar';
import './DashboardPage.css';

const DashboardPage = () => {
  const [ticketsByStatus, setTicketsByStatus] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('ALL');

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      const response = await ticketAPI.getMyTicketsGrouped();
      setTicketsByStatus(response.data);
      setError('');
    } catch (err) {
      setError('Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  const getAllTickets = () => {
    return Object.values(ticketsByStatus).flat();
  };

  const getTicketsForTab = () => {
    if (activeTab === 'ALL') {
      return getAllTickets();
    }
    return ticketsByStatus[activeTab] || [];
  };

  const getTabCount = (tab) => {
    if (tab === 'ALL') {
      return getAllTickets().length;
    }
    return ticketsByStatus[tab]?.length || 0;
  };

  const tabs = [
    { key: 'ALL', label: 'All Tickets' },
    { key: 'NOT_STARTED', label: 'Not Started' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'RESOLVED', label: 'Resolved' },
    { key: 'INVALID', label: 'Invalid' },
  ];

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="dashboard-container">
          <div className="loading-state">Loading tickets...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="dashboard-container">
        <div className="dashboard-header">
          <h1>My Tickets</h1>
          <p>Manage and track your assigned tickets</p>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <div className="tabs-container">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`tab-btn ${activeTab === tab.key ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
              <span className="tab-count">{getTabCount(tab.key)}</span>
            </button>
          ))}
        </div>

        <div className="tickets-grid">
          {getTicketsForTab().length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">📭</span>
              <h3>No tickets found</h3>
              <p>There are no tickets in this category</p>
            </div>
          ) : (
            getTicketsForTab().map((ticket) => (
              <TicketCard key={ticket.id} ticket={ticket} />
            ))
          )}
        </div>
      </div>
    </>
  );
};

export default DashboardPage;
