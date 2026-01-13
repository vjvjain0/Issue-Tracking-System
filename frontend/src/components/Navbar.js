import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Navbar.css";

const Navbar = () => {
  const { user, logout, isManager } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-brand">
          <span className="brand-icon">🎫</span>
          Issue Tracker
        </Link>

        <div className="navbar-menu">
          {isManager() ? (
            <>
              <Link to="/manager/tickets" className="nav-link">
                All Tickets
              </Link>
              <Link to="/manager/unassigned" className="nav-link">
                Unassigned
              </Link>
              <Link to="/manager/agents" className="nav-link">
                Agents
              </Link>
            </>
          ) : (
            <Link to="/dashboard" className="nav-link">
              My Tickets
            </Link>
          )}
        </div>

        <div className="navbar-user">
          <span className="user-info">
            <span className="user-name">{user?.name}</span>
            <span className="user-role">{user?.role}</span>
          </span>
          <button onClick={handleLogout} className="logout-btn">
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
