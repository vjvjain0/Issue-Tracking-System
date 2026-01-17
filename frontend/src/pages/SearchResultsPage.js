import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { ticketAPI, managerAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import TicketCard from '../components/TicketCard';
import Navbar from '../components/Navbar';
import './SearchResultsPage.css';

const SearchResultsPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isManager } = useAuth();
  const query = searchParams.get('q') || '';
  
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const pageSize = 10;

  useEffect(() => {
    if (query) {
      fetchResults();
    } else {
      setTickets([]);
      setLoading(false);
    }
  }, [query, page]);

  const fetchResults = async () => {
    try {
      setLoading(true);
      const api = isManager() ? managerAPI : ticketAPI;
      const response = await api.search(query, page, pageSize);
      setTickets(response.data.tickets);
      setTotalCount(response.data.totalCount);
      setTotalPages(response.data.totalPages);
      setError('');
    } catch (err) {
      setError('Failed to search tickets');
      setTickets([]);
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = (newPage) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const renderPagination = () => {
    if (totalPages <= 1) return null;

    const pages = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(0, page - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
    
    if (endPage - startPage < maxVisiblePages - 1) {
      startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }

    return (
      <div className="pagination">
        <button
          className="pagination-btn"
          onClick={() => handlePageChange(0)}
          disabled={page === 0}
        >
          « First
        </button>
        <button
          className="pagination-btn"
          onClick={() => handlePageChange(page - 1)}
          disabled={page === 0}
        >
          ‹ Prev
        </button>
        
        {startPage > 0 && <span className="pagination-ellipsis">...</span>}
        
        {Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i).map(p => (
          <button
            key={p}
            className={`pagination-btn pagination-number ${p === page ? 'active' : ''}`}
            onClick={() => handlePageChange(p)}
          >
            {p + 1}
          </button>
        ))}
        
        {endPage < totalPages - 1 && <span className="pagination-ellipsis">...</span>}
        
        <button
          className="pagination-btn"
          onClick={() => handlePageChange(page + 1)}
          disabled={page >= totalPages - 1}
        >
          Next ›
        </button>
        <button
          className="pagination-btn"
          onClick={() => handlePageChange(totalPages - 1)}
          disabled={page >= totalPages - 1}
        >
          Last »
        </button>
      </div>
    );
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="search-results-container">
          <div className="loading-state">Searching...</div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="search-results-container">
        <button className="back-btn" onClick={() => navigate(-1)}>
          ← Back
        </button>

        <div className="search-results-header">
          <h1>Search Results</h1>
          <p className="search-query">
            {query ? (
              <>
                Showing results for: <strong>"{query}"</strong>
              </>
            ) : (
              'Enter a search query'
            )}
          </p>
          {totalCount > 0 && (
            <p className="results-count">
              Found <strong>{totalCount}</strong> ticket{totalCount !== 1 ? 's' : ''} matching your search
            </p>
          )}
        </div>

        {error && <div className="error-banner">{error}</div>}

        {!query ? (
          <div className="empty-state">
            <span className="empty-icon">🔍</span>
            <h3>No search query</h3>
            <p>Use the search bar in the navigation to search for tickets</p>
          </div>
        ) : tickets.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">📭</span>
            <h3>No tickets found</h3>
            <p>No tickets match your search criteria "{query}"</p>
          </div>
        ) : (
          <>
            <div className="tickets-grid">
              {tickets.map((ticket) => (
                <TicketCard key={ticket.id} ticket={ticket} showAgent={isManager()} />
              ))}
            </div>
            
            {renderPagination()}
            
            <div className="page-info">
              Showing {page * pageSize + 1} - {Math.min((page + 1) * pageSize, totalCount)} of {totalCount} results
            </div>
          </>
        )}
      </div>
    </>
  );
};

export default SearchResultsPage;
