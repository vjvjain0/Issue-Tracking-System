import React from 'react';
import { useNavigate } from 'react-router-dom';
import './TicketCard.css';

const TicketCard = ({ ticket, showAgent = false }) => {
  const navigate = useNavigate();

  const getStatusClass = (status) => {
    const statusClasses = {
      NOT_STARTED: 'status-not-started',
      IN_PROGRESS: 'status-in-progress',
      RESOLVED: 'status-resolved',
      INVALID: 'status-invalid',
    };
    return statusClasses[status] || '';
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const handleClick = () => {
    navigate(`/tickets/${ticket.id}`);
  };

  return (
    <div className="ticket-card" onClick={handleClick}>
      <div className="ticket-header">
        <span className={`ticket-status ${getStatusClass(ticket.status)}`}>
          {ticket.status.replace('_', ' ')}
        </span>
        <span className="ticket-date">{formatDate(ticket.createdAt)}</span>
      </div>

      <h3 className="ticket-title">{ticket.title}</h3>

      <p className="ticket-description">
        {ticket.description.length > 100
          ? `${ticket.description.substring(0, 100)}...`
          : ticket.description}
      </p>

      <div className="ticket-footer">
        <div className="ticket-customer">
          <span className="label">Customer:</span>
          <span className="value">{ticket.customerName}</span>
        </div>
        {showAgent && ticket.assignedAgentName && (
          <div className="ticket-agent">
            <span className="label">Agent:</span>
            <span className="value">{ticket.assignedAgentName}</span>
          </div>
        )}
      </div>

      <div className="ticket-meta">
        <span className="meta-item">
          💬 {ticket.comments?.length || 0} comments
        </span>
      </div>
    </div>
  );
};

export default TicketCard;
