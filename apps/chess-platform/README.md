# Chess Platform

A standalone chess product built from first principles, taking architectural lessons from Lila without using Lichess branding.

## Goals
- Real-time online chess
- Accounts, profiles and ratings
- Challenges and matchmaking
- Game history and PGN
- Chat and presence
- Computer analysis
- Tournaments and puzzles
- Variants as separate game rules

## Architecture
- `server/`: TypeScript HTTP/WebSocket API
- `web/`: browser client
- `shared/`: shared domain contracts
- MongoDB: persistent data
- Redis: presence, matchmaking and realtime fan-out
- Stockfish: analysis service

The project intentionally has its own product identity. Any code derived from AGPL-covered Lila components must retain the applicable license and notices.
