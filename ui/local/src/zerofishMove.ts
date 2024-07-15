import * as co from 'chessops';
import { interpolate } from './operator';
import { clamp } from 'common';
import type { SearchResult, Line } from 'zerofish';
import type { Operators, Operator } from './types';

let nextNormal: number | undefined;

const prices: { readonly [role in co.Role]?: number } = {
  pawn: 1,
  knight: 2.8,
  bishop: 3,
  rook: 5,
  queen: 9,
};

type Weights = 'acpl' | 'lc0' | 'aggression';

type SearchMove = {
  uci: Uci;
  score?: number;
  cpl?: number;
  weights: {
    [key in Weights]?: number;
  };
};

type MoveContext = {
  fish: SearchResult | undefined;
  zero: SearchResult | undefined;
  operators: Operators;
  chess: co.Chess;
  score?: number;
  scored: SearchMove[];
  byMove: { [uci: string]: SearchMove };
};

export function zerofishMove(
  fish: SearchResult | undefined,
  zero: SearchResult | undefined,
  operators: Operators,
  chess: co.Chess,
): { move: Uci; cpl: number } {
  const scored: SearchMove[] = [];
  const byMove: { [uci: string]: SearchMove } = {};
  //const zmoves: string[] = [];
  const weights: number[] = [];
  const lc0bias = zero ? (operators.lc0bias ? from(operators.lc0bias) : 0.5) : 0;
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
  for (const pv of zero?.pvs.filter(x => x.moves[0]) ?? []) {
    const uci = pv.moves[0];
    //zmoves.push(uci);
    const move = (byMove[uci] ??= { uci, weights: {} });
    byMove[uci].weights.lc0 = lc0bias;
    move.cpl ??= scored[scored.length - 1]?.cpl ?? 80;
    if (!scored.includes(move)) scored.push(move);
  }
  score ??= 0;
  if (isAcpl()) applyAcpl();
  const sorted = scored.slice();
  if (isAcpl()) sorted.sort(weightSort);
  else if (isLc0bias()) {
    const chance = Math.random();
    const index = sorted.findIndex(mv => (mv.weights.lc0 ?? 0) > chance);
    if (index !== -1) sorted.unshift(...sorted.splice(index, 1));
  }
  const byMaterial = scored.slice();
  byMaterial.sort(aggressionSort);
  for (const mv of byMaterial) {
    const fsh = fish?.pvs.find(pv => pv.moves[0] === mv.uci);
    const dest = fsh ? byDestruction(fsh) : NaN;
    console.log(dest, fsh);
  }
  const cpl = (fish?.pvs.length ?? 0) > 1 ? sorted[0].cpl! : NaN;

  return {
    move: sorted[0]?.uci,
    cpl,
  };

  function aggressionSort(a: SearchMove, b: SearchMove) {
    const afish = fish?.pvs.find(pv => pv.moves[0] === a.uci);
    const bfish = fish?.pvs.find(pv => pv.moves[0] === b.uci);
    if (!bfish && !afish) return 0;
    if (!bfish) return -1;
    if (!afish) return 1;
    return byDestruction(bfish, false) - byDestruction(afish, false);
  }

  function byDestruction(pv: Line, mutual = false) {
    const beforeMaterial = co.Material.fromBoard(chess.board);
    const opponent = co.opposite(chess.turn);
    const before = weigh(mutual ? beforeMaterial : beforeMaterial[opponent]);
    try {
      const pvChess = chess.clone();
      for (const move of pv.moves) pvChess.play(co.parseUci(move)!);
      const afterMaterial = co.Material.fromBoard(pvChess.board);
      const destruction =
        (before - weigh(mutual ? afterMaterial : afterMaterial[opponent])) / pv.moves.length;
      return destruction;
    } catch (e) {
      console.error(e, pv.moves);
    }
    return NaN;
  }

  function isAcpl() {
    return operators.acplMean && operators.acplStdev !== undefined;
  }

  function isLc0bias() {
    return operators.lc0bias !== undefined;
  }

  function applyAcpl() {
    const targetCpl = makeTargetCpl();
    for (const mv of scored) {
      // currently we use a sigmoid with .06 sensitivity and offset 80, seemsgood
      const distance = Math.abs((mv.cpl ?? 0) - targetCpl);
      const offset = 80;
      const sensitivity = 0.06;
      mv.weights.acpl = distance === 0 ? 1 : 1 / (1 + Math.E ** (sensitivity * (distance - offset)));
    }
  }

  function from(m: Operator) {
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

  function scoreOf(pv: Line, depth = pv.scores.length - 1) {
    const sc = pv.scores[clamp(depth, { min: 0, max: pv.scores.length - 1 })];
    return isNaN(sc) ? 1000 : clamp(sc, { min: -1000, max: 1000 });
  }

  function outcomeExpectancy(cp: number) {
    return 2 / (1 + 10 ** (-cp / 400)) - 1; // [-1, 1]
  }

  function weigh(material: co.Material | co.MaterialSide) {
    let score = 0;
    for (const [role, price] of Object.entries(prices) as [co.Role, number][]) {
      score += price * ('white' in material ? material.count(role) : material[role]);
    }
    return score;
  }

  /*

function applyShallow(r: SearchResult, depth = 0) {
  // negative contribution from deepest score
  r = structuredClone(r);
  // console.log(structuredClone(r).pvs.sort(sortShallow(depth)));
  return r.pvs.sort(sortShallow(depth))[0].moves[0];
}

function sortShallow(depth: number) {
  return (lhs: Line, rhs: Line) => {
    return 2 * scoreOf(rhs, depth) - scoreOf(rhs) - (2 * scoreOf(lhs, depth) - scoreOf(lhs));
  };
}
*/

  /*function byDestruction(lines: Line[], fen: string, mutual = false) {
  const chess = co.Chess.fromSetup(co.fen.parseFen(fen).unwrap()).unwrap();
  const beforeMaterial = co.Material.fromBoard(chess.board);
  const opponent = co.opposite(chess.turn);
  const before = weigh(mutual ? beforeMaterial : beforeMaterial[opponent]);
  const aggression: [number, Score][] = [];
  for (const history of lines) {
    for (const pv of history) {
      try {
        const pvChess = chess.clone();
        for (const move of pv.moves) pvChess.play(co.parseUci(move)!);
        const afterMaterial = co.Material.fromBoard(pvChess.board);
        const destruction =
          (before - weigh(mutual ? afterMaterial : afterMaterial[opponent])) / pv.moves.length;
        if (destruction > 0) aggression.push([destruction, pv]);
      } catch (e) {
        console.error(e, pv.moves);
      }
    }
  }
  return aggression;
}*/
}
