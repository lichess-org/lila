import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { POVTeamMatch } from '../src/study/relay/interfaces';
import { finishedTeamMatchCount } from '../src/study/relay/relayTeamStandings';

test('counts only finished relay team matches', () => {
  const matches: POVTeamMatch[] = [
    { roundId: 'round-1', opponent: 'Team A', points: '1', mp: 2, gp: 4 },
    { roundId: 'round-2', opponent: 'Team B' },
    { roundId: 'round-3', opponent: 'Team C', points: '0', mp: 0, gp: 1 },
  ];

  assert.equal(finishedTeamMatchCount(matches), 2);
});
