/*import type { Zerofish, ZeroSearch, FishSearch, Position } from 'zerofish';
import * as co from 'chessops';
import { Libot, ZfBotConfig, CardData } from './types';
import { Database } from './database';
export class ZerofishBot2 implements Libot {
  readonly name: string;
  readonly uid: string;
  readonly description: string;
  readonly image: string;
  readonly zero?: { net: string; depth?: number };
  readonly fish?: { search?: FishSearch };
  readonly card?: CardData;
  elo?: number;

  constructor(
    info: Libot,
    readonly db: Database,
    readonly zf: Zerofish,
  ) {
    Object.assign(this, info);
  }

  updateElo(elo: number) {
    return (this.elo = elo);
  }

  async move(pos: Position) {
    const promises = [];
    if (this.zero) {
      promises.push(
        this.zf.goZero(pos, {
          depth: this.zero.depth,
          net: { name: this.zero.net, fetch: this.db.getNet },
        }),
      );
    }
    if (this.fish) {
      promises.push(await this.zf.goFish(pos, this.fish.search));
    }
    const movers = await Promise.all(promises);
    if (movers.length === 0) return '0000';
    else if (movers.length === 1) return movers[0].bestmove;
    else {
      const [zeroResult, fishResult] = movers;
      return this.chooseMove(pos.fen!, zeroResult, fishResult);
    }
    const fishMove = zf.goFish(fen, {
      depth: this.ctx.searchDepth?.(p) ?? 12,
      pvs: this.ctx.searchWidth(p),
    });
    const [zero, fish] = await Promise.all([zeroMove, fishMove]);
    console.log('zf', zero, scores(fish, zero));
    const biasCp = biasScore(fish, { move: zero, bias: ctx.zeroCpDefault(p), depth: ctx.scoreDepth?.(p) });
    console.log('cp', score(fish[0]), 'biasCp', biasCp);
    const threshold = ctx.cpThreshold(p);
    const filtered = filter(fish, biasCp, threshold);
    if (!chance(ctx.aggression(p))) {
      const mv = filtered[Math.floor(Math.random() * filtered.length)]?.moves[0] ?? zero;
      console.log('returning ', mv, 'from', filtered);
    }
    const aggression = byDestruction(fish, fen);
    aggression.sort((a, b) => b[0] - a[0]);
    const zeroIndex = aggression.findIndex(([_, pv]) => pv.moves[0] === zero);
    const zeroDestruction = zeroIndex >= 0 ? aggression[zeroIndex]?.[0] : -0.1;
    console.log(zeroDestruction, aggression);
    return aggression[0]?.[1]?.moves[0] ?? zero;
  }

  chooseMove(fen: string, zeroResult: any, fishResult: any) {
    return zeroResult.bestmove;
    const zero = zeroResult.bestmove;
    const fish = fishResult.pvs;
    const biasCp = biasScore(fish, { move: zero, bias: 0, depth: 1 });
    const threshold = 50;
    const filtered = filter(fish, biasCp, threshold);
    if (!chance(1)) {
      const mv = filtered[Math.floor(Math.random() * filtered.length)]?.moves[0] ?? zero;
      console.log('returning ', mv, 'from', filtered);
      return mv;
    }
    const aggression = byDestruction(fish, fen);
    aggression.sort((a, b) => b[0] - a[0]);
    const zeroIndex = aggression.findIndex(([_, pv]) => pv.moves[0] === zero);
    const zeroDestruction = zeroIndex >= 0 ? aggression[zeroIndex]?.[0] : -0.1;
    console.log(zeroDestruction, aggression);
    return aggression[0]?.[1]?.moves[0] ?? zero;
  }
}

const defaultCfg: ZfBotConfig = {
  // if no opening book moves are selected, these parameters govern how a zerobot chooses a move
  zeroChance: constant(1), // [0 computed, 1 always lc0]
  zeroCpDefault: constant(-20),
  // first, if chance(zeroChance) then the lc0 move is chosen and we are done
  cpThreshold: constant(50), // a limiter for the number of centipawns we can lose vs fish[0][max]
  searchDepth: constant(8), // how deep to go
  scoreDepth: constant(1), // prefer scores at this depth
  searchWidth: constant(16), // multipv
  aggression: constant(1), // [0 passive, 1 aggressive]
};


function constant(x: number) {
  return () => x;
}

function filter(pvs: Score[][], bias: number, threshold: number): Score[] {
  const matches: Score[] = [];
  for (const history of pvs) {
    for (const line of history) {
      if (Math.abs(line.score - bias) < threshold) matches.push({ ...line });
    }
  }
  return matches;
}

function biasScore(pvs: Score[][], evalCriteria?: { move?: string; bias?: number; depth?: number }) {
  //const best = score(pvs[0]);
  //console.log(pvs);
  if (pvs.length === 0 || !evalCriteria?.move) return evalCriteria?.bias ?? 0;
  let fit = { score: evalCriteria?.bias ?? 0, depth: -100 };
  const targetDepth = evalCriteria?.depth ?? 99;
  for (const history of pvs) {
    for (const line of history) {
      if (line.moves[0] === evalCriteria.move) {
        //console.log(Math.abs(targetDepth - line.depth), Math.abs(targetDepth - fit.depth), fit.score);
        if (Math.abs(targetDepth - line.depth) < Math.abs(targetDepth - fit.depth)) fit = line;
      }
    }
  }
  return fit.score;
}

function byDestruction(lines: Score[][], fen: string, mutual = false) {
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
}

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

function chance(chance: number) {
  return Math.random() < chance;
}

function scores(lines: Score[][], move?: string) {
  const matches: Score[] = [];
  if (!move) return matches;
  for (const history of lines) {
    for (const line of history) {
      if (line.moves[0] === move) matches.push(line);
    }
  }
  return matches;
}

function deepScores(lines: Score[][], move?: string) {
  const matches: Score[] = [];
  if (!move) return matches;
  let deepest = 0;
  for (const history of lines) {
    for (const line of history) {
      if (line.moves[0] !== move || line.depth < deepest) continue;
      if (line.depth > deepest) matches.length = 0;
      matches.push(line);
      deepest = line.depth;
    }
  }
  return matches;
}

function score(line: Score[]) {
  return line[line.length - 1]?.score ?? 0;
}

/*function deepScore(lines: Score[][], move?: string) {
  return Math.min(...(deepScores(lines, move).map(line => line.score) ?? []));
}

function shallowScores(lines: Score[][], move?: string) {
  const matches: Score[] = [];
  if (!move) return matches;
  let shallowest = 99;
  for (const history of lines) {
    for (const line of history) {
      if (line.moves[0] !== move || line.depth > shallowest) continue;
      if (line.depth > shallowest) matches.length = 0;
      matches.push(line);
      shallowest = line.depth;
    }
  }
  return matches;
}*/
