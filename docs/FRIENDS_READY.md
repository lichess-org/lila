# Friend-ready production baseline

This branch is the safe baseline for turning this Lila fork into a private chess site for real games.

## Scope of the first playable release

- Standard chess
- Real-time rated and casual games
- Bullet, blitz, rapid, classical and custom clocks
- Direct challenges between friends
- Game history and PGN
- Player profiles and ratings
- Real-time game state and clocks
- Chat and basic social features
- Analysis board
- Tournaments and Swiss are kept available but are not required for the first private launch
- Lichess-supported variants remain available through the existing Scalachess integration

## Required services

A playable deployment is not just the Scala application. It also needs the services configured by Lila:

- MongoDB
- Redis
- the Lila WebSocket service
- a production reverse proxy/TLS endpoint
- optional Stockfish/fishnet services for server-side analysis

## Required toolchain

The repository's current development guide requires:

- Java 21 JDK
- Node.js 24.20+
- pnpm 12+
- SBT 2.0.9

## Validation gate

Before calling a deployment ready for friends:

1. Backend compile/test passes.
2. Frontend build/tests pass.
3. A user can register and log in.
4. Two separate browsers can challenge each other.
5. Both browsers receive moves and clock updates in real time.
6. Resign, draw and abort work.
7. The completed game appears in both players' histories.
8. Rated games update ratings correctly.
9. Refreshing/reconnecting during a live game does not corrupt the game.
10. Production is served over HTTPS.

This document intentionally does not claim that the repository is already production-ready. The final gate requires running the application and its external services, not only reading source code.
