# Go Game Web Application

A full-stack web application for playing the board game Go (Weiqi/Baduk) with enforced game rules, user management, and real-time updates.

The main purpose of this project is to test the capabilities of Claude Code in generating a complete full-stack application with complex game logic, user authentication, and real-time features.

## Features

- User registration and authentication with JWT
- Create and manage Go games with configurable board sizes (9x9, 13x13, 19x19)
- Full Go rules implementation (Ko, Suicide, Captures)
- Real-time game updates using Server-Sent Events (SSE)
- Turn-based gameplay with move validation
- Game history and move tracking
- File-based persistence (no database required)

## Technology Stack

### Backend
- **Framework**: Quarkus 3.6.4
- **Language**: Java 17
- **Build Tool**: Gradle
- **Authentication**: JWT (smallrye-jwt)
- **Password Hashing**: BCrypt
- **Real-time Updates**: Server-Sent Events (SSE)
- **Storage**: File-based JSON

### Frontend
- **Framework**: React 18.2
- **Language**: TypeScript
- **Build Tool**: Vite
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **Styling**: CSS

## Project Structure

```
go-server-claude-test/
├── src/main/java/com/go/game/
│   ├── auth/           # JWT authentication
│   ├── user/           # User management
│   ├── game/           # Game management and endpoints
│   └── go/             # Go rules engine
├── frontend/
│   ├── src/
│   │   ├── components/ # React components
│   │   ├── pages/      # Page components
│   │   ├── services/   # API services
│   │   ├── hooks/      # Custom hooks
│   │   └── types/      # TypeScript types
│   └── public/
└── data/               # File storage
    ├── users/
    └── games/
        ├── active/
        ├── pending/
        └── completed/
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Node.js 18 or higher (for initial frontend build only)
- npm or yarn (for initial frontend build only)

### Quick Start (Production Build)

1. Navigate to the project root directory

2. Build the application (includes both backend and frontend):
   ```bash
   ./gradlew build
   ```

3. Run the application:
   ```bash
   java -jar build/quarkus-app/quarkus-run.jar
   ```

   The application will start on `http://localhost:4714`

4. Open your browser and navigate to `http://localhost:4714`

### Development Mode

Run the application in development mode with auto-reload:

```bash
./gradlew quarkusDev
```

The application will start on `http://localhost:8080` and serve both the API and the compiled frontend.

### Separate Frontend Development (Optional)

If you want to work on the frontend with hot module replacement (HMR):

1. Start the Quarkus backend:
   ```bash
   ./gradlew quarkusDev
   ```

2. In a separate terminal, start the frontend dev server:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   The frontend dev server will start on `http://localhost:3000` with HMR enabled.

## API Endpoints

### Authentication

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and receive JWT token

### Users

- `GET /api/users/me` - Get current user profile
- `GET /api/users/{username}` - Get user profile by username

### Games

- `POST /api/games` - Create a new game
- `GET /api/games?status={status}` - List user's games (status: active, pending, completed)
- `GET /api/games/{gameId}` - Get game details
- `POST /api/games/{gameId}/accept` - Accept game invitation
- `POST /api/games/{gameId}/decline` - Decline game invitation
- `POST /api/games/{gameId}/move` - Make a move
- `POST /api/games/{gameId}/pass` - Pass turn
- `POST /api/games/{gameId}/resign` - Resign from game

### Real-time Updates

- `GET /api/games/{gameId}/events` - Server-Sent Events stream for game updates

## Game Rules

The application implements Japanese Go rules:

1. **Turn-based gameplay**: Black plays first, then players alternate
2. **Stone placement**: Stones are placed on intersections, not squares
3. **Captures**: Stones with no liberties (empty adjacent intersections) are captured
4. **Ko rule**: Cannot immediately recapture a single stone (prevents infinite loops)
5. **Suicide rule**: Cannot place a stone with no liberties unless it captures opponent stones
6. **Game end**: Game ends when both players pass consecutively or one player resigns

## Configuration

### Backend Configuration

Edit `src/main/resources/application.properties`:

```properties
# HTTP Port
quarkus.http.port=4714                # Production port
%dev.quarkus.http.port=8080           # Development mode port

# CORS (for frontend dev server)
quarkus.http.cors.origins=http://localhost:3000,http://localhost:5173

# JWT Settings
jwt.expiration.hours=24
smallrye.jwt.sign.key=your-secret-key

# Data Directory
app.data.directory=data
```

### Frontend Configuration

Edit `frontend/vite.config.ts` to change the proxy settings if the backend runs on a different port.

## Development

### Building for Production

Build the entire application (backend + frontend):

```bash
./gradlew build
```

This will:
1. Build the frontend React application
2. Copy the compiled frontend to `src/main/resources/META-INF/resources`
3. Package the Quarkus backend with the embedded frontend

Run the production build:

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

The application will be available at `http://localhost:4714` serving both the API and the frontend.

## Usage Guide

1. **Register an Account**: Create a new account with a unique username and password (min 8 characters)

2. **Create a Game**:
   - Click "New Game" in the navigation
   - Enter opponent's username
   - Choose board size (9x9, 13x13, or 19x19)
   - Select your color or random assignment
   - Click "Create Game"

3. **Accept Invitation**:
   - Go to "Pending Invitations" tab
   - Accept or decline game invitations

4. **Play Game**:
   - Click on an active game from the lobby
   - Click on intersections to place stones
   - Use "Pass" button to skip your turn
   - Use "Resign" button to forfeit the game

5. **Real-time Updates**:
   - When viewing a game, you'll see a green indicator showing live updates are enabled
   - The board will automatically update when your opponent makes a move

## Troubleshooting

### Application doesn't start
- Ensure Java 17 is installed: `java -version`
- Check if port 4714 (production) or 8080 (dev) is available
- Check `data/` directory has write permissions

### Frontend doesn't load
- Verify the application was built with `./gradlew build`
- Check that the frontend compiled successfully (look for build logs)
- Navigate to `http://localhost:4714` (production) or `http://localhost:8080` (dev)

### Frontend dev server doesn't connect to backend
- Verify backend is running on port 8080 (dev mode)
- Check browser console for CORS errors
- Verify proxy configuration in `frontend/vite.config.ts`

### SSE connection fails
- SSE requires HTTP/1.1 (not HTTP/2)
- Some browser extensions may block SSE connections
- Check browser console for connection errors

## Future Enhancements

- Game replay system with move-by-move navigation
- Territory scoring system
- Game analysis and variation explorer
- User rating/ranking system
- Game chat functionality
- Time controls
- AI opponent integration

## License

MIT

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
