# Quick Start Guide

Get the Go Game application up and running in 3 minutes!

## Prerequisites Check

Before starting, ensure you have:

```bash
# Check Java version (need 17+)
java -version
```

That's it! Node.js is only needed if you want to rebuild the frontend.

## Step 1: Build the Application (First Time Only)

```bash
# Give execute permission to gradlew (Unix/Mac)
chmod +x gradlew

# Build the application (includes frontend compilation)
./gradlew build
```

This will compile both the backend and frontend into a single application.

## Step 2: Start the Application

### Option A: Development Mode (Recommended for Development)

```bash
./gradlew quarkusDev
```

You should see:
```
Quarkus started on port 8080
```

The application is now running at `http://localhost:8080`

### Option B: Production Mode

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

You should see:
```
Quarkus started on port 4714
```

The application is now running at `http://localhost:4714`

## Step 3: Open the Application

Open your web browser and go to:
- **Development mode:** `http://localhost:8080`
- **Production mode:** `http://localhost:4714`

The Quarkus server serves both the API and the compiled React frontend!

## Step 4: Create Your First Game

1. **Register an account:**
   - Click "Register here"
   - Enter a username (3-20 characters, alphanumeric + underscore)
   - Enter a password (minimum 8 characters)
   - Click "Register"

2. **Login:**
   - Enter your credentials
   - Click "Login"

3. **Create two accounts** (to test multiplayer):
   - Open an **incognito/private window**
   - Go to the same URL
   - Register a second account
   - Keep both windows open

4. **Create a game:**
   - In the first account, click "New Game"
   - Enter the second account's username as opponent
   - Choose board size (19x19 is standard, 9x9 for quick games)
   - Choose your color (Black plays first)
   - Click "Create Game"

5. **Accept the invitation:**
   - Switch to the second account's window
   - Go to "Pending Invitations" tab
   - Click "Accept" on the game invitation

6. **Play!**
   - Click on the game to open it
   - The player with Black stones goes first
   - Click on intersections to place stones
   - See the board update in real-time in both windows!

## Alternative: Separate Frontend Development

If you want to work on the frontend with hot module replacement (HMR):

```bash
# Terminal 1: Start backend
./gradlew quarkusDev

# Terminal 2: Start frontend dev server
cd frontend
npm install  # First time only
npm run dev
```

Then access the app at `http://localhost:3000` with live HMR.

## Common Issues and Solutions

### Application won't start

**Issue:** Port already in use (8080 for dev, 4714 for production)

**Solution:**
```bash
# Find what's using the port
lsof -i :8080  # Mac/Linux (dev mode)
lsof -i :4714  # Mac/Linux (production)
netstat -ano | findstr :8080  # Windows (dev)
netstat -ano | findstr :4714  # Windows (production)

# Kill the process or change the port in application.properties
```

**Issue:** Java version too old

**Solution:**
```bash
# Install Java 17 or higher
# Mac: brew install openjdk@17
# Ubuntu: sudo apt install openjdk-17-jdk
# Windows: Download from https://adoptium.net/
```

### Frontend doesn't load

**Issue:** Blank page or 404 errors

**Solution:**
```bash
# Rebuild the frontend
./gradlew build
# Restart the server
./gradlew quarkusDev
```

### Can't connect to backend (when using separate frontend dev server)

**Issue:** CORS error in browser console

**Solution:**
Check that `application.properties` includes the frontend dev server URL:
```properties
quarkus.http.cors.origins=http://localhost:3000,http://localhost:5173
```

### SSE (Live Updates) not working

**Issue:** "Connecting..." stays gray

**Solution:**
1. Check browser console for errors
2. Ensure you're using HTTP/1.1 (not HTTP/2)
3. Try refreshing the page
4. Check browser doesn't block EventSource

## Development Tips

### Hot Reload

- **Backend:** Quarkus dev mode automatically reloads on code changes
- **Frontend (in quarkusDev):** Requires rebuild (`./gradlew build`) to see changes
- **Frontend (separate dev server):** Vite automatically reloads on file saves with HMR

### Viewing Data Files

Game data is stored in JSON files:
```bash
# View users
ls data/users/
cat data/users/player1.json

# View games
ls data/games/active/
cat data/games/active/{game-id}.json
```

### Clear All Data

To reset and start fresh:
```bash
rm -rf data/
```

The directories will be recreated automatically when needed.

### Debug Mode

Enable more verbose logging in `application.properties`:
```properties
quarkus.log.category."com.go.game".level=DEBUG
```

## Next Steps

- Read the [README.md](README.md) for detailed documentation
- Check [API.md](API.md) for API endpoint details
- Review [ARCHITECTURE.md](ARCHITECTURE.md) for system design

## Getting Help

If you encounter issues:

1. Check browser console (F12) for frontend errors
2. Check backend terminal for error messages
3. Verify all prerequisites are met
4. Try restarting both backend and frontend

## Stopping the Application

- **Development mode:** Press `q` or `Ctrl+C` in the terminal
- **Production mode:** Press `Ctrl+C` in the terminal
- **Separate frontend dev server:** Press `Ctrl+C` in each terminal

Enjoy playing Go!
