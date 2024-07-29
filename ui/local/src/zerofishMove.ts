import * as co from 'chessops';
import { interpolate } from './operator';
import { clamp } from 'common';
import type { SearchResult, Line } from 'zerofish';
import type { Operators, Operator } from './types';

const LOG_MOVE_LOGIC = true;

type Weights = 'lc0' | 'acpl' | 'aggression';

type SearchMove = {
  uci: Uci;
  score?: number;
  cpl?: number;
  weights: {
    [key in Weights]?: number;
  };
};

export function zerofishMove(
  fish: SearchResult | undefined,
  zero: SearchResult | undefined,
  operators: Operators,
  chess: co.Chess,
): { move: Uci; cpl: number } {
  if ((!fish || fish.bestmove === '0000') && (!zero || zero.bestmove === '0000'))
    return { move: '0000', cpl: 0 };

  const scored: SearchMove[] = [];
  const byMove: { [uci: string]: SearchMove } = {};
  const lc0bias = zero ? from(operators.lc0bias) ?? 0 : 0;
  const lc0decay = zero ? from(operators.lc0decay) ?? 0.5 : 0.5;
  let score: number;

  for (const pv of fish?.pvs.filter(x => x.moves[0]) ?? []) {
    const cp = scoreOf(pv);
    score ??= cp;
    const move = {
      uci: pv.moves[0],
      score: cp,
      shallowScore: scoreOf(pv, 0),
      cpl: Math.abs(score - cp),
      weights: {},
    };
    byMove[pv.moves[0]] = move;
    scored.push(move);
  }
  zero?.pvs
    .filter(x => x.moves[0])
    ?.forEach((pv, i) => {
      const uci = pv.moves[0];
      const move = (byMove[uci] ??= { uci, weights: {} });
      const decay = (1 - lc0decay) ** i;
      byMove[uci].weights.lc0 = isLc0bias() ? lc0bias * decay : decay - Math.random();
      move.cpl ??= scored[scored.length - 1]?.cpl ?? 80;
      if (!scored.includes(move)) scored.push(move);
    });
  score ??= 0;
  if (LOG_MOVE_LOGIC) console.log('pre-sort', scored);
  if (isAcpl()) scoreAcpl();
  const sorted = scored.slice().sort(weightSort);
  const cpl = (fish?.pvs.length ?? 0) > 1 ? sorted[0].cpl! : NaN;
  if (LOG_MOVE_LOGIC) console.log('post-sort cpl', cpl, sorted);
  return {
    move: sorted[0]?.uci,
    cpl,
  };

  function isAcpl() {
    return operators.acplMean && operators.acplStdev !== undefined;
  }

  function isLc0bias() {
    return operators.lc0bias !== undefined;
  }

  function scoreAcpl() {
    const targetCpl = makeTargetCpl();
    for (const mv of scored) {
      const distance = Math.abs((mv.cpl ?? 0) - targetCpl);
      const offset = 80;
      const sensitivity = 0.06; // sigmoid slope
      mv.weights.acpl = distance === 0 ? 1 : 1 / (1 + Math.E ** (sensitivity * (distance - offset)));
    }
  }

  function from(m: Operator | undefined) {
    if (!m) return undefined;
    return interpolate(m, m.from === 'move' ? chess.fullmoves : outcomeExpectancy(score));
  }

  function weightSort(a: SearchMove, b: SearchMove) {
    const wScore = (mv: SearchMove) => Object.values(mv.weights).reduce((acc, w) => acc + (w ?? 0), 0);
    return wScore(b) - wScore(a);
  }

  function makeTargetCpl() {
    const mean = from(operators.acplMean) ?? 0;
    const stdev = from(operators.acplStdev) ?? 0;
    return Math.max(mean + stdev * makeNormal(), 0);
  }

  function scoreOf(pv: Line, depth = pv.scores.length - 1) {
    const sc = pv.scores[clamp(depth, { min: 0, max: pv.scores.length - 1 })];
    return isNaN(sc) ? 0 : clamp(sc, { min: -10000, max: 10000 });
  }

  function outcomeExpectancy(cp: number) {
    return 2 / (1 + 10 ** (-cp / 400)) - 1; // [-1, 1]
  }
}

let nextNormal: number | undefined;

function makeNormal() {
  if (nextNormal !== undefined) {
    const normal = nextNormal;
    nextNormal = undefined;
    return normal;
  }
  const r = Math.sqrt(-2.0 * Math.log(Math.random()));
  const theta = 2.0 * Math.PI * Math.random();
  nextNormal = r * Math.sin(theta);
  return r * Math.cos(theta);
}
