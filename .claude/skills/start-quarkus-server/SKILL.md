---
name: start-quarkus-server
description: Start the Quarkus dev server. Use when needing to run the backend server, start quarkusDev, launch the dev server, or when another skill (like Playwright testing) needs the backend running.
allowed-tools: Bash(netstat *), Bash(taskkill *), Bash(powershell *), Bash(curl *), Bash(ping *), Bash(lsof *), Bash(kill *), Bash(sleep *), Bash(./gradlew *)
---

# Start Quarkus Dev Server

Start the Quarkus dev server for this project. This skill handles checking for existing servers, stopping them if needed, and verifying the server is running.

## Instructions

Follow these steps in order:

### Step 1: Check for Running Server

Check if there's already a server running on port 4714.

**On Windows:**
```bash
netstat -ano | findstr :4714 | findstr LISTENING
```

**On Linux/macOS:**
```bash
lsof -i :4714 | grep LISTEN
```

- If the output is empty or the command fails, no server is running - skip to Step 3.
- If output shows a PID (the last column on Windows, second column on Linux), proceed to Step 2.

### Step 2: Stop Existing Server

Extract the PID from the previous output and stop the process.

**On Windows:**
```bash
taskkill //F //PID <PID>
```

**On Linux/macOS:**
```bash
kill -9 <PID>
```

After stopping, verify the port is free by running the check from Step 1 again.

### Step 3: Start the Server

Start Quarkus dev server in the background.

**On Windows:**
```bash
powershell -Command "Start-Process -NoNewWindow -FilePath './gradlew.bat' -ArgumentList 'quarkusDev'"
```

**On Linux/macOS:**
```bash
./gradlew quarkusDev &
```

### Step 4: Wait for Startup

Wait approximately 30 seconds for the server to start.

**On Windows:**
```bash
ping 127.0.0.1 -n 31 > nul
```

**On Linux/macOS:**
```bash
sleep 30
```

### Step 5: Verify Server is Running

Confirm the server is responding.

**Option A - HTTP check (preferred):**
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:4714/api/games
```

Expected response: 200, 401, or 403 (any response means the server is up).

**Option B - Port check:**

Windows:
```bash
netstat -ano | findstr :4714 | findstr LISTENING
```

Linux/macOS:
```bash
lsof -i :4714 | grep LISTEN
```

If the server is not responding after 30 seconds, wait an additional 15 seconds and check again.

## Platform Detection

Try Windows commands first (netstat, taskkill, gradlew.bat). If they fail with command not found errors, switch to Linux/macOS commands.

## Success Criteria

The skill succeeds when the server responds to HTTP requests on http://localhost:4714.
