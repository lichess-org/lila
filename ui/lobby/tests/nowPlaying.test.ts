import assert from 'node:assert/strict';
import { test } from 'node:test';

import type { LobbyData, NowPlaying } from '../src/interfaces';
import { updateNowPlayingData } from '../src/nowPlaying';

test('updates now-playing list and tab counts together', () => {
  const data: Pick<LobbyData, 'nowPlaying' | 'nbNowPlaying' | 'nbMyTurn'> = {
    nowPlaying: [pov('old', false)],
    nbNowPlaying: 3,
    nbMyTurn: 0,
  };

  updateNowPlayingData(data, {
    nowPlaying: [pov('fresh-turn', true), pov('fresh-wait', false)],
    nbNowPlaying: 8,
    nbMyTurn: 4,
  });

  assert.deepEqual(
    {
      gameIds: data.nowPlaying.map(p => p.gameId),
      nbNowPlaying: data.nbNowPlaying,
      nbMyTurn: data.nbMyTurn,
    },
    {
      gameIds: ['fresh-turn', 'fresh-wait'],
      nbNowPlaying: 8,
      nbMyTurn: 4,
    },
  );
});

const pov = (gameId: string, isMyTurn: boolean): NowPlaying => ({
  color: 'white',
  fen: '8/8/8/8/8/8/8/8 w - - 0 1' as FEN,
  fullId: `${gameId}white`,
  gameId,
  hasMoved: true,
  isMyTurn,
  lastMove: 'e2e4',
  opponent: {
    id: 'opponent',
    username: 'Opponent',
  },
  perf: 'correspondence',
  rated: false,
  speed: 'correspondence',
  variant: {
    key: 'standard',
    name: 'Standard',
  },
});
