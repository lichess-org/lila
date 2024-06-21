import type { Zerofish, Line, SearchResult, Search, Position } from 'zerofish';
import { Libot, CardData, Point, Mapping } from './types';
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
  zero?: { net: string; search: Search };
  fish?: { multipv: number; search: Search };
  glicko: { r: number; rd: number };
  searchMix?: Mapping;
  acpl?: Mapping;
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
    if (this.searchMix) normalize(this.searchMix);
  }

  get imageUrl() {
    return this.ctrl.assetDb.getImageUrl(this.image);
  }

  async move(pos: Position, chess: co.Chess) {
    const openings = (await this.openings)?.(chess);
    let chance = Math.random();
    for (const { uci, weight } of openings ?? []) {
      chance -= weight;
      if (chance <= 0) console.log('opening', uci);
      if (chance <= 0) return uci;
    }
    const [zeroResult, fishResult] = await Promise.all([
      this.zero &&
        this.ctrl.zf.goZero(pos, {
          search: this.zero.search,
          net: {
            name: this.zero.net,
            fetch: async () => (await this.ctrl.assetDb.getNet(this.zero!.net))!,
          },
        }),
      this.fish && this.ctrl.zf.goFish(pos, this.fish),
    ]);
    deepFreeze(zeroResult);
    deepFreeze(fishResult);
    console.log(this.uid, chess.turn, 'zero =', zeroResult, 'fish =', fishResult);
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
    if (this.acpl) head = applyAcpl(f, interpolateValue(this.acpl, f, chess) ?? 0);
    if (this.searchMix) {
      const val = interpolateValue(this.searchMix, f, chess);
      if (val && z?.bestmove && Math.random() < val) head = z.bestmove;
    }
    return head;
  }
}

function interpolateValue(m: Mapping, r: SearchResult, chess: co.Chess) {
  return !m
    ? undefined
    : m.from === 'moves'
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
