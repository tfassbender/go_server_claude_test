# GNU Go AI Integration Documentation

## Overview

This application integrates **GNU Go**, a lightweight open-source Go AI, to allow users to play against computer opponents. GNU Go was chosen for its simplicity, low resource requirements, and ease of deployment on small servers.

### Why GNU Go?

- **Lightweight**: Single binary, no neural networks, minimal CPU/RAM usage
- **Easy Installation**: One executable file, no complex configuration
- **Perfect for Small Servers**: Runs on any hardware, even resource-constrained systems
- **Battle-Tested**: Stable, mature codebase with standard GTP protocol
- **Strength**: 5-10 kyu (suitable for casual to intermediate players)
- **Free & Open Source**: GPL license

## Architecture

### High-Level Flow

```
User creates game → Select AI difficulty → AI auto-accepts → Game starts
  ↓
Human makes move → Turn switches → AI service detects
  ↓
AI service syncs board state with GNU Go via GTP → Requests move
  ↓
GNU Go returns move → AI service calls GameService.makeMove()
  ↓
Game continues until completion
```

### Core Components

1. **AI Bot Users** - Special accounts (GnuGo-Easy, GnuGo-Casual, etc.) with `isBot = true`
2. **GtpClient** - Manages GNU Go process via stdin/stdout, sends GTP commands
3. **GnuGoService** - High-level operations (move generation, difficulty, coordinate conversion)
4. **AiPlayerService** - Event-driven service that auto-accepts and generates AI moves
5. **Health Monitor** - Checks GNU Go health every 60s, auto-restarts on failure

### Difficulty Levels

GNU Go difficulty is controlled by the `level` command (1-10):

| Bot Username | Level | Estimated Rank | Speed | Description |
|--------------|-------|----------------|-------|-------------|
| GnuGo-Easy | 2 | 15-20 kyu | Fast (<2s) | Beginner - makes simple moves |
| GnuGo-Casual | 4 | 12-15 kyu | Fast (<3s) | Casual - basic patterns |
| GnuGo-Medium | 6 | 10-12 kyu | Medium (3-10s) | Intermediate - decent tactics |
| GnuGo-Hard | 8 | 8-10 kyu | Slow (10-20s) | Advanced - good reading |
| GnuGo-Max | 10 | 5-8 kyu | Very slow (20-30s) | Maximum strength |

**Note**: Level 10 is GNU Go's default. Lower levels trade strength for speed.

## GNU Go Installation & Setup

### Windows Development Setup

1. **Create directory**:
   ```bash
   mkdir ai\gnugo
   ```

2. **Download GNU Go**:
   - Visit: http://www.gnu.org/software/gnugo/download.html
   - Download Windows binary: `gnugo-3.8.exe` (or later)
   - Rename to `gnugo.exe` and place in `ai/gnugo/`

   OR compile from source:
   ```bash
   # Download source
   wget http://ftp.gnu.org/gnu/gnugo/gnugo-3.8.tar.gz
   tar -xzf gnugo-3.8.tar.gz
   cd gnugo-3.8
   ./configure
   make
   cp interface/gnugo.exe ../ai/gnugo/
   ```

3. **Test installation**:
   ```bash
   cd ai\gnugo
   gnugo.exe --version
   # Should output: GNU Go 3.8 (or similar)
   ```

4. **Start server** (GNU Go starts automatically):
   ```bash
   ./gradlew quarkusDev
   ```

### Ubuntu Server Production Setup

**Option 1: Package Manager (Easiest)**

```bash
# Install GNU Go system-wide
sudo apt-get update
sudo apt-get install gnugo

# Create symlink in project
mkdir -p ai/gnugo
ln -s /usr/games/gnugo ai/gnugo/gnugo

# Test
./ai/gnugo/gnugo --version
```

**Option 2: Compile from Source**

```bash
# Download and compile
mkdir -p ai/gnugo
cd ai/gnugo
wget http://ftp.gnu.org/gnu/gnugo/gnugo-3.8.tar.gz
tar -xzf gnugo-3.8.tar.gz
cd gnugo-3.8
./configure
make

# Copy binary
cp interface/gnugo ../gnugo
cd ..
rm -rf gnugo-3.8*

# Test
./gnugo --version
```

**Start server** (GNU Go starts automatically):
```bash
java -jar build/quarkus-app/quarkus-run.jar
```

## Move Generation Flow (Detailed)

### 1. Game Creation with AI Opponent

```
1. User selects "GnuGo-Medium" in UI
2. Frontend calls POST /api/games with opponentUsername="GnuGo-Medium"
3. GameService.createGame() creates pending game
4. GameService fires GameCreatedEvent
5. AiPlayerService observes event, detects AI opponent
6. Calls GameService.acceptGame() automatically
7. acceptGame() fires TurnChangedEvent (black's turn)
```

### 2. AI Move Generation

```
1. TurnChangedEvent fired (currentTurn = "black" or "white")
2. AiPlayerService observes event
3. Checks if current player is AI bot (GnuGoService.isAiBot())
4. If yes, calls executeAiMoveAsync() in background thread
5. GnuGoService.generateMove(botUsername, game):
   a. Syncs board state with GNU Go:
      - Sends "clear_board"
      - Sends "boardsize 19"
      - Sends "komi 5.5"
      - Replays all moves: "play black D4", "play white Q16", etc.
   b. Sets difficulty: "level 6"
   c. Requests move: "genmove black"
   d. GNU Go responds: "= D16" (or "= pass")
   e. Converts GTP coordinate to Position object
   f. Returns Position to AiPlayerService
6. AiPlayerService calls GameService.makeMove() or pass()
7. GameService fires TurnChangedEvent
8. If opponent is human, waits for their move
   If opponent is also AI, repeats flow
```

### 3. GTP Command Sequence Example

For a 19x19 game where black (AI) is about to move:

```
> clear_board
=

> boardsize 19
=

> komi 5.5
=

> play white Q16
=

> play black D4
=

> level 6
=

> genmove black
= D16

```

## GTP Protocol Reference

### Standard GTP Commands Used

| Command | Purpose | Example | Response |
|---------|---------|---------|----------|
| `name` | Get engine name | `name` | `= GNU Go` |
| `version` | Get version | `version` | `= 3.8` |
| `quit` | Shutdown engine | `quit` | `= ` |
| `clear_board` | Clear the board | `clear_board` | `= ` |
| `boardsize N` | Set board size | `boardsize 19` | `= ` |
| `komi X.X` | Set komi value | `komi 5.5` | `= ` |
| `play COLOR MOVE` | Execute a move | `play black D4` | `= ` |
| `genmove COLOR` | Generate a move | `genmove white` | `= Q16` |
| `level N` | Set difficulty (1-10) | `level 6` | `= ` |

### Response Format

GNU Go responses follow standard GTP format:

**Success:**
```
= <result>

```

**Error:**
```
? <error message>

```

Note: Responses end with a blank line.

## Position Coordinate Conversion

### GTP Format vs Internal Format

**GTP Format**: Letters A-T (skip I) for columns, numbers 1-19 for rows
- Example: `D4` = column D (4th), row 4 from bottom

**Internal Format**: 0-based (x, y) where y=0 is top
- Example: `Position(3, 15)` = 4th column, 16th row from top

### Conversion Algorithm

**GTP to Position:**
```java
String gtpMove = "D4";
char colChar = gtpMove.charAt(0);
int row = Integer.parseInt(gtpMove.substring(1));

// Convert column letter to x (skip 'I')
int x = colChar - 'A';
if (x >= 8) x--;  // Adjust for skipped 'I'

// Convert row to y (flip: GTP counts from bottom, we count from top)
int y = boardSize - row;  // 19 - 4 = 15

Position pos = new Position(x, y);  // Result: (3, 15)
```

**Position to GTP:**
```java
Position pos = new Position(3, 15);

// Convert x to column letter (skip 'I')
int xAdjusted = pos.x >= 8 ? pos.x + 1 : pos.x;
char col = (char)('A' + xAdjusted);  // 3 = D

// Convert y to row (flip: we count from top, GTP counts from bottom)
int row = boardSize - pos.y;  // 19 - 15 = 4

String gtp = "" + col + row;  // Result: "D4"
```

### Why 'I' is Skipped

Traditional Go board notation skips the letter 'I' to avoid confusion with '1'. The column sequence is:
```
A B C D E F G H J K L M N O P Q R S T
```

## Testing Procedures

### Manual Testing Checklist

- [ ] **Create game vs each difficulty**
  - [ ] GnuGo-Easy (should accept and move quickly)
  - [ ] GnuGo-Casual
  - [ ] GnuGo-Medium
  - [ ] GnuGo-Hard
  - [ ] GnuGo-Max (slower moves expected)

- [ ] **Verify AI behavior**
  - [ ] AI auto-accepts immediately
  - [ ] AI responds within expected time (Easy <2s, Max <30s)
  - [ ] AI makes legal moves

- [ ] **Test all board sizes**
  - [ ] 9x9 board
  - [ ] 13x13 board
  - [ ] 19x19 board

- [ ] **Complete games**
  - [ ] Play full game to completion
  - [ ] Verify proper scoring
  - [ ] Test pass behavior
  - [ ] Test resignation

- [ ] **Concurrent games**
  - [ ] Start 3+ games simultaneously
  - [ ] Verify all AI bots respond correctly

- [ ] **Error recovery**
  - [ ] Kill GNU Go process mid-game
  - [ ] Verify health monitor detects failure
  - [ ] Verify auto-restart attempts

- [ ] **Human-vs-human games**
  - [ ] Verify human games still work
  - [ ] Verify AI bots not visible in user search

## Troubleshooting Guide

### Problem: GNU Go process won't start

**Solutions:**
- Check that `ai/gnugo/` directory exists
- Verify `gnugo.exe` (Windows) or `gnugo` (Linux) is present and executable
  ```bash
  chmod +x ai/gnugo/gnugo  # Linux only
  ```
- Test GNU Go manually:
  ```bash
  ./ai/gnugo/gnugo --mode gtp
  name
  quit
  ```
- Check logs: Look for "Starting GNU Go process" message
- Enable DEBUG logging in `application.properties`:
  ```
  quarkus.log.category."net.tfassbender.game.ai".level=DEBUG
  ```

### Problem: AI moves too slow

**Solutions:**
- Use lower difficulty levels (Easy, Casual, Medium)
- Check server CPU usage - GNU Go is CPU-intensive at high levels
- Consider level 6 or below for responsive gameplay

### Problem: AI moves invalid or causes errors

**Solutions:**
- Check GTP command sequence in DEBUG logs
- Verify board state synchronization
- Test coordinate conversion with known positions
- Ensure game rules match GNU Go's expectations

### Problem: Health monitor keeps restarting GNU Go

**Solutions:**
- Check if GNU Go process is actually crashing
- Verify GNU Go binary is not corrupted
- Ensure sufficient system resources (RAM, CPU)
- Check for GTP protocol errors in logs

## Operational Guide

### Monitoring Health

**Check GNU Go process:**
```bash
# Windows
tasklist | findstr gnugo

# Linux
ps aux | grep gnugo
```

**Check application logs:**
```bash
# Look for health check messages every 60 seconds
tail -f logs/application.log | grep "GNU Go"
```

**Health indicators:**
- ✅ "GNU Go process started successfully"
- ✅ "GNU Go connected: GNU Go"
- ⚠️ "GNU Go health check failed"
- ❌ "Failed to start GNU Go process"

### Configuration Tuning

**Adjust difficulty levels** (`application.properties`):
```properties
# Make bots faster by lowering levels
ai.bots.config=GnuGo-Easy:1,GnuGo-Casual:3,GnuGo-Medium:5,GnuGo-Hard:7,GnuGo-Max:9

# Or make bots stronger (slower)
ai.bots.config=GnuGo-Easy:3,GnuGo-Casual:5,GnuGo-Medium:7,GnuGo-Hard:9,GnuGo-Max:10
```

**Adjust timeouts**:
```properties
# Increase if moves timing out
ai.gtp.command.timeout=60000  # 60 seconds

# Restart attempts
ai.gtp.process.restart.maxAttempts=5
```

### Performance Characteristics

| Level | Response Time | CPU Usage | Strength |
|-------|---------------|-----------|----------|
| 1-2 | < 2 seconds | Low | Beginner (20 kyu) |
| 3-4 | 2-5 seconds | Low | Casual (15 kyu) |
| 5-6 | 5-10 seconds | Medium | Intermediate (10 kyu) |
| 7-8 | 10-20 seconds | High | Advanced (8 kyu) |
| 9-10 | 20-30+ seconds | High | Strong (5-7 kyu) |

## API Endpoints

### Get AI Bots List

```
GET /api/users/bots
Authorization: Bearer <jwt-token>

Response:
{
  "bots": ["GnuGo-Easy", "GnuGo-Casual", "GnuGo-Medium", "GnuGo-Hard", "GnuGo-Max"]
}
```

### Create Game vs AI

```
POST /api/games
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "opponentUsername": "GnuGo-Medium",
  "boardSize": 19,
  "requestedColor": "black",
  "komi": 5.5
}

Response:
{
  "id": "game-uuid",
  "status": "active",  // Auto-accepted by AI
  "blackPlayer": "yourUsername",
  "whitePlayer": "GnuGo-Medium",
  ...
}
```

## Configuration Reference

### application.properties

```properties
# GNU Go paths and settings
ai.gnugo.enabled=true
ai.gnugo.base.dir=ai/gnugo
ai.gnugo.executable.name.windows=gnugo.exe
ai.gnugo.executable.name.linux=gnugo

# AI bot configuration (username:level format, level 1-10)
ai.bots.config=GnuGo-Easy:2,GnuGo-Casual:4,GnuGo-Medium:6,GnuGo-Hard:8,GnuGo-Max:10

# GTP settings
ai.gtp.command.timeout=30000
ai.gtp.process.restart.maxAttempts=3
```

## Resources

- **GNU Go Official Site**: http://www.gnu.org/software/gnugo/
- **GNU Go Download**: http://www.gnu.org/software/gnugo/download.html
- **GNU Go Documentation**: http://www.gnu.org/software/gnugo/gnugo_toc.html
- **GTP Specification**: https://www.lysator.liu.se/~gunnar/gtp/gtp2-spec-draft2/gtp2-spec.html
- **GNU Go GTP Commands**: http://www.gnu.org/software/gnugo/gnugo_19.html
