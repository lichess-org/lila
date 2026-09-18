# CoolChess School Portal

Standalone frontend for the closed CoolChess school platform. The app is intentionally separated from the Lila root so it can evolve as a school product without pulling the whole public-service UI into the browser bundle.

## Run

```bash
cd school-portal
pnpm install
pnpm dev
```

Production build:

```bash
pnpm build
pnpm preview
```

## Current frontend

- Bot game from the standard initial chess position.
- ELO slider for the Maia3-compatible bot mode.
- Responsive chessground board for desktop and mobile.
- School leaderboard with online status.
- Internal school games screen.
- Puzzle library grouped by tactical category: mate, fork, attack.
- Learner profile with XP-based levels: every 100 XP gives one level.
- Detailed activity and XP statistics.
- Warm CoolChess visual palette with red accent and cream surfaces.

The current data source is local mock data. The browser bot is a temporary chess.js legal-move opponent until a Maia3 service is connected.

## Required backend work

### 1. Authentication and roles

- Add school login, logout, refresh-token/session handling.
- Connect the frontend to Lila auth or a dedicated school auth API.
- Add roles: learner, coach, school admin.
- Protect private routes and websocket channels.
- Store the current learner in the frontend session instead of `initialUser`.

### 2. Database

Create persistent collections/tables for:

- users and school membership;
- ratings and XP history;
- completed puzzle attempts;
- puzzle categories, FEN, solution lines, ratings and hints;
- bot games and moves;
- school games and invitations;
- online presence and last-seen timestamps;
- studies/materials and coach access;
- audit events for admin actions.

The frontend mock types in `src/types.ts` should become API DTOs generated from a shared contract.

### 3. Lichess puzzle import

- Decide whether puzzles are copied from the Lichess database or queried through a service.
- Respect the Lichess database license and attribution requirements.
- Build an import job for puzzle id, FEN, UCI solution line, rating, themes and metadata.
- Map Lichess themes to the school filters used in `src/components/Roadmap.tsx`.
- Add pagination, caching and a local puzzle endpoint instead of shipping all puzzles to the browser.
- Add server-side validation that every imported UCI move is legal from the previous position.

### 4. Maia3 bot service

- Run Maia3/Stockfish behind a backend service; do not expose engine credentials or process control in the browser.
- Add an endpoint such as `POST /api/bot/games/:id/move` with game id, FEN and player move.
- Return the engine move, evaluation, clock data and optional explanation.
- Map the ELO slider to Maia model/configuration and enforce server-side limits.
- Replace the temporary random legal move in `src/components/BotGame.tsx` with this API call.

### 5. Real school games

- Add matchmaking or challenge-by-link for school members.
- Use WebSockets for move events, clocks, resignations, draw offers and reconnects.
- Persist PGN and game result.
- Replace the mock `schoolGames` list with a paginated live endpoint.
- Add authorization so a learner can only join games allowed by school policy.

### 6. Presence and community

- Track online/offline state through the websocket gateway and heartbeat.
- Replace `schoolMembers` with a server-side rating query.
- Add pagination and stable ranking tie-break rules.
- Avoid revealing private learner data beyond the school directory policy.

### 7. Profile, XP and statistics

- Move XP calculation to the backend and record an immutable XP ledger.
- Calculate level as `floor(totalXp / 100)` on the server.
- Add daily/weekly/monthly aggregates for games, puzzles, accuracy and activity.
- Return statistics from dedicated endpoints instead of hardcoded values in `src/App.tsx`.

### 8. Coach studies and materials

- Add study/material APIs with coach ownership and learner permissions.
- Support PGN chapters, annotations, assignments and completion state.
- Replace the current placeholder materials screen with real study data.

### 9. Operations and security

- Configure MongoDB/Redis and websocket deployment.
- Add rate limits, request validation, CSRF protection and structured audit logs.
- Add backups and retention policy for games, attempts and personal data.
- Add integration tests for auth, puzzle solving, bot moves, online presence and school games.

## Suggested implementation order

1. Authentication and school membership.
2. Database schema and API contract.
3. Puzzle import and puzzle endpoint.
4. Maia3 backend adapter.
5. Real bot games with persisted PGN.
6. WebSocket school games and presence.
7. Profile XP ledger and statistics.
8. Coach studies/materials.
9. Production hardening, backups and monitoring.
