import React, { useState, useEffect } from 'react';
import { ticketAPI } from '../services/api';
import TicketCard from '../components/TicketCard';
import Navbar from '../components/Navbar';
import './ManagerDashboardPage.css';

const ManagerDashboardPage = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL');

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      const response = await ticketAPI.getTickets();
      setTickets(response.data);
      setError('');
    } catch (err) {
      setError('Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  const getFilteredTickets = () => {
    if (filterStatus === 'ALL') {
      return tickets;
    }
    return tickets.filter((t) => t.status === filterStatus);
  };

  const getStatusCount = (status) => {
    if (status === 'ALL') return tickets.length;
    return tickets.filter((t) => t.status === status).length;
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
        <div className="manager-container">
          <div className="loading-state">Loading tickets...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="manager-container">
        <div className="manager-header">
          <h1>All Tickets</h1>
          <p>View and manage all tickets in the system</p>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <div className="tabs-container">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`tab-btn ${filterStatus === tab.key ? 'active' : ''}`}
              onClick={() => setFilterStatus(tab.key)}
            >
              {tab.label}
              <span className="tab-count">{getStatusCount(tab.key)}</span>
            </button>
          ))}
        </div>

        <div className="tickets-grid">
          {getFilteredTickets().length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">📭</span>
              <h3>No tickets found</h3>
              <p>There are no tickets in this category</p>
            </div>
          ) : (
            getFilteredTickets().map((ticket) => (
              <TicketCard key={ticket.id} ticket={ticket} showAgent={true} />
            ))
          )}
        </div>
      </div>
    </>
  );
};

export default ManagerDashboardPage;
