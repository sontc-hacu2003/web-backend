# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the auth service (port 10001)
./gradlew backend-auth:bootRun

# Build all modules
./gradlew build

# Build specific module
./gradlew backend-core:build
./gradlew backend-auth:build

# Run tests
./gradlew test
./gradlew backend-auth:test

# Clean build artifacts
./gradlew clean
```

## Module Structure

**Two-module Gradle project** (`settings.gradle`):

| Module | Type | Purpose |
|---|---|---|
| `backend-core` | Library (JAR) | Shared models, filters, repositories, config, security utilities |
| `backend-auth` | Spring Boot app | Auth microservice — runs on port 10001, depends on `backend-core` |

Root `build.gradle` sets `group = sontc`, Java 26, Spring Boot 4.0.6. Root has no bootJar.

## Architecture

### Request pipeline (filter order)

```
LogContextFilter (HIGHEST_PRECEDENCE)
  → MiddlewareEncryptionFilter (HIGHEST_PRECEDENCE + 1)
  → JwtAuthenticationFilter
  → Controllers
```

**LogContextFilter** — extracts/generates `traceId` (from `X-Trace-Id` header or UUID), client IP, URI; stores in ThreadContext (Log4j2) and MDC.

**MiddlewareEncryptionFilter** — for non-excluded POST/PUT/PATCH/DELETE:
- Decrypts incoming request body (ECDH P-256 ephemeral key + AES-GCM)
- Re-wraps the decrypted body so downstream can read it normally via `CachedBodyHttpServletRequest`
- Encrypts the outgoing response symmetrically
- Enabled via `app.middleware.encryption.enabled=true`; excluded paths in `app.middleware.encryption.excluded-paths`

**JwtAuthenticationFilter** — validates HS256 JWT from `Authorization: Bearer <token>`:
- Checks signature, `exp`, `nbf`
- Puts claims into request attributes (`jwtClaims`, `jwtSubject`) and MDC
- Excluded paths: `/auth/**,/login,/register,/error`

No Spring Security — all auth is done through these custom servlet filters.

### Encryption protocol

Server generates a static EC P-256 keypair at startup (via `MiddlewareCryptoService`). Public key available at `GET /auth/public-key`.

Per-request:
1. Client generates ephemeral EC P-256 keypair
2. Client derives AES-256-GCM key via ECDH(clientPrivateKey, serverPublicKey)
3. Encrypted request body: `{ d: <ciphertext>, s: <IV>, p: <clientPublicKey> }` (all base64)
4. Server derives same AES key via ECDH(serverPrivateKey, clientPublicKey), decrypts
5. Server encrypts response: `{ r: <ciphertext>, s: <IV> }`

Frontend interceptor (`src/app/helper/http-interceptor.ts`) mirrors this protocol.

### API response envelope

All responses use `BaseResponse`:
```java
{ code: String, desc: String, data: Object, traceId: String }
```
`code = "00"` → success. `code = "99"` → error. `code = "90"` → validation error.

When encryption is enabled, the entire `BaseResponse` is wrapped in `BaseEncryptedResponse { r, s }`.

### Auth endpoints (`/auth`)

| Method | Path | Auth required | Encrypted |
|---|---|---|---|
| POST | `/auth/signup` | No | No |
| POST | `/auth/signin` | No | No |
| POST | `/auth/reset-password` | No | No |
| GET | `/auth/public-key` | No | No |

`/auth/signin` returns `LoginResponse { user, token, functions[] }` where `functions` is a hierarchical tree built from `CmsFunction` entities filtered by the user's `roleId` (all functions if `CmsRole.isAdmin = true`).

### Permission / menu system

`CmsFunction` is a self-referencing tree (`parentId`). `AuthHelper.buildFunctionTree()` converts the flat DB list to nested `CmsFunctionDto`. The frontend navigation reads `funcCode` to determine render type:
- `"group"` → section header
- `"collapse"` or has children → collapsible item
- leaf with `funcUrl` → nav link

### Password hashing

PBKDF2WithHmacSHA256, 120 000 iterations, 16-byte salt, 256-bit key. Format: `pbkdf2_sha256$iterations$base64_salt$base64_hash` (implemented in `PasswordHash`).

## Configuration

External config loaded at runtime from `config/shared.properties` (relative to working directory):

```properties
# Database
spring.datasource.url=jdbc:postgresql://...supabase.com:5432/postgres
spring.datasource.username=...
spring.datasource.password=...

# CORS
backend.cors.allowed-origins=http://localhost:4200

# JWT
app.security.jwt.secret=web-backend
app.security.jwt.expiration-seconds=3600
app.security.jwt.excluded-paths=/auth/**,/login,/register,/error

# Middleware encryption
app.middleware.encryption.enabled=true
app.middleware.encryption.excluded-paths=/auth/public-key,/auth/signup,...
```

`config/` directory sits at the project root and is shared between modules. Each module's `application.properties` imports it via `spring.config.import=optional:file:config/shared.properties,optional:file:../config/shared.properties`.

Logging: `config/log4j2.properties` — rolling files under `logs/{app.name}/info/` and `logs/{app.name}/error/`, 30-day retention, 10 MB per file.

## Key Paths

| Path | Purpose |
|---|---|
| `backend-core/src/main/java/backend/web/core/filter/` | JWT, encryption, log-context filters |
| `backend-core/src/main/java/backend/web/core/service/MiddlewareCryptoService.java` | ECDH + AES-GCM implementation |
| `backend-core/src/main/java/backend/web/core/helper/PasswordHash.java` | PBKDF2 hash/verify |
| `backend-core/src/main/java/backend/web/core/helper/AuthHelper.java` | Build function tree from flat list |
| `backend-core/src/main/java/backend/web/core/model/entity/admin/` | JPA entities (CmsUser, CmsRole, CmsFunction, CmsRoleFunc) |
| `backend-core/src/main/java/backend/web/core/model/response/base/BaseResponse.java` | Standard response envelope |
| `backend-auth/src/main/java/backend/web/auth/` | Auth controllers + service |
| `config/shared.properties` | Runtime config (DB, JWT, encryption) |
