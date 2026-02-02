# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A full-stack Go (Weiqi/Baduk) board game application with Java/Quarkus backend and React/TypeScript frontend. Uses JWT authentication, Server-Sent Events for real-time updates, and file-based JSON storage.

## Build and Run Commands

### Production Mode (Recommended)
```bash
./gradlew build               # Build backend + frontend (frontend compiled into backend)
./gradlew quarkusDev          # Start server in dev mode (port 8080, auto-reload)
java -jar build/quarkus-app/quarkus-run.jar  # Start production server (port 4714)
```

The Quarkus server serves both the API and the compiled React frontend at the same port.

### Development Mode Options

**Option 1: Full-stack dev mode (Recommended)**
```bash
./gradlew quarkusDev          # Starts on port 8080, serves compiled frontend
```

**Option 2: Separate frontend dev server (for frontend-only development)**
```bash
cd frontend
npm run dev                   # Dev server on port 3000 with Vite HMR
```

### Build Commands
```bash
./gradlew build               # Build both backend and frontend
./gradlew test                # Run tests
cd frontend && npm run lint   # Run ESLint
```

### Notes
- The Gradle `build` task automatically runs `buildFrontend` which compiles React and copies output to `src/main/resources/META-INF/resources`
- In production, only the Quarkus server runs (port 4714)
- In dev mode, Quarkus runs on port 8080

### Starting and Stopping Quarkus Server (Windows)

**Starting the server in background:**
```bash
powershell -Command "Start-Process -NoNewWindow -FilePath './gradlew.bat' -ArgumentList 'quarkusDev'"
ping 127.0.0.1 -n 31 > nul  # Wait ~30 seconds for server to start
```

**Finding the running server process:**
```bash
netstat -ano | findstr :4714 | findstr LISTENING
# Output: TCP    127.0.0.1:4714    0.0.0.0:0    LISTENING    <PID>
```

**Stopping the server:**
```bash
taskkill //F //PID <PID>
# Example: taskkill //F //PID 116432
```

**Verify port is free:**
```bash
netstat -ano | findstr :4714
# Should return empty (exit code 1) if port is free
```

**Notes:**
- Use `gradlew.bat` (not `./gradlew`) on Windows
- Use double slashes `//` for taskkill flags (not single `/`)
- Default ports: 4714 (production/dev), 8080 (alternative dev mode)

## Architecture

### Backend Structure (Java 17, Quarkus 3.6.4)
- **Package**: `net.tfassbender.game`
- **auth/**: JWT authentication (JwtService, PasswordHasher, AuthResource)
- **user/**: User management with file-based storage (UserService, UserRepository)
- **game/**: Game CRUD and real-time events (GameService, GameRepository, GameEventService)
- **go/**: Go rules engine (GoRulesEngine, Board, Stone, Position)
- **Logging**: SLF4J with parameterized logging (e.g., `LOG.info("Message {}", param)`)

### Frontend Structure (React 18, TypeScript, Vite)
- **services/**: API client with JWT interceptor (apiClient.ts), auth/game/user services
- **hooks/**: Custom hooks (useAuth.ts, useGameEvents.ts for SSE)
- **components/**: Auth forms, Game board/controls, Layout header
- **pages/**: GameLobby, CreateGame, GamePlay, Register

### Data Storage
File-based JSON in `data/` directory:
- `data/users/{username}.json`
- `data/games/{pending|active|completed}/{gameId}.json`

### Real-time Updates
SSE endpoint at `/api/games/{gameId}/events` broadcasts move, pass, and gameEnd events to connected clients.

## Key Patterns

### Go Rules Engine
- Move validation: position validity → empty check → simulation → capture detection → suicide check → ko check
- Board state reconstructed from move history
- Board hash used for ko detection

### API Security
- JWT tokens with 24-hour expiration
- `@RolesAllowed("User")` on protected endpoints
- CORS configured for localhost:3000 and localhost:5173 (dev only)
- Production runs on port 4714, dev mode on port 8080

### Frontend State
- React hooks for local state
- JWT stored in localStorage
- Axios interceptor adds Authorization header

## Skills

### start-quarkus-server

Located at `.claude/skills/start-quarkus-server/SKILL.md`.

**Invoke with:** `/start-quarkus-server` or ask Claude to "start the quarkus server", "run quarkusDev", "start the backend", etc.

**What it does:**
1. Checks if a server is already running on port 4714
2. Stops any existing server to free the port
3. Starts quarkusDev in the background
4. Waits for server startup (~30 seconds)
5. Verifies the server is responding

**Usage:** Claude can invoke this skill automatically when needed (e.g., when a Playwright testing skill requires the backend). You can also invoke it directly with `/start-quarkus-server`.

### stop-quarkus-server

Located at `.claude/skills/stop-quarkus-server/SKILL.md`.

**Invoke with:** `/stop-quarkus-server` or ask Claude to "stop the quarkus server", "stop quarkusDev", "stop the backend", "shut down the server", etc.

**What it does:**
1. Checks if a server is running on port 4714
2. Stops the server if found
3. Verifies the port is now free

**Usage:** Invoke directly with `/stop-quarkus-server` or ask Claude to stop the server.
