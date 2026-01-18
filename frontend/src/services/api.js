import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api/v1";

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

// Ticket API - Unified for both agents and managers
export const ticketAPI = {
  // Get tickets (role-based: managers see all, agents see assigned)
  getTickets: () => api.get("/tickets"),
  
  // Get tickets grouped by status (for agents)
  getTicketsGrouped: () => api.get("/tickets?grouped=true"),
  
  // Get unassigned tickets (manager only)
  getUnassignedTickets: () => api.get("/tickets?assigned=false"),
  
  // Get ticket details
  getTicketDetails: (ticketId) => api.get(`/tickets/${ticketId}`),
  
  // Update ticket status
  updateStatus: (ticketId, status) =>
    api.patch(`/tickets/${ticketId}/status`, { status }),
  
  // Add comment to ticket
  addComment: (ticketId, content) =>
    api.post(`/tickets/${ticketId}/comments`, { content }),
  
  // Create a new ticket (public)
  createTicket: (data) => api.post("/tickets", data),
  
  // Assign ticket to agent (manager only)
  assignTicket: (ticketId, agentId) =>
    api.patch(`/tickets/${ticketId}/assign`, { agentId }),
  
  // Auto-assign all unassigned tickets (manager only)
  autoAssignAll: () => api.post("/tickets/auto-assign"),
  
  // Search tickets (paginated)
  search: (query, page = 0, size = 10) =>
    api.get(`/tickets?query=${encodeURIComponent(query)}&page=${page}&size=${size}`),
  
  // Autocomplete search
  autocomplete: (query, limit = 5) =>
    api.get(`/tickets/autocomplete?query=${encodeURIComponent(query)}&limit=${limit}`),
};

// Agent API (Manager only)
export const agentAPI = {
  // Get all agents
  getAgents: () => api.get("/agents"),
  
  // Get agent details
  getAgentDetails: (agentId) => api.get(`/agents/${agentId}`),
  
  // Get workloads for all agents
  getWorkloads: () => api.get("/agents/workloads"),
  
  // Get workload for a specific agent
  getWorkload: (agentId) => api.get(`/agents/${agentId}/workload`),
  
  // Get scores for all agents
  getScores: (weeks = 1) => api.get(`/agents/scores?weeks=${weeks}`),
  
  // Get score for a specific agent
  getScore: (agentId) => api.get(`/agents/${agentId}/score`),
};

// User API
export const userAPI = {
  getCurrentUser: () => api.get("/users/me"),
  heartbeat: () => api.post("/users/heartbeat"),
};

export default api;
