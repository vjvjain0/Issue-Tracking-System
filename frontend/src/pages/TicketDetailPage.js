import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ticketAPI, managerAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import Navbar from '../components/Navbar';
import StatusBadge from '../components/StatusBadge';
import './TicketDetailPage.css';

const TicketDetailPage = () => {
  const { ticketId } = useParams();
  const navigate = useNavigate();
  const { isManager, isAgent } = useAuth();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [newComment, setNewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [activeSection, setActiveSection] = useState('comments');

  useEffect(() => {
    fetchTicket();
  }, [ticketId]);

  const fetchTicket = async () => {
    try {
      setLoading(true);
      const response = isManager()
        ? await managerAPI.getTicketDetails(ticketId)
        : await ticketAPI.getTicketDetails(ticketId);
      setTicket(response.data);
      setError('');
    } catch (err) {
      setError('Failed to load ticket details');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (newStatus) => {
    try {
      setSubmitting(true);
      await ticketAPI.updateStatus(ticketId, newStatus);
      await fetchTicket();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update status');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    try {
      setSubmitting(true);
      await ticketAPI.addComment(ticketId, newComment);
      setNewComment('');
      await fetchTicket();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add comment');
    } finally {
      setSubmitting(false);
    }
  };

  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getNextStatuses = () => {
    if (!ticket) return [];

    switch (ticket.status) {
      case 'NOT_STARTED':
        return ['IN_PROGRESS'];
      case 'IN_PROGRESS':
        return ['RESOLVED', 'INVALID'];
      default:
        return [];
    }
  };

  const getActionIcon = (action) => {
    const icons = {
      TICKET_CREATED: '📝',
      TICKET_ASSIGNED: '👤',
      STATUS_CHANGED: '🔄',
      COMMENT_ADDED: '💬',
    };
    return icons[action] || '📌';
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="detail-container">
          <div className="loading-state">Loading ticket details...</div>
        </div>
      </>
    );
  }

  if (error && !ticket) {
    return (
      <>
        <Navbar />
        <div className="detail-container">
          <div className="error-state">{error}</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="detail-container">
        <button className="back-btn" onClick={() => navigate(-1)}>
          ← Back
        </button>

        {error && <div className="error-banner">{error}</div>}

        <div className="ticket-detail-layout">
          <div className="ticket-main">
            <div className="ticket-header-section">
              <StatusBadge status={ticket.status} size="large" />
              <h1 className="ticket-title">{ticket.title}</h1>
            </div>

            <div className="ticket-info-grid">
              <div className="info-item">
                <span className="info-label">Customer</span>
                <span className="info-value">{ticket.customerName}</span>
                <span className="info-sub">{ticket.customerEmail}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Assigned To</span>
                <span className="info-value">{ticket.assignedAgentName || 'Unassigned'}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Created</span>
                <span className="info-value">{formatDateTime(ticket.createdAt)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Last Updated</span>
                <span className="info-value">{formatDateTime(ticket.updatedAt)}</span>
              </div>
            </div>

            <div className="description-section">
              <h3>Description</h3>
              <p>{ticket.description}</p>
            </div>

            {isAgent() && getNextStatuses().length > 0 && (
              <div className="status-actions">
                <h3>Update Status</h3>
                <div className="status-buttons">
                  {getNextStatuses().map((status) => (
                    <button
                      key={status}
                      className={`status-action-btn status-btn-${status.toLowerCase().replace('_', '-')}`}
                      onClick={() => handleStatusChange(status)}
                      disabled={submitting}
                    >
                      Move to {status.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div className="section-tabs">
              <button
                className={`section-tab ${activeSection === 'comments' ? 'active' : ''}`}
                onClick={() => setActiveSection('comments')}
              >
                Comments ({ticket.comments?.length || 0})
              </button>
              <button
                className={`section-tab ${activeSection === 'activity' ? 'active' : ''}`}
                onClick={() => setActiveSection('activity')}
              >
                Activity History ({ticket.activities?.length || 0})
              </button>
            </div>

            {activeSection === 'comments' && (
              <div className="comments-section">
                {isAgent() && (
                  <form className="comment-form" onSubmit={handleAddComment}>
                    <textarea
                      value={newComment}
                      onChange={(e) => setNewComment(e.target.value)}
                      placeholder="Add a comment..."
                      rows={3}
                    />
                    <button type="submit" disabled={submitting || !newComment.trim()}>
                      {submitting ? 'Adding...' : 'Add Comment'}
                    </button>
                  </form>
                )}

                <div className="comments-list">
                  {ticket.comments?.length === 0 ? (
                    <div className="empty-comments">No comments yet</div>
                  ) : (
                    [...ticket.comments].reverse().map((comment) => (
                      <div key={comment.id} className="comment-item">
                        <div className="comment-header">
                          <span className="comment-author">{comment.userName}</span>
                          <span className="comment-date">{formatDateTime(comment.createdAt)}</span>
                        </div>
                        <p className="comment-content">{comment.content}</p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}

            {activeSection === 'activity' && (
              <div className="activity-section">
                <div className="activity-list">
                  {[...ticket.activities].reverse().map((activity) => (
                    <div key={activity.id} className="activity-item">
                      <span className="activity-icon">{getActionIcon(activity.action)}</span>
                      <div className="activity-content">
                        <p className="activity-details">{activity.details}</p>
                        <p className="activity-meta">
                          by {activity.userName} • {formatDateTime(activity.timestamp)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default TicketDetailPage;
