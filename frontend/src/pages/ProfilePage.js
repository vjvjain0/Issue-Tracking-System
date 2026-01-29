import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Navbar from '../components/Navbar';
import './ProfilePage.css';

const ProfilePage = () => {
  const navigate = useNavigate();
  const { user, isManager, isAgent } = useAuth();

  const getRoleIcon = () => {
    return isManager() ? '👔' : '🎧';
  };

  const getRoleLabel = () => {
    return isManager() ? 'Manager' : 'Support Agent';
  };

  return (
    <>
      <Navbar />
      <div className="profile-container">
        <button className="back-btn" onClick={() => navigate(-1)}>
          ← Back
        </button>

        <div className="profile-card">
          <div className="profile-header">
            <div className="profile-avatar">
              {user?.name?.charAt(0).toUpperCase()}
            </div>
            <div className="profile-info">
              <h1 className="profile-name">{user?.name}</h1>
              <div className="profile-role">
                <span className="role-icon">{getRoleIcon()}</span>
                <span className="role-label">{getRoleLabel()}</span>
              </div>
            </div>
          </div>

          <div className="profile-details">
            <div className="detail-section">
              <h3>Contact Information</h3>
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="detail-icon">📧</span>
                  <div className="detail-content">
                    <span className="detail-label">Email</span>
                    <span className="detail-value">{user?.email}</span>
                  </div>
                </div>
                <div className="detail-item">
                  <span className="detail-icon">📞</span>
                  <div className="detail-content">
                    <span className="detail-label">Phone Number</span>
                    <span className="detail-value">{user?.phoneNumber || 'Not set'}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="detail-section">
              <h3>Employee Information</h3>
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="detail-icon">🪪</span>
                  <div className="detail-content">
                    <span className="detail-label">Employee ID</span>
                    <span className="detail-value">{user?.employeeId || 'Not assigned'}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default ProfilePage;
