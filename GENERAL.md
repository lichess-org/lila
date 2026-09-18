# CoolChess Project Structure

## Active

### `school-portal/`

The only active website in this repository.

- React + Vite frontend.
- `src/` contains the application code.
- `src/components/` contains the board, bot game, profile, puzzle library and arena.
- `src/data/` contains temporary mock data.
- `package.json` contains the standalone development commands.

Run it with:

```bash
cd school-portal
pnpm install
pnpm dev
```

Build it with:

```bash
cd school-portal
pnpm build
```

## Future / Not Connected

### `future-puzzle/`

Preserved puzzle resources from the former Lila tree. They are not imported by the current website and do not run when the portal starts.

- `source/puzzle-module/` - old Scala puzzle domain module.
- `tools/mongodb-scripts/` - puzzle migration and repair scripts.
- `tools/cron-jobs/` - puzzle denormalization jobs.
- `assets/` - compiled puzzle assets from the old frontend.
- `config/` - old Lila/MongoDB configuration templates.
- `locales/` - puzzle translations.

This folder can be removed when the project switches to a new puzzle API/import service. Before removing it, make sure the real Lichess puzzle dataset has been imported into the new database and the migration scripts are no longer needed.

## Current State

The active frontend still uses local mock data. It does not yet connect to:

- authentication;
- a user database;
- the Lichess puzzle dataset;
- a Maia3 backend;
- school-game WebSockets;
- online presence;
- persistent XP and statistics.

Those integrations are documented in `school-portal/README.md`.
