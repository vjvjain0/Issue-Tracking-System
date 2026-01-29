# Issue Tracking System

A comprehensive issue tracking system built with Java Spring Boot 3 backend, React frontend, MongoDB database, and Elasticsearch for advanced search capabilities.

## Features

### Agent Features
- Login using work email and password
- View tickets assigned to them in segregated view by status
- Update ticket status (NOT_STARTED → IN_PROGRESS → RESOLVED/INVALID)
- Add comments to tickets
- View history of activities with timestamps
- **Search tickets** by ID, title, or description (only their assigned tickets)

### Manager Features
- View all tickets in the system
- View unassigned tickets
- Manually assign tickets to agents
- **Auto-assign tickets** based on workload and productivity (via Agents page)
- View agent workload and productivity scores (via Agents page)
- **Search all tickets** system-wide by ID, title, or description

### Search Features
- **Powered by Elasticsearch**: Fast, scalable search with fuzzy matching and relevance scoring
- **Real-time autocomplete**: As you type in the search bar, results appear instantly
- **Smart search**: Search by ticket ID, title, or description with fuzzy matching
- **Role-based results**: Agents see only their assigned tickets, managers see all tickets
- **Paginated results**: Full search results page with pagination
- **Result count**: Shows total number of matching tickets
- **Quick navigation**: Click autocomplete results to go directly to ticket details

### Auto-Assignment System
- **Smart ticket distribution** based on agent workload and productivity
- **Workload tracking**: Monitors NOT_STARTED and IN_PROGRESS tickets per agent
- **Productivity scoring**: Weekly scores based on tickets closed (Resolved = 1.0 pts, Invalid = 0.5 pts)
- **Assignment priority**: Agents with lower workload and higher productivity get more tickets
- **Scheduled scoring**: Automatic weekly score calculation every Monday at 1:00 AM (via cron job)

### Ticket Management
- Tickets can be created via public API (for customer app integration)
- Status flow: NOT_STARTED → IN_PROGRESS → INVALID or RESOLVED
- Each ticket has title, description, status, and assigned agent
- Comments and activity history tracking
- Tracks when tickets are closed for productivity scoring
- **Ticket ID display** on cards and detail pages with copy functionality

## Tech Stack

- **Backend**: Java 17 with Spring Boot 3
- **Frontend**: React 18
- **Database**: MongoDB
- **Search Engine**: Elasticsearch 8.x
- **Authentication**: JWT-based authentication

## Prerequisites

- Java 17+
- Node.js 16+
- MongoDB running on localhost:27017
- Elasticsearch 8.x running on localhost:9200

## Getting Started

### 1. Start MongoDB
Make sure MongoDB is running on `localhost:27017`

```bash
# Using homebrew on macOS
brew services start mongodb-community

# Or using Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 2. Start Elasticsearch
Make sure Elasticsearch is running on `localhost:9200`

```bash
# Using homebrew on macOS
brew services start elasticsearch

# Or using Docker
docker run -d -p 9200:9200 -e "discovery.type=single-node" --name elasticsearch elasticsearch:8.11.0
```

**Note**: When using Docker, you'll need to set up passwords and certificates for production use. For development, the single-node configuration works.

### 3. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### 4. Start Frontend

```bash
cd frontend
npm install
npm start
```

The frontend will start on `http://localhost:3000`

## Test Credentials

### Manager
| Email | Password |
|-------|----------|
| manager@company.com | manager123 |

### Agents
| Name | Email | Password |
|------|-------|----------|
| John Smith | john.smith@company.com | agent123 |
| Emily Johnson | emily.johnson@company.com | agent123 |
| Michael Brown | michael.brown@company.com | agent123 |
| Jessica Davis | jessica.davis@company.com | agent123 |
| David Wilson | david.wilson@company.com | agent123 |

## API Endpoints (v1)

All endpoints use the `/api/v1` prefix.

### Authentication
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/login` | Login with email and password | No |

### Ticket Management
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/tickets` | Create a new ticket | No (public) |
| GET | `/api/v1/tickets` | Get all tickets (managers) or assigned tickets (agents) | Yes |
| GET | `/api/v1/tickets?assigned=false` | Get unassigned tickets | Yes (Manager) |
| GET | `/api/v1/tickets?grouped=true` | Get tickets grouped by status | Yes (Agent) |
| GET | `/api/v1/tickets?query={searchText}` | Search tickets | Yes |
| GET | `/api/v1/tickets/autocomplete?query={searchText}` | Autocomplete search | Yes |
| GET | `/api/v1/tickets/{ticketId}` | Get ticket details | Yes |
| PATCH | `/api/v1/tickets/{ticketId}/status` | Update ticket status | Yes (Agent) |
| POST | `/api/v1/tickets/{ticketId}/comments` | Add comment to ticket | Yes (Agent) |
| PATCH | `/api/v1/tickets/{ticketId}/assign` | Assign ticket to agent | Yes (Manager) |
| POST | `/api/v1/tickets/auto-assign` | Auto-assign all unassigned tickets | Yes (Manager) |

### User Management
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/users/me` | Get current user info | Yes |

### Agent Management (Manager only)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/agents` | Get all agents | Yes (Manager) |
| GET | `/api/v1/agents/workloads` | Get all agent workloads | Yes (Manager) |
| GET | `/api/v1/agents/{agentId}/workload` | Get specific agent workload | Yes (Manager) |
| GET | `/api/v1/agents/scores` | Get all agent scores (current week) | Yes (Manager) |
| GET | `/api/v1/agents/scores?weeks={n}` | Get agent scores for last n weeks | Yes (Manager) |
| GET | `/api/v1/agents/{agentId}/score` | Get specific agent's latest score | Yes (Manager) |

## Sample API Calls

### Create Ticket (Customer App Integration)
```bash
curl -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Cannot login to my account",
    "description": "I am getting an error when trying to login",
    "customerEmail": "customer@example.com",
    "customerName": "John Customer"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.smith@company.com",
    "password": "agent123"
  }'
```

### Get All Tickets (as manager)
```bash
curl -X GET http://localhost:8080/api/v1/tickets \
  -H "Authorization: Bearer {token}"
```

### Get Unassigned Tickets (as manager)
```bash
curl -X GET "http://localhost:8080/api/v1/tickets?assigned=false" \
  -H "Authorization: Bearer {token}"
```

### Update Ticket Status
```bash
curl -X PATCH http://localhost:8080/api/v1/tickets/{ticketId}/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

### Assign Ticket to Agent
```bash
curl -X PATCH http://localhost:8080/api/v1/tickets/{ticketId}/assign \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "agentId": "{agentId}"
  }'
```

### Search Tickets
```bash
curl -X GET "http://localhost:8080/api/v1/tickets?query=login&page=0&size=10" \
  -H "Authorization: Bearer {token}"
```

### Auto-Assign All Unassigned Tickets
```bash
curl -X POST http://localhost:8080/api/v1/tickets/auto-assign \
  -H "Authorization: Bearer {token}"
```

### Get Agent Workloads
```bash
curl -X GET http://localhost:8080/api/v1/agents/workloads \
  -H "Authorization: Bearer {token}"
```

## Project Structure

```
Issue Tracking System/
├── backend/
│   ├── src/main/java/com/ticketing/system/
│   │   ├── config/          # Configuration classes
│   │   │   ├── DataInitializer.java           # Initial data setup
│   │   │   └── ElasticsearchIndexInitializer.java # ES index management
│   │   ├── controller/      # REST controllers
│   │   │   ├── AdminController.java          # Admin endpoints
│   │   │   ├── AgentController.java          # /api/v1/agents/*
│   │   │   ├── AuthController.java           # /api/v1/auth/*
│   │   │   ├── TicketController.java         # /api/v1/tickets/*
│   │   │   └── UserController.java           # /api/v1/users/*
│   │   ├── dto/             # Data transfer objects
│   │   ├── exception/       # Exception handling
│   │   ├── model/           # MongoDB and ES documents
│   │   │   ├── Ticket.java                  # MongoDB document
│   │   │   └── TicketDocument.java          # Elasticsearch document
│   │   ├── repository/      # MongoDB and ES repositories
│   │   │   ├── TicketDocumentRepository.java # ES repository
│   │   │   └── ... (MongoDB repositories)
│   │   ├── security/        # JWT security
│   │   └── service/         # Business logic
│   │       ├── TicketElasticsearchService.java # ES operations
│   │       └── ... (other services)
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # Reusable components
│   │   ├── context/         # React context
│   │   ├── pages/           # Page components
│   │   ├── services/        # API services
│   │   └── App.js
│   └── package.json
└── README.md
```

## Data Initialization

On first startup, the system automatically creates:
- 1 Manager account
- 5 Agent accounts
- 50 tickets with various statuses randomly assigned to agents
- **Agent productivity scores for the last 3 weeks**
- **Elasticsearch index** with proper mappings for ticket search

This data is only created if the database is empty. On subsequent startups, the system checks if the Elasticsearch index is synchronized and reindexes if necessary.

## Auto-Assignment Algorithm

The auto-assignment system uses a weighted algorithm to distribute tickets fairly:

### Priority Calculation
```
priority = (0.6 × normalizedWorkload) + (0.4 × normalizedScore)
```

Where:
- **normalizedWorkload** = 1 / (1 + activeTickets) — Lower workload = higher priority
- **normalizedScore** = productivityScore / 20 — Higher productivity = higher priority

### Productivity Score
- Calculated weekly (automatically every Monday at 1 AM)
- **Resolved tickets** = 1.0 point each
- **Invalid tickets** = 0.5 points each
- Used to reward efficient agents with more tickets

### How It Works
1. System calculates current workload for each agent (NOT_STARTED + IN_PROGRESS tickets)
2. Retrieves latest productivity score for each agent
3. Computes assignment priority using the formula above
4. Assigns ticket to agent with highest priority
5. Repeats for each unassigned ticket

## Elasticsearch Configuration

The system uses Elasticsearch for fast, fuzzy search capabilities:

- **Index Name**: `tickets`
- **Document Type**: `TicketDocument` with keyword fields for dates and text fields for searchable content
- **Search Features**:
  - Fuzzy matching with `AUTO` fuzziness on title and description
  - Exact matching on ticket ID (when query matches hex pattern)
  - Role-based filtering (agents see only assigned tickets)
- **Initialization**: Automatic index creation and synchronization on startup
- **Manual Reindexing**: Use `POST /admin/reindex` if needed

For production deployments, configure Elasticsearch security, clustering, and backup strategies.
