# Authentication Documentation

This document explains how authentication is implemented in the web frontend and backend, and how Firebase identity is used to isolate one chess session per user.

## Goal

The application uses Firebase Authentication in the browser to identify a user, obtain a Firebase ID token, and send that token to the Scala backend so the backend can verify the user and load the correct in-memory chess session.

## High-level flow

1. The user signs in in the browser.
2. Firebase Auth establishes a browser session.
3. The frontend obtains a Firebase ID token for the current user.
4. The frontend sends the token in the `Authorization` header.
5. The backend verifies the token against Firebase public keys.
6. The backend extracts the Firebase `uid`.
7. The backend uses that `uid` as the `userId` key for the per-user chess session.

## Frontend authentication methods

The frontend currently supports:

- email/password sign-up
- email/password sign-in
- Google sign-in
- sign-out

These flows live in:

- `web-ui/js/auth.js`
- `web-ui/js/main.js`
- `web-ui/js/api.js`
- `web-ui/js/ui.js`

## Frontend responsibilities

### Firebase initialization

The browser initializes Firebase using the web app configuration from the Firebase console.

The frontend uses browser ESM imports from the Firebase CDN rather than npm or a bundler.

### Auth state tracking

The frontend subscribes to Firebase auth state changes.

When there is no authenticated user:

- the auth status shows signed-out state
- chess controls remain blocked
- the frontend does not call `/game/*`

When a user is authenticated:

- the auth status shows the signed-in identity
- the frontend fetches the current board, status, and replay metadata
- chess controls are enabled

### Token propagation

The frontend API client attaches the current Firebase ID token to authenticated requests:

```http
Authorization: Bearer <firebase-id-token>
```

This is done centrally in `web-ui/js/api.js` through a token-provider callback.

## Backend responsibilities

### Public vs protected routes

- `GET /health` is public
- all `/game/*` routes are protected

### Token verification

The backend verifies the Firebase ID token using Firebase public signing keys.

Relevant files:

- `src/main/scala/makarchess/api/auth/AuthVerifier.scala`
- `src/main/scala/makarchess/api/auth/FirebaseAuthVerifier.scala`
- `src/main/scala/makarchess/api/routes/GameRoutes.scala`

The verifier currently checks:

- token is present
- backend Firebase project id is configured
- JWT algorithm is `RS256`
- key id exists
- issuer matches `https://securetoken.google.com/<projectId>`
- audience matches `<projectId>`
- subject exists and is not empty
- subject length is within Firebase limits

If verification succeeds, the backend builds:

```scala
AuthenticatedUser(userId = firebaseUid, email = maybeEmail)
```

## Per-user session model

The backend stores one `ChessController` per authenticated user.

Relevant file:

- `src/main/scala/makarchess/api/service/GameRegistry.scala`

The registry uses the verified Firebase `uid` as the key.

This means:

- two different Firebase users get different in-memory games
- one user resetting or changing a position does not affect another user
- replay state is also isolated per user

## Request sketch

Example authenticated request:

```bash
curl http://127.0.0.1:8080/game/board \
  -H "Authorization: Bearer <firebase-id-token>"
```

The backend route flow is conceptually:

```text
HTTP request
-> extract Authorization bearer token
-> verify Firebase ID token
-> extract Firebase uid
-> call ApiGameService with userId
-> read or mutate that user's ChessController
-> return JSON response
```

## Startup configuration

The backend must know the Firebase project id.

Supported configuration:

- environment variable: `FIREBASE_PROJECT_ID`
- JVM property: `firebase.projectId`

Examples:

```bash
FIREBASE_PROJECT_ID=makarchess-f1e62 sbt "runMain makarchess.api.ServerApp"
```

```bash
sbt -Dfirebase.projectId=makarchess-f1e62 "runMain makarchess.api.ServerApp"
```

If this is missing, protected routes cannot verify tokens correctly.

## Failure modes

### Missing auth header

Response:

```json
{
  "message": "Authorization bearer token is required."
}
```

### Malformed auth header

Response:

```json
{
  "message": "Authorization header must use Bearer token format."
}
```

### Invalid or unverifiable token

Response shape:

```json
{
  "message": "Unauthorized: ..."
}
```

### Firebase console misconfiguration

Common causes during local development:

- `localhost` missing from Firebase Authorized domains
- Google provider not enabled
- email/password provider not enabled
- frontend using a different Firebase project than the backend expects

## Sketch of the current architecture

```text
Browser UI
  -> Firebase Auth
  -> Firebase ID token
  -> API client adds Bearer token
  -> Scala http4s routes
  -> AuthVerifier / FirebaseAuthVerifier
  -> verified Firebase uid
  -> GameRegistry(userId)
  -> ChessController for that user
```

## Notes for future improvement

Possible future enhancements:

- persist user sessions beyond server restart
- add redirect fallback for Google sign-in if popup flow is unreliable
- add logout/session-expiry handling in a more explicit UI state model
- document frontend deployment auth requirements for hosted domains
