import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { ChapterPreview } from '../src/study/interfaces';
import { currentTeamTable, type TeamTable } from '../src/study/relay/relayTeamTable';

test('updates relay team match scores from current chapter statuses', () => {
  const staleTeams: TeamTable = {
    table: [
      {
        teams: [
          { name: 'Team A', points: 1 },
          { name: 'Team B', points: 0 },
        ],
        games: [{ id: 'game-1', pov: 'white' }],
      },
    ],
  };

  const current = currentTeamTable(staleTeams, [
    chapter('game-1', '1-0', 'Team A', 'Team B'),
    chapter('game-2', '1-0', 'Team A', 'Team B'),
    chapter('game-3', '0-1', 'Team C', 'Team D'),
  ]);

  assert.deepEqual(
    current.table.map(row => ({
      games: row.games.map(game => game.id),
      points: row.teams.map(team => team.points),
      teams: row.teams.map(team => team.name),
    })),
    [
      { games: ['game-1', 'game-2'], points: [2, 0], teams: ['Team A', 'Team B'] },
      { games: ['game-3'], points: [0, 1], teams: ['Team C', 'Team D'] },
    ],
  );
});

test('keeps fetched rows when live chapter data cannot recompute them', () => {
  const staleTeams: TeamTable = {
    table: [
      {
        teams: [
          { name: 'Team A', points: 1 },
          { name: 'Team B', points: 0 },
        ],
        games: [{ id: 'game-1', pov: 'white' }],
      },
    ],
  };

  assert.deepEqual(currentTeamTable(staleTeams, []), staleTeams);
});

test('applies relay custom scoring while recomputing team scores', () => {
  const current = currentTeamTable(
    { table: [] },
    [chapter('game-1', '1-0', 'Team A', 'Team B'), chapter('game-2', '½-½', 'Team A', 'Team B')],
    {
      customScoring: {
        black: { draw: 2, win: 4 },
        white: { draw: 1, win: 3 },
      },
    } as any,
  );

  assert.deepEqual(
    current.table.map(row => row.teams.map(team => team.points)),
    [[4, 2]],
  );
});

const chapter = (
  id: string,
  status: ChapterPreview['status'],
  whiteTeam: string,
  blackTeam: string,
): ChapterPreview => ({
  fen: 'startpos',
  id,
  name: id,
  orientation: 'white',
  playing: false,
  players: {
    white: { name: `${whiteTeam} player`, team: whiteTeam },
    black: { name: `${blackTeam} player`, team: blackTeam },
  },
  status,
});
