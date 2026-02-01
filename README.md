# Issue Tracking System

A comprehensive issue tracking system built with Java Spring Boot 3 backend, React frontend, MongoDB database, and Elasticsearch for advanced search capabilities.

## Features

### Agent Features
- Login using work email and password
- View tickets assigned to them in segregated view by status **(sorted by priority: HIGH → MEDIUM → LOW)**
- Update ticket status (NOT_STARTED → IN_PROGRESS → RESOLVED/INVALID)
- Add comments to tickets
- View history of activities with timestamps
- **Search tickets** by ID, title, or description (only their assigned tickets, results sorted by priority)

### Manager Features
- View all tickets in the system
- View unassigned tickets
- **Set ticket priorities** (HIGH, MEDIUM, LOW) for proper assignment and escalation
- Manually assign tickets to agents (requires priority to be set)
- **Auto-assign tickets** based on current workload (via Agents page)
- View agent workload scores and priority breakdowns (via Agents page)
- **Search all tickets** system-wide by ID, title, or description (results sorted by priority)

### Search Features
- **Powered by Elasticsearch**: Fast, scalable search with fuzzy matching and relevance scoring
- **Real-time autocomplete**: As you type in the search bar, results appear instantly
- **Smart search**: Search by ticket ID, title, or description with fuzzy matching
- **Role-based results**: Agents see only their assigned tickets, managers see all tickets
- **Paginated results**: Full search results page with pagination
- **Result count**: Shows total number of matching tickets
- **Quick navigation**: Click autocomplete results to go directly to ticket details

### Priority & Auto-Assignment System
- **Priority levels**: Tickets can be set to HIGH, MEDIUM, or LOW priority by managers
- **SLA-based escalation**: Automatic priority escalation if tickets aren't closed within timeframes:
  - LOW priority tickets escalate to MEDIUM after 7 days
  - MEDIUM priority tickets escalate to HIGH after 3 days
  - HIGH priority tickets are already at maximum priority
- **Smart ticket distribution** based on current workload only
- **Workload calculation**: `0.5 × HIGH + 0.3 × MEDIUM + 0.2 × LOW` priority tickets per agent
- **Assignment priority**: Agents with lower workload scores get assigned tickets first
- **Priority-ordered assignment**: HIGH priority tickets assigned first, then MEDIUM, then LOW

### Ticket Management
- Tickets can be created via public API (for customer app integration)
- **Priority management**: Managers can set HIGH, MEDIUM, or LOW priority levels
- Status flow: NOT_STARTED → IN_PROGRESS → INVALID or RESOLVED
- Each ticket has title, description, status, priority, and assigned agent
- Comments and activity history tracking (including priority changes and SLA escalations)
- **Automatic SLA escalation** based on priority timeframes
- Assignment requires priority to be set first
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
| PATCH | `/api/v1/tickets/{ticketId}/priority` | Update ticket priority | Yes (Manager) |
| POST | `/api/v1/tickets/auto-assign` | Auto-assign all unassigned tickets | Yes (Manager) |
| POST | `/api/v1/tickets/sla-escalation` | Manually trigger SLA escalation check | Yes (Manager) |

### User Management
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/users/me` | Get current user info | Yes |

### Agent Management (Manager only)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/agents` | Get all agents | Yes (Manager) |
| GET | `/api/v1/agents/workloads` | Get all agent workloads and priority breakdowns | Yes (Manager) |
| GET | `/api/v1/agents/{agentId}/workload` | Get specific agent workload | Yes (Manager) |
| GET | `/api/v1/agents/{agentId}` | Get agent details including workload score | Yes (Manager) |

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

### Set Ticket Priority
```bash
curl -X PATCH http://localhost:8080/api/v1/tickets/{ticketId}/priority \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "priority": "HIGH"
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

### Manually Trigger SLA Escalation
```bash
curl -X POST http://localhost:8080/api/v1/tickets/sla-escalation \
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
│   │   │   ├── AgentController.java          # /api/v1/agents/*
│   │   │   ├── AuthController.java           # /api/v1/auth/*
│   │   │   ├── TicketController.java         # /api/v1/tickets/*
│   │   │   └── UserController.java           # /api/v1/users/*
│   │   ├── dto/             # Data transfer objects
│   │   ├── exception/       # Exception handling
│   │   ├── model/           # MongoDB and ES documents
│   │   │   ├── Priority.java                # Priority enum
│   │   │   ├── Ticket.java                  # MongoDB document
│   │   │   └── TicketDocument.java          # Elasticsearch document
│   │   ├── repository/      # MongoDB and ES repositories
│   │   │   ├── TicketDocumentRepository.java # ES repository
│   │   │   └── ... (MongoDB repositories)
│   │   ├── security/        # JWT security
│   │   ├── service/         # Business logic
│   │   │   ├── SlaEscalationService.java    # SLA escalation logic
│   │   │   ├── TicketAutoAssignmentService.java # Auto-assignment logic
│   │   │   ├── TicketElasticsearchService.java # ES operations
│   │   │   └── ... (other services)
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
- 50 tickets with various statuses, **random priorities (HIGH/MEDIUM/LOW)**, randomly assigned to agents
- 3 additional **unassigned tickets without priorities** (for testing auto-assignment)
- **Elasticsearch index** with proper mappings for ticket search

This data is only created if the database is empty. On subsequent startups, the system checks if the Elasticsearch index is synchronized and reindexes if necessary.

## Priority & Auto-Assignment System

### Ticket Priorities
Tickets can be assigned one of three priority levels:
- **HIGH**: Most urgent tickets (require immediate attention)
- **MEDIUM**: Standard priority tickets
- **LOW**: Least urgent tickets

### SLA Escalation Rules
The system automatically escalates ticket priorities if they remain unresolved beyond time limits:
- **LOW** → **MEDIUM** after 7 days
- **MEDIUM** → **HIGH** after 3 days
- **HIGH** priority tickets do not escalate further

Escalation runs every hour automatically, or can be triggered manually by managers.

### Workload-Based Auto-Assignment

The auto-assignment system distributes tickets based on current agent workload:

### Workload Score Calculation
```
workloadScore = (0.5 × highPriorityTickets) + (0.3 × mediumPriorityTickets) + (0.2 × lowPriorityTickets)
```

Where priority tickets are counted from each agent's active tickets (NOT_STARTED + IN_PROGRESS).

### Assignment Priority Order
1. **Priority-based**: HIGH priority tickets assigned first, then MEDIUM, then LOW
2. **Workload-based**: Within each priority level, tickets go to agents with lowest workload scores first
3. **Round-robin**: When agents have equal workloads, tickets are distributed evenly

### Assignment Requirements
- Tickets must have a priority set before they can be assigned (manually or automatically)
- Unassigned tickets without priorities are skipped during auto-assignment

### How It Works
1. System identifies all unassigned tickets with priorities set
2. Groups tickets by priority (HIGH → MEDIUM → LOW)
3. For each priority group:
   - Sorts agents by lowest workload score first
   - Assigns tickets to agents in round-robin fashion within the group
4. Updates ticket assignments and activity logs

## Elasticsearch Configuration

The system uses Elasticsearch for fast, fuzzy search capabilities:

- **Index Name**: `tickets`
- **Document Type**: `TicketDocument` with keyword fields for priority/dates and text fields for searchable content
- **Search Features**:
  - Fuzzy matching with `AUTO` fuzziness on title and description
  - Exact matching on ticket ID (when query matches hex pattern)
  - **Priority-based sorting**: Results sorted HIGH → MEDIUM → LOW → newest first
  - Role-based filtering (agents see only assigned tickets)
- **Initialization**: Automatic index creation and synchronization on startup
- **Manual Reindexing**: Use `POST /admin/reindex` if needed

For production deployments, configure Elasticsearch security, clustering, and backup strategies.
