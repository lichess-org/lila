import type { Zerofish, Line, SearchResult, FishSearch, Position } from 'zerofish';
import { Libot, ZerofishBotInfo, CardData, ZeroSearch, Mapping, Mappings } from './types';
import { PolyglotBook } from 'bits/types';
import { clamp, deepFreeze } from 'common';
import { BotCtrl, botAssetUrl, uidToDomId } from './botCtrl';
import { interpolate, normalize } from './mapping';
import * as co from 'chessops';

export type ZerofishBots = { [id: string]: ZerofishBot };

export class ZerofishBot implements Libot, ZerofishBotInfo {
  name: string;
  readonly uid: string;
  description: string;
  image: string;
  books: { name: string; weight?: number }[] = [];
  zero?: ZeroSearch;
  fish?: FishSearch;
  glicko?: { r: number; rd: number };
  selectors?: { [type: string]: Mapping };
  private ctrl: BotCtrl;
  private openings: Promise<PolyglotBook[]>;
  private stats: any;

  constructor(info: Libot, ctrl: BotCtrl) {
    Object.assign(this, info);
    Object.values(this.selectors ?? {}).forEach(normalize);

    // non enumerable properties are not stored or cloned
    Object.defineProperty(this, 'ctrl', { value: ctrl });
    Object.defineProperty(this, 'openings', {
      get: () => Promise.all(this.books ? [...this.books.map(b => ctrl.assetDb.getBook(b.name))] : []),
    });
    Object.defineProperty(this, 'stats', { value: { moves: 0, cpl: 0 } });
  }

  get imageUrl() {
    return this.ctrl.assetDb.getImageUrl(this.image);
  }

  async bookMove(chess: co.Chess) {
    if (!this.books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; bookWeight: number }[] = [];
    let bookChance = 0;
    const openings = await this.openings;
    for (let i = 0; i < this.books.length; i++) {
      const moves = openings[i](chess);
      if (moves.length === 0) continue;
      moveList.push({ moves, bookWeight: this.books[i].weight ?? 1 });
      bookChance += this.books[i].weight ?? 1;
    }
    bookChance = Math.random() * bookChance;
    for (const { moves, bookWeight } of moveList) {
      bookChance -= bookWeight;
      if (bookChance <= 0) {
        let chance = Math.random();
        for (const { uci, weight } of moves) {
          chance -= weight;
          if (chance <= 0) return uci;
        }
      }
    }
    return undefined;
  }

  async move(pos: Position, chess: co.Chess) {
    const opening = await this.bookMove(chess);
    if (opening) return opening;
    const [zeroResult, fishResult] = await Promise.all([
      this.zero &&
        this.ctrl.zf.goZero(pos, {
          ...this.zero,
          net: {
            name: this.name + '-' + this.zero.net,
            fetch: async () => (await this.ctrl.assetDb.getNet(this.zero!.net))!,
          },
        }),
      this.fish && this.ctrl.zf.goFish(pos, this.fish),
    ]);
    deepFreeze(zeroResult);
    deepFreeze(fishResult);
    return this.chooseMove(chess, zeroResult, fishResult);
  }

  get ratingText() {
    return `${this.glicko?.r ?? 1500}${(this.glicko?.rd ?? 3580) > 80 ? '?' : ''}`;
  }

  get fullRatingText() {
    return this.ratingText + ` (${Math.round(this.glicko?.rd ?? 350)})`;
  }

  get statsText() {
    return this.stats.moves ? `acpl ${Math.round(this.stats.cpl / this.stats.moves)}` : '';
  }

  chooseMove(
    chess: co.Chess,
    zeroResult: SearchResult | undefined,
    fishResult: SearchResult | undefined,
  ): Uci {
    return new SearchData(fishResult, zeroResult, this.selectors ?? {}, chess).bestMove;
    /*const [f, z] = [fishResult as SearchResult, zeroResult as SearchResult];
    let head = z?.bestmove ?? f?.bestmove ?? '0000';
    const cp = score(f?.pvs[0]);
    const lastHead = head;
    if (this.selectors?.acplMean) {
      const mean = interpolateValue(this.selectors.acplMean, f, chess) ?? 0;
      const stdev = interpolateValue(this.selectors.acplStdev, f, chess) ?? 0;
      head = applyAcpl(f, mean, stdev);
    }
    if (head !== lastHead) {
      const newCp = score(f?.pvs.find(pv => pv.moves[0] === head) ?? f?.pvs[0]);
      this.stats.moves++;
      this.stats.cpl += Math.abs(newCp - cp);
    }
    if (this.selectors?.lc0) {
      const val = interpolateValue(this.selectors.lc0, f, chess);
      if (val && z?.bestmove && Math.random() < val) head = z.bestmove;
    }
    return head;*/
  }
}

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

//function applySearchMix
function score(pv: Line, depth = pv.scores.length - 1) {
  return pv.scores[clamp(depth, { min: 0, max: pv.scores.length - 1 })];
}

function outcomeExpectancy(cp: number) {
  return 2 / (1 + 10 ** (-cp / 400)) - 1; // [-1, 1]
}

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

let nextNormal: number | undefined = undefined;

function normal() {
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

class SearchData {
  scored: SearchMove[] = [];
  byMove: { [uci: string]: SearchMove } = {};
  zmoves: string[] = [];
  weights: number[] = [];
  score: number;
  constructor(
    readonly fish: SearchResult | undefined,
    readonly zero: SearchResult | undefined,
    readonly mappings: Mappings,
    readonly chess: co.Chess,
  ) {
    fish?.pvs.forEach(pv => {
      const cp = score(pv);
      this.score ??= cp;
      const move = {
        uci: pv.moves[0],
        score: cp,
        shallowScore: score(pv, 0),
        cpl: Math.abs(this.score - cp),
        weights: {},
      };
      this.byMove[pv.moves[0]] = move;
      this.scored.push(move);
    });
    const lc0bias = zero
      ? this.mappings.lc0Bias
        ? interpolateValue(this.mappings.lc0Bias, zero, chess) ?? 0.5
        : 1
      : (0 as number);
    zero?.pvs.forEach(pv => {
      if (pv.moves.length === 0) return;
      const uci = pv.moves[0];
      if (!uci) return;
      const lc0Weight = (lc0bias * (zero.pvs.length - this.zmoves.length)) / zero.pvs.length;
      this.zmoves.push(uci);
      (this.byMove[uci] ??= { uci, weights: {} }).weights.lc0 = lc0Weight;
    });
    this.score ??= 0;
    this.applyAcpl();
  }
  applyAcpl() {
    if (!this.mappings.acplMean) return; // this.scored;
    const mean = this.interpolateValue(this.mappings.acplMean) ?? 0;
    const stdev = this.interpolateValue(this.mappings.acplStdev) ?? 0;

    const targetCpl = Math.max(mean + stdev * normal(), 0); // truncating distribution will skew mean
    for (const mv of this.scored) {
      // sigmoid with .06 sensitivity and offset 80
      const distance = Math.abs((mv.cpl ?? 0) - targetCpl);
      const offset = 80;
      const sensitivity = 0.06;
      mv.weights.acpl = distance === 0 ? 1 : 1 / (1 + Math.E ** (sensitivity * (distance - offset)));
    }
  }
  interpolateValue(m: Mapping) {
    return !m
      ? undefined
      : m.from === 'move'
      ? interpolate(m, this.chess.fullmoves)
      : m.from === 'score'
      ? interpolate(m, outcomeExpectancy(this.score))
      : undefined;
  }
  sortMoves = (a: SearchMove, b: SearchMove) => {
    const wScore = (mv: SearchMove) => Object.values(mv.weights).reduce((acc, w) => acc + (w ?? 0), 0);
    return wScore(b) - wScore(a);
  };
  get bestMove() {
    console.log(this.scored.sort(this.sortMoves));
    return this.scored.sort(this.sortMoves)[0].uci;
  }
}
