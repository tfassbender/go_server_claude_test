# JWT Authentication Debugging Notes

## Investigation Timeline

### Problem 1: JWT Signing Error (RESOLVED ✓)

**Error**: `SRJWT05028: Signing key can not be created from the loaded content`

**Location**: `JwtService.java:36` in `generateToken()` method

**Root Cause**:
- SmallRye JWT cannot use `.sign()` method alone with HMAC HS256 when the signing key is configured as a base64 string in `application.properties`
- The `.sign()` method expects the key to be auto-discovered via configuration, but this doesn't work reliably with symmetric keys

**Solution Applied**:
Modified `src/main/java/net/tfassbender/game/auth/JwtService.java`:

```java
@ConfigProperty(name = "smallrye.jwt.sign.key")
String signingKey;

public String generateToken(String username) {
    // ... other code ...
    return Jwt.issuer(issuer)
            .upn(username)
            .groups(roles)
            .expiresAt(expiry)
            .issuedAt(now)
            .signWithSecret(signingKey);  // Changed from .sign() to .signWithSecret()
}
```

**Result**: Login endpoint now works correctly and returns 200 OK with valid JWT token.

**Test Credentials**: `testuser123` / `testpass123`

---

### Problem 2: JWT Verification Failure (ONGOING ⚠️)

**Symptom**:
- Login successful (200 OK, token generated)
- All protected endpoints return 401 Unauthorized
- Token appears valid but fails verification

**Configuration Attempted**:

Created `src/main/resources/jwt-secret.jwk`:
```json
{
  "keys": [
    {
      "kty": "oct",
      "alg": "HS256",
      "k": "O+5OGvGw9TtAwbuEuUh/CZh7dpb84RyK8oXjdnLHfLQ="
    }
  ]
}
```

Updated `src/main/resources/application.properties`:
```properties
mp.jwt.verify.issuer=go-game-server
smallrye.jwt.sign.key=O+5OGvGw9TtAwbuEuUh/CZh7dpb84RyK8oXjdnLHfLQ=
smallrye.jwt.verify.key.location=jwt-secret.jwk
smallrye.jwt.verify.algorithm=HS256
smallrye.jwt.sign.key.encrypt=false
```

**What's Been Tried**:
1. ✗ Single JWK object (without "keys" array)
2. ✗ JWK Set format with "keys" array
3. ✗ Using `mp.jwt.verify.publickey.algorithm` (incorrect for symmetric keys)
4. ✗ Direct base64 key in `smallrye.jwt.verify.key`
5. ✗ Current configuration with JWK file + algorithm setting

**Status**: Still returns 401 on protected endpoints despite correct configuration format

**Next Steps**:
- Debug in IntelliJ to trace verification process
- Check Quarkus logs for verification errors
- Verify token structure with jwt.io
- Check if `@RolesAllowed("User")` is matching token groups

---

## Server Management Commands (Windows)

### Start Server (Background)
```bash
powershell -Command "Start-Process -NoNewWindow -FilePath './gradlew.bat' -ArgumentList 'quarkusDev'"
ping 127.0.0.1 -n 31 > nul  # Wait ~30 seconds
```

### Find Running Process
```bash
netstat -ano | findstr :4714 | findstr LISTENING
# Output shows PID: TCP    127.0.0.1:4714    0.0.0.0:0    LISTENING    <PID>
```

### Stop Server
```bash
taskkill //F //PID <PID>
```

### Verify Port is Free
```bash
netstat -ano | findstr :4714
# Should return empty (exit code 1) if port is free
```

**Important Notes**:
- Use `gradlew.bat` (not `./gradlew`) on Windows
- Use double slashes `//` for taskkill flags
- Default port: 4714 (production/dev mode)

---

## IntelliJ Debugging Options

### Option 1: Gradle Task Debug
1. Open Gradle panel (View → Tool Windows → Gradle)
2. Navigate to: Tasks → quarkus → quarkusDev
3. Right-click → Debug
4. Set breakpoints in code

**Issue**: Breakpoints may not work if source is out of sync with compiled code

**Fixes**:
- Rebuild project (Build → Rebuild Project)
- Add to `application.properties`: `%dev.quarkus.live-reload.instrumentation=false`
- Check for "Connected to the target VM" message in debug console

### Option 2: Remote JVM Debug
1. Start server normally: `gradlew.bat quarkusDev`
2. Create new Run Configuration: Run → Edit Configurations → Add New → Remote JVM Debug
3. Settings:
   - Host: localhost
   - Port: 5005
   - Command line args: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
4. Run the configuration
5. Set breakpoints

### Option 3: Gradle Run Configuration
1. Run → Edit Configurations → Add New → Gradle
2. Gradle project: Select this project
3. Tasks: `quarkusDev`
4. Run in Debug mode

---

## Key Files Modified

### `src/main/java/net/tfassbender/game/auth/JwtService.java`
**Purpose**: Generates JWT tokens for authenticated users

**Key Change**: Added explicit signing key injection and used `.signWithSecret()` method

**Lines Modified**: 21-22 (added), 39 (changed)

### `src/main/resources/application.properties`
**Purpose**: JWT configuration for signing and verification

**Key Settings**:
- `smallrye.jwt.sign.key`: Base64 secret for signing
- `smallrye.jwt.verify.key.location`: JWK file for verification
- `smallrye.jwt.verify.algorithm`: HS256 for symmetric key

### `src/main/resources/jwt-secret.jwk` (NEW)
**Purpose**: JWK format required for HMAC verification in Quarkus

**Format**: JWK Set with single symmetric key

---

## Test Endpoints

### Authentication
```bash
# Register
curl -X POST http://localhost:4714/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser123","password":"testpass123"}'

# Login (working)
curl -X POST http://localhost:4714/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser123","password":"testpass123"}'
```

### Protected Endpoints (currently failing with 401)
```bash
# Get active games
curl -X GET "http://localhost:4714/api/games?status=active" \
  -H "Authorization: Bearer <token>"
```

---

## References

### SmallRye JWT Documentation
- HMAC signing requires `.signWithSecret(secret)` or JWK file via `smallrye.jwt.sign.key.location`
- HMAC verification requires `smallrye.jwt.verify.algorithm=HS256` (not `mp.jwt.verify.publickey.algorithm`)
- Symmetric keys must use "oct" key type in JWK format

### Dependencies (build.gradle)
```gradle
implementation 'io.quarkus:quarkus-smallrye-jwt'
implementation 'io.quarkus:quarkus-smallrye-jwt-build'
```

---

## Investigation Status

**Fixed Issues**: ✓
- JWT signing error resolved
- Login endpoint working
- Token generation successful

**Ongoing Issues**: ⚠️
- JWT verification failing on protected endpoints
- IntelliJ breakpoints not triggering (troubleshooting)

**Last Updated**: 2026-01-25
