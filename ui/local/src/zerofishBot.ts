import type { Zerofish, Line, SearchResult, FishSearch, Position } from 'zerofish';
import { Libot, CardData, ZeroSearch, Mapping } from './types';
import { PolyglotBook } from 'bits/types';
import { clamp, deepFreeze } from 'common';
import { BotCtrl, botAssetUrl } from './botCtrl';
import { interpolate, normalize } from './mapping';
import * as co from 'chessops';

export type ZerofishBots = { [id: string]: ZerofishBot };

export interface ZerofishBotEditor extends ZerofishBot {
  [key: string]: any;
  disabled: Set<string>;
}

export class ZerofishBot implements Libot {
  name: string;
  readonly uid: string;
  readonly card: CardData;
  description: string;
  image: string;
  book?: string;
  zero?: ZeroSearch;
  fish?: FishSearch;
  glicko: { r: number; rd: number };
  selectors?: { [type: string]: Mapping };
  private ctrl: BotCtrl;
  private openings: Promise<PolyglotBook | undefined>;

  constructor(info: Libot, ctrl: BotCtrl) {
    Object.assign(this, info);
    Object.defineProperty(this, 'ctrl', { value: ctrl });
    Object.defineProperty(this, 'openings', { get: () => ctrl.assetDb.getBook(this.book) });
    Object.defineProperty(this, 'card', {
      get: () => ({
        label: this.name,
        domId: this.uid.startsWith('#') ? this.uid.slice(1) : this.uid,
        imageUrl: this.imageUrl,
      }),
    });
    Object.values(this.selectors ?? {}).forEach(normalize);
  }

  get imageUrl() {
    return this.ctrl.assetDb.getImageUrl(this.image);
  }

  async move(pos: Position, chess: co.Chess) {
    const openings = (await this.openings)?.(chess);
    let chance = Math.random();
    for (const { uci, weight } of openings ?? []) {
      chance -= weight;
      if (chance <= 0) return uci;
    }
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

  updateRating(opp: { r: number; rd: number } = { r: 1500, rd: 350 }, score: number) {
    const q = Math.log(10) / 400;
    this.glicko ??= { r: 1500, rd: 350 };
    const expected = 1 / (1 + 10 ** ((opp.r - this.glicko.r) / 400));
    const g = 1 / Math.sqrt(1 + (3 * q ** 2 * opp.rd ** 2) / Math.PI ** 2);
    const dSquared = 1 / (q ** 2 * g ** 2 * expected * (1 - expected));
    const deltaR = (q * g * (score - expected)) / (1 / dSquared + 1 / this.glicko.rd ** 2);
    this.glicko = {
      r: Math.round(this.glicko.r + deltaR),
      rd: Math.max(30, Math.sqrt(1 / (1 / this.glicko.rd ** 2 + 1 / dSquared))),
    };
  }

  get ratingText() {
    return `${this.glicko?.r ?? 1500}${(this.glicko?.rd ?? 350) > 80 ? '?' : ''}`;
  }

  chooseMove(
    chess: co.Chess,
    zeroResult: SearchResult | undefined,
    fishResult: SearchResult | undefined,
  ): Uci {
    const [f, z] = [fishResult as SearchResult, zeroResult as SearchResult];
    let head = z?.bestmove ?? f?.bestmove ?? '0000';
    //if (fishResult) return applyShallow(f, 0);
    if (f && z && f.pvs.length > 1 && z.pvs.length > 1) {
      let i = 0;
      for (const pv of z.pvs) {
        const move = pv.moves[0];
        if (f.pvs.some(pv => pv.moves[0] === move)) i++;
      }
      console.log(i, 'of', z.pvs.length, 'are scored by fish');
    }
    /*if (!f && z) {
      const pvs = z.pvs.filter(pv => pv.moves.length > 0);
      const pv = pvs[Math.floor(Math.random() * pvs.length)];
      return pv.moves[pv.moves.length - 1];
    }*/
    if (this.selectors?.acpl) head = applyAcpl(f, interpolateValue(this.selectors.acpl, f, chess) ?? 0);
    if (this.selectors?.lc0) {
      const val = interpolateValue(this.selectors.lc0, f, chess);
      if (val && z?.bestmove && Math.random() < val) head = z.bestmove;
    }
    return head;
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
  console.log(structuredClone(r).pvs.sort(sortShallow(depth)));
  return r.pvs.sort(sortShallow(depth))[0].moves[0];
}

function applyAcpl(r: SearchResult, acpl: number) {
  r = structuredClone(r);
  const headScore = score(r.pvs[0]);
  const headMove = r.pvs[0].moves[0];
  const targetCp = clamp(normalRandom(acpl, clamp(acpl / 4, { min: 5, max: 50 })), { min: 0, max: 250 });
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

function normalRandom(mean: number, sd: number) {
  return mean + sd * Math.sqrt(-2.0 * Math.log(Math.random())) * Math.sin(2.0 * Math.PI * Math.random());
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
