import React from 'react';
import './StatusBadge.css';

const StatusBadge = ({ status, size = 'medium' }) => {
  const getStatusClass = (status) => {
    const statusClasses = {
      NOT_STARTED: 'badge-not-started',
      IN_PROGRESS: 'badge-in-progress',
      RESOLVED: 'badge-resolved',
      INVALID: 'badge-invalid',
    };
    return statusClasses[status] || '';
  };

  return (
    <span className={`status-badge ${getStatusClass(status)} badge-${size}`}>
      {status.replace('_', ' ')}
    </span>
  );
};

export default StatusBadge;
