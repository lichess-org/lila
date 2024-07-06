import * as co from 'chessops';
import { interpolate } from './operator';
import { clamp } from 'common';
import type { SearchResult, Line } from 'zerofish';
import type { Operators, Operator } from './types';

export function zerofishMove(
  fish: SearchResult | undefined,
  zero: SearchResult | undefined,
  mappings: Operators,
  chess: co.Chess,
) {
  const zm = new ZerofishMove(fish, zero, mappings, chess);
  return zm.bestMove;
}

type SearchMove = {
  uci: Uci;
  score?: number;
  shallowScore?: number;
  cpl?: number;
  weights: {
    acpl?: number;
    lc0?: number;
  };
};

class ZerofishMove {
  scored: SearchMove[] = [];
  byMove: { [uci: string]: SearchMove } = {};
  zmoves: string[] = [];
  weights: number[] = [];
  score: number;
  static nextNormal: number | undefined;

  constructor(
    readonly fish: SearchResult | undefined,
    readonly zero: SearchResult | undefined,
    readonly mappings: Operators,
    readonly chess: co.Chess,
  ) {
    const lc0bias = zero ? (this.mappings.lc0Bias ? this.from(this.mappings.lc0Bias) : 0.5) : 0;

    fish?.pvs
      .filter(x => x.moves[0])
      .forEach(pv => {
        const cp = score(pv);
        this.score ??= cp;
        const move = {
          uci: pv.moves[0],
          score: cp,
          shallowScore: score(pv, 0),
          cpl: Math.abs(this.score - cp),
          weights: { lc0: 1 - lc0bias },
        };
        this.byMove[pv.moves[0]] = move;
        this.scored.push(move);
      });
    zero?.pvs
      .filter(x => x.moves[0])
      .forEach((pv, index) => {
        const uci = pv.moves[0];
        this.zmoves.push(uci);
        const move = (this.byMove[uci] ??= { uci, weights: {} });

        const lc0Weight = lc0bias * 2 ** -index;
        const spice = !this.mappings.acplMean ? Math.random() - 0.5 : 0;
        //const spice = isAcpl ? 1 : 0.5 + Math.random() / 2; // used to randomize multipv lc0 when no stockfish

        if (this.mappings.acplMean || !move.weights.lc0) move.weights.lc0 = lc0Weight + spice;
        move.cpl ??= this.scored[this.scored.length - 1]?.cpl ?? 200;
        if (!this.scored.includes(move)) this.scored.push(move);
      });
    this.score ??= 0;
    this.applyAcpl();
  }
  applyAcpl() {
    const targetCpl = this.makeTargetCpl();
    for (const mv of this.scored) {
      // sigmoid with .06 sensitivity and offset 80
      const distance = Math.abs((mv.cpl ?? 0) - targetCpl);
      const offset = 80;
      const sensitivity = 0.06;
      mv.weights.acpl = distance === 0 ? 1 : 1 / (1 + Math.E ** (sensitivity * (distance - offset)));
    }
  }
  from(m: Operator) {
    return interpolate(m, m.from === 'move' ? this.chess.fullmoves : outcomeExpectancy(this.score));
  }
  weightSort = (a: SearchMove, b: SearchMove) => {
    const wScore = (mv: SearchMove) => Object.values(mv.weights).reduce((acc, w) => acc + (w ?? 0), 0);
    return wScore(b) - wScore(a);
  };
  makeTargetCpl() {
    if (!this.mappings.acplMean) return 0;
    const mean = this.from(this.mappings.acplMean) ?? 0;
    const stdev = this.from(this.mappings.acplStdev) ?? 0;
    return Math.max(mean + stdev * this.makeNormal(), 0);
  }
  makeNormal() {
    if (ZerofishMove.nextNormal !== undefined) {
      const normal = ZerofishMove.nextNormal;
      ZerofishMove.nextNormal = undefined;
      return normal;
    }
    const r = Math.sqrt(-2.0 * Math.log(Math.random()));
    const theta = 2.0 * Math.PI * Math.random();
    ZerofishMove.nextNormal = r * Math.sin(theta);
    return r * Math.cos(theta);
  }
  get bestMove() {
    const sorted = this.scored.slice().sort(this.weightSort);
    const cpl = (this.fish?.pvs.length ?? 0) > 1 ? sorted[0].cpl! : NaN;
    console.log(cpl, sorted);
    return {
      move: sorted[0]?.uci,
      cpl,
    };
  }
}
function score(pv: Line, depth = pv.scores.length - 1) {
  const sc = pv.scores[clamp(depth, { min: 0, max: pv.scores.length - 1 })];
  return isNaN(sc) ? 1000 : clamp(sc, { min: -1000, max: 1000 });
}

function outcomeExpectancy(cp: number) {
  return 2 / (1 + 10 ** (-cp / 400)) - 1; // [-1, 1]
}
/*
function interpolateValue(m: Mapping, r: SearchResult, chess: co.Chess) {
  return !m
    ? undefined
    : m.from === 'move'
    ? interpolate(m, chess.fullmoves)
    : m.from === 'score' && r.pvs.length > 1
    ? interpolate(m, outcomeExpectancy(score(r.pvs[0])))
    : undefined;
}

function applyShallow(r: SearchResult, depth = 0) {
  // negative contribution from deepest score
  r = structuredClone(r);
  // console.log(structuredClone(r).pvs.sort(sortShallow(depth)));
  return r.pvs.sort(sortShallow(depth))[0].moves[0];
}

function applyAcpl(r: SearchResult, mean: number, stdev: number) {
  r = structuredClone(r);
  const headScore = score(r.pvs[0]);
  const headMove = r.pvs[0].moves[0];
  const targetCp = clamp(mean + stdev * normal(), { min: 0 });
  //console.log('target cp:', targetCp);
  return r.pvs.sort(sortCpl(headScore, targetCp)).filter(pv => pv.moves.length > 0)?.[0].moves[0] ?? headMove;
}
*/
//function applySearchMix
/*
function sortCpl(headScore: number, targetCp: number) {
  return (lhs: Line, rhs: Line) => {
    return Math.abs(headScore - score(lhs) - targetCp) - Math.abs(headScore - score(rhs) - targetCp);
  };
}

function sortShallow(depth: number) {
  return (lhs: Line, rhs: Line) => {
    return 2 * score(rhs, depth) - score(rhs) - (2 * score(lhs, depth) - score(lhs));
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

const prices: { [role in co.Role]?: number } = {
  pawn: 1,
  knight: 2.8,
  bishop: 3,
  rook: 5,
  queen: 9,
};

function weigh(material: co.Material | co.MaterialSide) {
  let score = 0;
  for (const [role, price] of Object.entries(prices) as [co.Role, number][]) {
    score += price * ('white' in material ? material.count(role) : material[role]);
  }
  return score;
}
