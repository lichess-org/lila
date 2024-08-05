import { clamp } from 'common';
import { getNormal, deepScore } from './util';
import type { SearchResult } from 'zerofish';
import type { MoveArgs } from './types';
import type { ZerofishBot } from './zerofishBot';

const LOG_MOVE_LOGIC = false; //true;

export function zerofishThink(bot: ZerofishBot, args: MoveArgs): number {
  //const maxDelay = bot.operator('delay', args) ?? 4;
  const perMove = Math.min(4, (args.initial ?? Infinity) / 40 + (args.increment ?? 0) * 0.7);
  return clamp((args.secondsRemaining ?? Infinity) / 20, {
    min: 0.25,
    max: ((1.5 + Math.random()) * perMove) / 2,
  });
}

export function zerofishMove(
  fish: SearchResult | undefined,
  zero: SearchResult | undefined,
  bot: ZerofishBot,
  args: MoveArgs,
): { move: Uci; cpl?: number; time: number } {
  const time = zerofishThink(bot, args);
  if ((!fish || fish.bestmove === '0000') && (!zero || zero.bestmove === '0000'))
    return { move: '0000', cpl: 0, time };
  const scored: SearchMove[] = [];
  const byMove: { [uci: string]: SearchMove } = {};
  const pvScore = fish?.lines[0]?.scores[0] ?? args.score ?? 0;
  for (const v of fish?.lines.filter(v => v.moves[0]) ?? []) {
    const move = {
      uci: v.moves[0],
      cpl: Math.abs(pvScore - deepScore(v)),
      weights: {},
    };
    byMove[v.moves[0]] = move;
    scored.push(move);
  }
  const lc0bias = bot.operator('lc0bias', args) ?? 0;
  for (const v of zero?.lines.filter(v => v.moves[0]) ?? []) {
    const uci = v.moves[0];
    const move = (byMove[uci] ??= { uci, weights: {} });
    byMove[uci].weights.lc0 = lc0bias;
    if (!scored.includes(move)) scored.push(move);
  }
  if (LOG_MOVE_LOGIC) console.log('pre-sort', scored);
  scoreAcpl();
  scored.sort(weightSort);
  const decay = bot.operator('decay', args);
  if (decay !== undefined) {
    let mastaP = 0;
    scored.forEach((mv, i) => {
      mv.P = (1 - decay) ** i;
      mastaP += mv.P;
    });
    mastaP = Math.random() * mastaP;
    for (const mv of scored) {
      mastaP -= mv.P!;
      if (mastaP <= 0) {
        if (LOG_MOVE_LOGIC) console.log('decay', decay, mv.uci, mv.P);
        return { move: mv.uci, cpl: mv.cpl, time };
      }
    }
  }
  if (LOG_MOVE_LOGIC) console.log('post-sort', scored[0]?.cpl ?? '', scored);

  return {
    move: scored[0]?.uci,
    cpl: scored[0]?.cpl,
    time,
  };

  function scoreAcpl() {
    if (!bot.operators?.acplMean) return;
    const mean = bot.operator('acplMean', args) ?? 0;
    const stdev = bot.operator('acplStdev', args) ?? 80;
    const targetCpl = Math.max(mean + stdev * getNormal(), 0);
    for (const mv of scored) {
      if (mv.cpl === undefined) continue;
      const distance = Math.abs((mv.cpl ?? 0) - targetCpl);
      const offset = 80;
      const sensitivity = 0.06; // sigmoid
      mv.weights.acpl = distance === 0 ? 1 : 1 / (1 + Math.E ** (sensitivity * (distance - offset)));
    }
  }

  function weightSort(a: SearchMove, b: SearchMove) {
    const wScore = (mv: SearchMove) => Object.values(mv.weights).reduce((acc, w) => acc + (w ?? 0), 0);
    return wScore(b) - wScore(a);
  }
}

type Weights = 'lc0' | 'acpl' | 'aggression';

interface SearchMove {
  uci: Uci;
  score?: number;
  cpl?: number;
  weights: { [key in Weights]?: number };
  P?: number;
}
