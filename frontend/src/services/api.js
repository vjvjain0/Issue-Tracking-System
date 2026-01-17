import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// Handle 401 responses
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

// Auth API
export const authAPI = {
  login: (email, password) => api.post("/auth/login", { email, password }),
};

// Ticket API
export const ticketAPI = {
  getMyTickets: () => api.get("/tickets/my-tickets"),
  getMyTicketsGrouped: () => api.get("/tickets/my-tickets/grouped"),
  getTicketDetails: (ticketId) => api.get(`/tickets/${ticketId}`),
  updateStatus: (ticketId, status) =>
    api.patch(`/tickets/${ticketId}/status`, { status }),
  addComment: (ticketId, content) =>
    api.post(`/tickets/${ticketId}/comments`, { content }),
  createTicket: (data) => api.post("/tickets/create", data),
  // Search endpoints for agents
  search: (query, page = 0, size = 10) =>
    api.get(`/tickets/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`),
  autocomplete: (query, limit = 5) =>
    api.get(`/tickets/search/autocomplete?query=${encodeURIComponent(query)}&limit=${limit}`),
};

// Manager API
export const managerAPI = {
  getAllTickets: () => api.get("/manager/tickets"),
  getUnassignedTickets: () => api.get("/manager/tickets/unassigned"),
  assignTicket: (ticketId, agentId) =>
    api.patch(`/manager/tickets/${ticketId}/assign`, { agentId }),
  getAllAgents: () => api.get("/manager/agents"),
  getTicketDetails: (ticketId) => api.get(`/manager/tickets/${ticketId}`),
  // Search endpoints for managers
  search: (query, page = 0, size = 10) =>
    api.get(`/manager/tickets/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`),
  autocomplete: (query, limit = 5) =>
    api.get(`/manager/tickets/search/autocomplete?query=${encodeURIComponent(query)}&limit=${limit}`),
};

// Auto-Assignment API (Manager only)
export const autoAssignAPI = {
  autoAssignAll: () => api.post("/manager/auto-assign/all"),
  getWorkloads: () => api.get("/manager/auto-assign/workloads"),
  getStats: () => api.get("/manager/auto-assign/stats"),
  getCurrentScores: () => api.get("/manager/auto-assign/scores/current"),
  getScoreHistory: (weeks = 4) =>
    api.get(`/manager/auto-assign/scores/history?weeks=${weeks}`),
};

// User API
export const userAPI = {
  getCurrentUser: () => api.get("/users/me"),
};

export default api;
