# API Documentation

## Base URL

```
Development: http://localhost:8080/api
Production:  http://localhost:4714/api
```

## Authentication

Most endpoints require a JWT token. Include it in the Authorization header:

```
Authorization: Bearer {token}
```

---

## Authentication Endpoints

### Register New User

**POST** `/auth/register`

Create a new user account.

**Request Body:**
```json
{
  "username": "player1",
  "password": "password123"
}
```

**Response (201 Created):**
```json
{
  "message": "User created successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid input
- `409 Conflict`: Username already exists

---

### Login

**POST** `/auth/login`

Authenticate and receive JWT token.

**Request Body:**
```json
{
  "username": "player1",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "player1"
}
```

**Error Responses:**
- `400 Bad Request`: Missing credentials
- `401 Unauthorized`: Invalid credentials

---

## User Endpoints

### Get Current User

**GET** `/users/me`

Get the authenticated user's profile.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "username": "player1",
  "statistics": {
    "gamesPlayed": 10,
    "wins": 6,
    "losses": 4
  },
  "createdAt": "2026-01-24T10:30:00Z"
}
```

---

### Get User Profile

**GET** `/users/{username}`

Get another user's public profile.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "username": "player2",
  "statistics": {
    "gamesPlayed": 15,
    "wins": 9,
    "losses": 6
  }
}
```

**Error Responses:**
- `404 Not Found`: User doesn't exist

---

## Game Endpoints

### Create Game

**POST** `/games`

Create a new game and invite an opponent.

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "boardSize": 19,
  "opponentUsername": "player2",
  "requestedColor": "black"
}
```

**Parameters:**
- `boardSize`: 9, 13, or 19
- `requestedColor`: "black", "white", or "random"

**Response (201 Created):**
```json
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "pending",
  "blackPlayer": "player1",
  "whitePlayer": "player2"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid parameters or opponent not found
- `409 Conflict`: Cannot create game (e.g., playing against self)

---

### List Games

**GET** `/games?status={status}`

Get list of user's games.

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
- `status` (optional): "active", "pending", "completed", or omit for all

**Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "opponent": "player2",
    "yourColor": "black",
    "currentTurn": "black",
    "lastMoveAt": "2026-01-24T11:45:00Z",
    "status": "active",
    "boardSize": 19
  }
]
```

---

### Get Game Details

**GET** `/games/{gameId}`

Get detailed information about a specific game.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "boardSize": 19,
  "blackPlayer": "player1",
  "whitePlayer": "player2",
  "currentTurn": "white",
  "status": "active",
  "createdAt": "2026-01-24T10:30:00Z",
  "lastMoveAt": "2026-01-24T11:45:00Z",
  "moves": [
    {
      "player": "black",
      "action": "place",
      "position": { "x": 3, "y": 3 },
      "timestamp": "2026-01-24T10:31:00Z",
      "capturedStones": []
    }
  ],
  "passes": 0,
  "result": null,
  "boardState": {
    "stones": [
      {
        "position": { "x": 3, "y": 3 },
        "color": "black"
      }
    ]
  }
}
```

**Error Responses:**
- `403 Forbidden`: Not a player in this game
- `404 Not Found`: Game doesn't exist

---

### Accept Game

**POST** `/games/{gameId}/accept`

Accept a pending game invitation.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "message": "Game accepted",
  "status": "active"
}
```

**Error Responses:**
- `400 Bad Request`: Game not pending or wrong player
- `404 Not Found`: Game doesn't exist

---

### Decline Game

**POST** `/games/{gameId}/decline`

Decline a pending game invitation.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "message": "Game declined",
  "status": "cancelled"
}
```

---

### Make Move

**POST** `/games/{gameId}/move`

Place a stone on the board.

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "action": "place",
  "position": { "x": 10, "y": 10 }
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "capturedStones": [
    { "x": 11, "y": 10 }
  ],
  "currentTurn": "white"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Position already occupied"
}
```

**Error Messages:**
- "Position already occupied"
- "Suicide move not allowed"
- "Ko rule violation"
- "It's not your turn"
- "Game is not active"

---

### Pass Turn

**POST** `/games/{gameId}/pass`

Pass your turn without placing a stone.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "passes": 1,
  "currentTurn": "white",
  "status": "active"
}
```

**Note:** Game ends after 2 consecutive passes.

---

### Resign

**POST** `/games/{gameId}/resign`

Forfeit the game.

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "status": "completed",
  "result": {
    "winner": "white",
    "method": "resignation"
  }
}
```

---

## Server-Sent Events (SSE)

### Game Events Stream

**GET** `/games/{gameId}/events`

Establish SSE connection for real-time game updates.

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```
Content-Type: text/event-stream
```

**Event Types:**

**connected:**
```
event: connected
data: SSE connection established
```

**move:**
```
event: move
data: {
  "player": "black",
  "action": "place",
  "position": { "x": 10, "y": 10 },
  "capturedStones": [],
  "currentTurn": "white"
}
```

**pass:**
```
event: pass
data: {
  "player": "white",
  "passes": 1,
  "currentTurn": "black",
  "status": "active"
}
```

**gameEnd:**
```
event: gameEnd
data: {
  "player": "black",
  "winner": "white",
  "method": "resignation"
}
```

---

## Error Codes

- `200 OK`: Success
- `201 Created`: Resource created
- `400 Bad Request`: Invalid input or request
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Not authorized for this action
- `404 Not Found`: Resource doesn't exist
- `409 Conflict`: Conflict with existing data
- `500 Internal Server Error`: Server error

## Rate Limiting

Currently no rate limiting implemented. Consider implementing in production.

## Versioning

Current version: v1 (implicit in `/api` base path)

Future versions may use `/api/v2`, etc.
