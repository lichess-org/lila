import { Outcome, Result, Matchup } from './types';
import { Script } from './testCtrl';
import * as co from 'chessops';

export function score(outcome: Outcome, color: Color = 'white') {
  return outcome === color ? 1 : outcome === 'draw' ? 0.5 : 0;
}

export function botScore(r: Result, uid: string) {
  return r.outcome === 'draw' ? 0.5 : r[r.outcome] === uid ? 1 : 0;
}

export function outcomesFor(results: Result[], uid: string | undefined) {
  const { w, d, l } = results.reduce(
    (a, r) => ({
      w: a.w + (r.outcome !== 'draw' && r[r.outcome] === uid ? 1 : 0),
      d: a.d + (r.outcome === 'draw' && (r.white === uid || r.black === uid) ? 1 : 0),
      l: a.l + (r.outcome !== 'draw' && r[co.opposite(r.outcome)] === uid ? 1 : 0),
    }),
    { w: 0, d: 0, l: 0 },
  );
  return { wins: w, draws: d, losses: l };
}

export function playerResults(results: Result[], uid?: string) {
  const { wins, losses, draws } = outcomesFor(results, uid);
  return `${wins}/${draws}/${losses}`;
}

export function playersWithResults(script: Script) {
  return [...new Set(script.players.filter(p => script.results.some(r => r.white === p || r.black === p)))];
}
