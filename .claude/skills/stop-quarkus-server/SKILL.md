---
name: stop-quarkus-server
description: Stop the Quarkus dev server. Use when needing to stop the backend server, shut down quarkusDev, or free up port 4714.
allowed-tools: Bash(netstat *), Bash(taskkill *), Bash(lsof *), Bash(kill *)
---

# Stop Quarkus Dev Server

Stop the Quarkus dev server for this project. This skill checks for a running server on port 4714 and stops it if found.

## Instructions

Follow these steps in order:

### Step 1: Check for Running Server

Check if there's a server running on port 4714.

**On Windows:**
```bash
netstat -ano | findstr :4714 | findstr LISTENING
```

**On Linux/macOS:**
```bash
lsof -i :4714 | grep LISTEN
```

- If the output is empty or the command fails, no server is running - report that no server was found and the skill is complete.
- If output shows a PID (the last column on Windows, second column on Linux), proceed to Step 2.

### Step 2: Stop the Server

Extract the PID from the previous output and stop the process.

**On Windows:**
```bash
taskkill //F //PID <PID>
```

**On Linux/macOS:**
```bash
kill -9 <PID>
```

### Step 3: Verify Server is Stopped

Verify the port is now free by running the check from Step 1 again.

**On Windows:**
```bash
netstat -ano | findstr :4714 | findstr LISTENING
```

**On Linux/macOS:**
```bash
lsof -i :4714 | grep LISTEN
```

The command should return empty or fail with exit code 1, indicating no process is listening on the port.

## Platform Detection

Try Windows commands first (netstat, taskkill). If they fail with command not found errors, switch to Linux/macOS commands (lsof, kill).

## Success Criteria

The skill succeeds when:
- No server was running (nothing to stop), OR
- The server was stopped and port 4714 is now free
