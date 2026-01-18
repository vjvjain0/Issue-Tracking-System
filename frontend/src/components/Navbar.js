import React, { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ticketAPI } from "../services/api";
import "./Navbar.css";

const Navbar = () => {
  const { user, logout, isManager } = useAuth();
  const navigate = useNavigate();
  
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [showDropdown, setShowDropdown] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const searchRef = useRef(null);
  const debounceRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchRef.current && !searchRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    if (searchQuery.trim().length < 2) {
      setSearchResults([]);
      setTotalCount(0);
      setShowDropdown(false);
      return;
    }

    debounceRef.current = setTimeout(async () => {
      try {
        setIsSearching(true);
        const response = await ticketAPI.autocomplete(searchQuery.trim(), 5);
        setSearchResults(response.data.tickets);
        setTotalCount(response.data.totalCount);
        setShowDropdown(true);
      } catch (err) {
        console.error("Search error:", err);
        setSearchResults([]);
        setTotalCount(0);
      } finally {
        setIsSearching(false);
      }
    }, 300);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [searchQuery]);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      setShowDropdown(false);
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const handleResultClick = (ticketId) => {
    setShowDropdown(false);
    setSearchQuery("");
    navigate(`/tickets/${ticketId}`);
  };

  const handleViewAllResults = () => {
    setShowDropdown(false);
    navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
  };

  const handleProfileClick = () => {
    navigate('/profile');
  };

  const getStatusClass = (status) => {
    const statusClasses = {
      NOT_STARTED: 'status-not-started',
      IN_PROGRESS: 'status-in-progress',
      RESOLVED: 'status-resolved',
      INVALID: 'status-invalid',
    };
    return statusClasses[status] || '';
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

        <div className="navbar-search" ref={searchRef}>
          <form onSubmit={handleSearchSubmit} className="search-form">
            <div className="search-input-wrapper">
              <span className="search-icon">🔍</span>
              <input
                type="text"
                className="search-input"
                placeholder="Search tickets by ID, title, or description..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => searchQuery.trim().length >= 2 && setShowDropdown(true)}
              />
              {isSearching && <span className="search-spinner">⏳</span>}
            </div>
          </form>

          {showDropdown && (
            <div className="search-dropdown">
              {searchResults.length === 0 ? (
                <div className="search-no-results">
                  No tickets found for "{searchQuery}"
                </div>
              ) : (
                <>
                  {searchResults.map((ticket) => (
                    <div
                      key={ticket.id}
                      className="search-result-item"
                      onClick={() => handleResultClick(ticket.id)}
                    >
                      <div className="search-result-top">
                        <span className={`search-result-status ${getStatusClass(ticket.status)}`}>
                          {ticket.status.replace('_', ' ')}
                        </span>
                        <span className="search-result-id">{ticket.id.substring(0, 8)}...</span>
                      </div>
                      <div className="search-result-title">{ticket.title}</div>
                      <div className="search-result-desc">
                        {ticket.description && ticket.description.length > 60
                          ? ticket.description.substring(0, 60) + '...'
                          : ticket.description}
                      </div>
                    </div>
                  ))}
                  {totalCount > 5 && (
                    <button
                      className="search-view-all"
                      onClick={handleViewAllResults}
                    >
                      View all {totalCount} results →
                    </button>
                  )}
                </>
              )}
            </div>
          )}
        </div>

        <div className="navbar-user">
          <div className="user-info-clickable" onClick={handleProfileClick}>
            <span className="user-avatar">
              {user?.name?.charAt(0).toUpperCase()}
            </span>
            <span className="user-details">
              <span className="user-name">{user?.name}</span>
              <span className="user-role">{user?.role}</span>
            </span>
          </div>
          <button onClick={handleLogout} className="logout-btn">
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
