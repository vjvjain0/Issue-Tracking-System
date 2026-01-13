# Issue Tracking System

A comprehensive issue tracking system built with Java Spring Boot 3 backend, React frontend, and MongoDB database.

## Features

### Agent Features
- Login using work email and password
- View tickets assigned to them in segregated view by status
- Update ticket status (NOT_STARTED → IN_PROGRESS → RESOLVED/INVALID)
- Add comments to tickets
- View history of activities with timestamps

### Manager Features
- View all tickets in the system
- View unassigned tickets
- Manually assign tickets to agents
- **Auto-assign tickets** based on workload and productivity (via Agents page)
- View agent workload and productivity scores (via Agents page)

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

## Tech Stack

- **Backend**: Java 17 with Spring Boot 3
- **Frontend**: React 18
- **Database**: MongoDB
- **Authentication**: JWT-based authentication

## Prerequisites

- Java 17+
- Node.js 16+
- MongoDB running on localhost:27017

## Getting Started

### 1. Start MongoDB
Make sure MongoDB is running on `localhost:27017`

```bash
# Using homebrew on macOS
brew services start mongodb-community

# Or using Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 2. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Start Frontend

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

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with email and password

### Tickets (Agents)
- `GET /api/tickets/my-tickets` - Get all tickets assigned to the logged-in agent
- `GET /api/tickets/my-tickets/grouped` - Get tickets grouped by status
- `GET /api/tickets/{ticketId}` - Get ticket details
- `PATCH /api/tickets/{ticketId}/status` - Update ticket status
- `POST /api/tickets/{ticketId}/comments` - Add comment to ticket

### Tickets (Public - for Customer App)
- `POST /api/tickets/create` - Create a new ticket

### Manager
- `GET /api/manager/tickets` - Get all tickets
- `GET /api/manager/tickets/unassigned` - Get unassigned tickets
- `PATCH /api/manager/tickets/{ticketId}/assign` - Assign ticket to agent
- `GET /api/manager/agents` - Get all agents
- `GET /api/manager/tickets/{ticketId}` - Get ticket details

### Auto-Assignment (Manager only)
- `POST /api/manager/auto-assign/all` - Auto-assign all unassigned tickets
- `GET /api/manager/auto-assign/workloads` - Get agent workload information
- `GET /api/manager/auto-assign/stats` - Get assignment statistics
- `GET /api/manager/auto-assign/scores/current` - Get current week scores
- `GET /api/manager/auto-assign/scores/history?weeks=4` - Get score history

> **Note:** Scores are calculated automatically via a scheduled cron job every Monday at 1:00 AM.

## Sample API Calls

### Create Ticket (Customer App Integration)
```bash
curl -X POST http://localhost:8080/api/tickets/create \
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
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.smith@company.com",
    "password": "agent123"
  }'
```

### Update Ticket Status (with auth token)
```bash
curl -X PATCH http://localhost:8080/api/tickets/{ticketId}/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

## Project Structure

```
Ticketing System/
├── backend/
│   ├── src/main/java/com/ticketing/system/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── exception/      # Exception handling
│   │   ├── model/          # MongoDB documents
│   │   ├── repository/     # MongoDB repositories
│   │   ├── security/       # JWT security
│   │   └── service/        # Business logic
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/     # Reusable components
│   │   ├── context/        # React context
│   │   ├── pages/          # Page components
│   │   ├── services/       # API services
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

This data is only created if the database is empty.

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
