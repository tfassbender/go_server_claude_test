# Go Game Application - Architecture Documentation

## System Overview

This is a full-stack web application implementing the game of Go with enforced rules, real-time updates, and persistent storage.

## Architecture Layers

### 1. Presentation Layer (Frontend)

**Technology**: React + TypeScript

**Components**:
- **Pages**: Login, Register, GameLobby, CreateGame, GamePlay
- **Components**:
  - Auth: LoginForm, RegisterForm
  - Game: Board, GameInfo, GameControls
  - GameList: GameList, GameListItem
  - Layout: Header

**Services**:
- `authService`: User authentication (login/register)
- `gameService`: Game operations (CRUD, moves)
- `userService`: User profile management
- `sseService`: Server-Sent Events connection management
- `apiClient`: Axios instance with JWT interceptor

**State Management**:
- React Hooks (`useState`, `useEffect`)
- Custom hooks: `useAuth`, `useGameEvents`
- LocalStorage for JWT token persistence

### 2. API Layer (REST Endpoints)

**Technology**: Quarkus REST (JAX-RS)

**Resources**:
- `AuthResource`: `/api/auth/*` - Authentication endpoints
- `UserResource`: `/api/users/*` - User profile endpoints
- `GameResource`: `/api/games/*` - Game management endpoints
- `GameEventResource`: `/api/games/{id}/events` - SSE endpoint

**Security**:
- JWT validation on all protected endpoints
- `@RolesAllowed("User")` annotation for authorization
- CORS configuration for frontend access

### 3. Business Logic Layer

**Services**:

**UserService**:
- User registration with validation
- Password authentication
- Statistics updates

**GameService**:
- Game creation and invitation management
- Move validation and execution
- Pass and resign operations
- Board state reconstruction

**GoRulesEngine**:
- Move validation (occupied, liberties, captures)
- Ko rule detection (board hash comparison)
- Suicide rule validation
- Capture detection (DFS for connected groups)

**JwtService**:
- Token generation with expiration
- User claims embedding

**GameEventService**:
- SSE connection management
- Event broadcasting to active connections

### 4. Data Layer

**Technology**: File-based JSON storage

**Repositories**:

**UserRepository**:
- File: `data/users/{username}.json`
- Operations: save, findByUsername, exists, delete
- Atomic writes (temp file + rename)

**GameRepository**:
- Files: `data/games/{status}/{gameId}.json`
- Status directories: pending, active, completed
- Operations: save, findById, findByUser, delete, moveGameFile
- Atomic writes for consistency

**Data Models**:
- `User`: username, passwordHash, createdAt, statistics
- `Game`: id, boardSize, players, moves, status, result
- `Move`: player, action, position, timestamp, capturedStones

### 5. Domain Model (Go Rules)

**Core Classes**:

**Board**:
- 2D grid representation
- Intersection validation
- Group detection (connected stones)
- Liberty counting (BFS/DFS)
- Board state hashing for Ko

**Stone**: Enum (BLACK, WHITE)

**Position**: Coordinates (x, y)

**GoRulesEngine**:
- Move validation pipeline:
  1. Position validity
  2. Intersection empty check
  3. Simulation (place stone)
  4. Capture detection
  5. Suicide rule check
  6. Ko rule check
- Board reconstruction from move history

## Data Flow

### Game Move Flow

```
User clicks intersection
    ↓
Frontend validates turn
    ↓
POST /api/games/{id}/move
    ↓
GameResource.makeMove()
    ↓
GameService.makeMove()
    ↓
GoRulesEngine.validateAndExecuteMove()
    ↓
Board simulation + validation
    ↓
Apply move to game
    ↓
GameRepository.save()
    ↓
GameEventService.broadcastEvent()
    ↓
SSE to all connected clients
    ↓
Frontend receives event
    ↓
Board updates automatically
```

### Real-time Updates Flow

```
User opens game page
    ↓
GET /api/games/{id}/events
    ↓
GameEventResource.streamGameEvents()
    ↓
GameEventService.registerConnection()
    ↓
EventSource connection established
    ↓
Opponent makes move
    ↓
GameEventService.broadcastEvent()
    ↓
SSE event sent to all clients
    ↓
Frontend receives event
    ↓
Game state reloaded
    ↓
UI updates
```

## Security Model

### Authentication
1. User registers with username/password
2. Password hashed with BCrypt (cost factor 12)
3. Credentials stored in user file

### Authorization
1. User logs in
2. Server generates JWT with user claims
3. JWT returned to client
4. Client stores JWT in localStorage
5. Client includes JWT in Authorization header
6. Server validates JWT on each request
7. JWT expiration: 24 hours (configurable)

### Input Validation
- Username: 3-20 chars, alphanumeric + underscore
- Password: min 8 characters
- Board size: 9, 13, or 19 only
- Move coordinates: within board bounds

## Scalability Considerations

### Current Limitations (File-based Storage)
- No horizontal scaling (file locks)
- Limited concurrent access
- No query optimization
- Manual file management

### Future Database Migration Path
1. Replace repositories with JPA entities
2. Use PostgreSQL/MySQL for relational data
3. Maintain same service interfaces
4. Add connection pooling
5. Implement caching layer

### SSE Scalability
- Current: In-memory connection map
- Future: Redis pub/sub for multi-instance
- Consider WebSockets for bidirectional communication

## Performance Optimization

### Backend
- File I/O: Atomic writes, minimize reads
- Board reconstruction: Cache board states
- Move validation: Early exit on invalid moves
- SSE: Connection pooling, event batching

### Frontend
- Component memoization (React.memo)
- Lazy loading for game history
- Debounce rapid clicks on board
- Virtual scrolling for large game lists

## Error Handling

### Backend
- Try-catch blocks in all service methods
- Appropriate HTTP status codes (400, 401, 403, 404, 409, 500)
- Descriptive error messages in response body
- Logging at appropriate levels (ERROR, WARN, INFO, DEBUG)

### Frontend
- Axios interceptor for global error handling
- User-friendly error messages
- Automatic redirect on 401 (token expired)
- Loading states during async operations

## Testing Strategy

### Unit Tests
- Go rules engine (all move validations)
- User service (registration, authentication)
- Game service (move application, captures)

### Integration Tests
- API endpoints with REST Assured
- File repository operations
- JWT token generation/validation

### E2E Tests (Future)
- Complete game flow (register → create → play → complete)
- SSE connection and events
- Error scenarios

## Monitoring & Logging

### Current Implementation
- SLF4J logging framework
- Log levels: DEBUG (dev), INFO (prod)
- Logging points:
  - User registration/login
  - Game creation/moves
  - SSE connections
  - File I/O operations
  - Errors and exceptions

### Future Enhancements
- Centralized logging (ELK stack)
- Metrics (Prometheus + Grafana)
- Performance monitoring (APM)
- Error tracking (Sentry)
