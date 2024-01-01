import { type Zerofish, type Score } from 'zerofish';
import * as Chops from 'chessops';
import { Libot, BotInfo, ZfBotConfig } from './interfaces';

let ordinal = 0;

export class ZfBot implements Libot {
  readonly name: string;
  readonly uid: string;
  readonly description: string;
  readonly image: string;
  readonly ctx: ZfBotConfig;
  readonly netName?: string;
  ratings = new Map();
  ordinal: number;
  zf: Zerofish;

  get imageUrl() {
    return lichess.asset.url(`lifat/bots/images/${this.image}`, { noVersion: true });
  }

  constructor(info: BotInfo, zf: Zerofish) {
    const infoCfg = info.zfcfg;
    Object.assign(this, info);
    this.ctx = infoCfg ? Object.assign({}, defaultCfg, infoCfg) : defaultCfg;
    this.zf = zf;
    this.ordinal = ordinal++;
  }

  async move(fen: string) {
    const ctx = this.ctx;
    const chess = Chops.Chess.fromSetup(Chops.fen.parseFen(fen).unwrap()).unwrap();
    const p = { ply: chess.halfmoves, material: Chops.Material.fromBoard(chess.board) };
    const zeroMove = this.netName ? this.zf.goZero(fen) : Promise.resolve(undefined);
    if (chance(ctx.zeroChance(p))) return (await zeroMove) ?? '0000';
    const fishMove = this.zf.goFish(fen, {
      depth: this.ctx.searchDepth?.(p) ?? 12,
      pvs: this.ctx.searchWidth(p),
    });
    let before = performance.now();
    const [zero, fish] = await Promise.all([zeroMove, fishMove]);
    //const cp = score(fish[0]);
    console.log('zf', performance.now() - before);
    before = performance.now();
    const biasCp = biasScore(fish, { move: zero, bias: ctx.zeroCpDefault(p), depth: ctx.scoreDepth?.(p) });
    console.log('cp', score(fish[0]), 'biasCp', biasCp, performance.now() - before);
    before = performance.now();
    const threshold = ctx.cpThreshold(p);
    const filtered = filter(fish, biasCp, threshold);
    if (!chance(ctx.aggression(p))) {
      const mv = filtered[Math.floor(Math.random() * filtered.length)]?.moves[0] ?? zero;
      console.log('returning ', mv, filtered);
    }
    const aggression = byDestruction(fish, fen);
    aggression.sort((a, b) => b[0] - a[0]);
    const zeroIndex = aggression.findIndex(([_, pv]) => pv.moves[0] === zero);
    const zeroDestruction = zeroIndex >= 0 ? aggression[zeroIndex]?.[0] : -0.1;
    console.log(zeroDestruction, aggression, performance.now() - before);
    return aggression[0]?.[1]?.moves[0] ?? zero;
  }
}

const defaultCfg: ZfBotConfig = {
  // if no opening book moves are selected, these parameters govern how a zerobot chooses a move
  zeroChance: constant(0), // [0 computed, 1 always lc0]
  zeroCpDefault: constant(-1),
  // first, if chance(zeroChance) then the lc0 move is chosen and we are done
  cpThreshold: constant(50), // a limiter for the number of centipawns we can lose vs fish[0][max]
  searchDepth: constant(8), // how deep to go
  scoreDepth: constant(99), // prefer scores at this depth
  searchWidth: constant(16), // multiPV
  aggression: constant(0.1), // [0 passive, 1 aggressive]
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
  const chess = Chops.Chess.fromSetup(Chops.fen.parseFen(fen).unwrap()).unwrap();
  const beforeMaterial = Chops.Material.fromBoard(chess.board);
  const opponent = Chops.opposite(chess.turn);
  const before = weigh(mutual ? beforeMaterial : beforeMaterial[opponent]);
  const aggression: [number, Score][] = [];
  for (const history of lines) {
    for (const pv of history) {
      try {
        const pvChess = chess.clone();
        for (const move of pv.moves) pvChess.play(Chops.parseUci(move)!);
        const afterMaterial = Chops.Material.fromBoard(pvChess.board);
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

const prices: { [role in Chops.Role]?: number } = {
  pawn: 1,
  knight: 2.8,
  bishop: 3,
  rook: 5,
  queen: 9,
};

function weigh(material: Chops.Material | Chops.MaterialSide) {
  let score = 0;
  for (const [role, price] of Object.entries(prices) as [Chops.Role, number][]) {
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
