import * as co from 'chessops';
import { zip, clamp, defined } from 'common';
import { normalize, interpolate } from './operator';
import { type BotCtrl } from './botCtrl';
import { outcomeExpectancy, getNormal, deepScore } from './util';
//import { zerofishMove, zerofishThink } from './zerofishMove';
import type { FishSearch, SearchResult, Zerofish } from 'zerofish';
import type { OpeningBook } from 'bits/polyglot';
import type { Assets } from './assets';
import type {
  BotInfo,
  ZeroSearch,
  Operators,
  Book,
  Mover,
  MoveArgs,
  MoveResult,
  SoundEvents,
  Ratings,
} from './types';

export type Bots = { [id: string]: Bot };

export class Bot implements BotInfo, Mover {
  private zerofish: Zerofish;
  private assets: Assets;
  private openings: Promise<OpeningBook[]>;
  private stats: { cplMoves: number; cpl: number };
  readonly uid: string;
  readonly version: number = 0;
  name: string;
  description: string;
  image: string;
  ratings: Ratings;
  books?: Book[];
  sounds?: SoundEvents;
  operators?: Operators;
  zero?: ZeroSearch;
  fish?: FishSearch;

  constructor(info: BotInfo, botCtrl: BotCtrl) {
    Object.assign(this, structuredClone(info));
    if (this.operators) Object.values(this.operators).forEach(normalize);

    // use Object.defineProperties to keep them from being stored or cloned with the bot
    Object.defineProperties(this, {
      zerofish: { value: botCtrl.zerofish },
      assets: { value: botCtrl.assets },
      ratings: { get: () => ({ ...info.ratings, ...botCtrl.ratings[this.uid] }) },
      stats: { value: { cplMoves: 0, cpl: 0 } },
      openings: {
        get: () => Promise.all(this.books?.flatMap(b => this.assets.getBook(b.key)) ?? []),
      },
    });
  }

  get statsText(): string {
    return this.stats.cplMoves ? `acpl ${Math.round(this.stats.cpl / this.stats.cplMoves)}` : '';
  }

  get needsScore(): boolean {
    return Object.values(this.operators ?? {}).some(o => o.from === 'score');
  }

  async move(args: MoveArgs): Promise<MoveResult> {
    const moveArgs = { ...args, thinktime: this.thinktime(args) };
    const { pos, chess } = moveArgs;
    const opening = await this.bookMove(chess);
    if (opening) return { uci: opening, thinktime: moveArgs.thinktime };
    const { uci, cpl } = this.chooseMove(
      await Promise.all([
        this.zero &&
          this.zerofish.goZero(pos, {
            ...this.zero,
            net: {
              key: this.name + '-' + this.zero.net,
              fetch: async () => (await this.assets.getNet(this.zero?.net))!,
            },
          }),
        this.fish && this.zerofish.goFish(pos, this.fish),
      ]),
      moveArgs,
    );
    if (cpl !== undefined && cpl < 1000) {
      this.stats.cplMoves++; // debug stats
      this.stats.cpl += cpl;
    }
    return { uci, thinktime: moveArgs.thinktime };
  }

  private operator(op: string, { chess, score, thinktime }: MoveArgs): undefined | number {
    const o = this.operators?.[op];
    if (!o) return undefined;
    const val = interpolate(
      o,
      o.from === 'move'
        ? chess.fullmoves
        : o.from === 'score'
        ? outcomeExpectancy(chess.turn, score ?? 0)
        : thinktime
        ? Math.log2(thinktime)
        : 8,
    );
    console.log(thinktime, thinktime ? Math.log2(thinktime) : undefined, val);
    return val;
  }

  private thinktime(args: MoveArgs): number | undefined {
    if (args.remaining === undefined || args.initial === undefined) return undefined;

    const quickest = Math.min(args.initial / 160, 3);
    const perMoveTarget = args.initial / 45 + (args.increment ?? 0);
    return quickest + 2 * Math.random() * (perMoveTarget - quickest);
  }

  private async bookMove(chess: co.Chess) {
    if (!this.books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; bookWeight: number }[] = [];
    let bookChance = 0;
    for (const [book, opening] of zip(this.books, await this.openings)) {
      const moves = opening(chess);
      if (moves.length === 0) continue;
      moveList.push({ moves, bookWeight: book.weight ?? 1 });
      bookChance += book.weight ?? 1;
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

  private chooseMove([fish, zero]: (SearchResult | undefined)[], args: MoveArgs): { uci: Uci; cpl?: number } {
    const bot = this;

    if ((!fish || fish.bestmove === '0000') && (!zero || zero.bestmove === '0000'))
      return { uci: '0000', cpl: 0 };

    const sorted: SearchMove[] = [];
    const byUci: { [uci: string]: SearchMove } = {};
    const pvScore = fish?.lines[0]?.scores[0] ?? args.score ?? 0;

    if (fish)
      for (const v of fish.lines.filter(v => v.moves[0])) {
        const move = { uci: v.moves[0], cpl: Math.abs(pvScore - deepScore(v)), weights: {} };
        byUci[v.moves[0]] = move;
        sorted.push(move);
      }
    const lc0bias = this.operator('lc0bias', args) ?? 0;
    if (zero)
      for (const uci of zero.lines.map(v => v.moves[0]).filter(v => v)) {
        const move = (byUci[uci] ??= { uci, weights: {} });
        byUci[uci].weights.lc0 = lc0bias;
        if (!sorted.includes(move)) sorted.push(move);
      }

    scoreByCpl();
    sorted.sort(weightSort);

    if (args.pos.moves?.length) {
      const last = args.pos.moves[args.pos.moves.length - 1].slice(2, 4);
      // if the current favorite is a capture of the opponent's last moved piece, just take it
      if (sorted[0].uci.slice(2, 4) === last) return sorted[0];
    }
    return lineDecay(this.operator('lineDecay', args) ?? 0) ?? sorted[0];

    function scoreByCpl() {
      if (!bot.operators?.cplTarget) return;
      const mean = bot.operator('cplTarget', args) ?? 0;
      const stdev = bot.operator('cplStdev', args) ?? 80;
      const cplTarget = Math.abs(mean + stdev * getNormal());
      // folding (vs truncating) the normal at zero will skew the distribution mean further from the
      // target, but is slightly more interesting. hey it's "cplTarget" not "cplMean".
      for (const mv of sorted) {
        if (mv.cpl === undefined) continue;
        const distance = Math.abs((mv.cpl ?? 0) - cplTarget);
        const offset = 80;
        const sensitivity = 0.06;
        mv.weights.acpl = distance === 0 ? 1 : 1 / (1 + Math.E ** (sensitivity * (distance - offset)));
      }
    }

    function lineDecay(decay: number) {
      // in this weighted random selection, each move's probability is defined by a quality decay parameter
      // raised to the power of the move's index as sorted by previous operators. then a random number is
      // chosen between 0 and the probability sum to identify the final move

      let variate = sorted.reduce((sum, mv, i) => (sum += mv.P = decay ** i), 0) * Math.random();

      console.log(decay, variate);
      return sorted.find(mv => (variate -= mv.P!) <= 0);
    }

    function weightSort(a: SearchMove, b: SearchMove) {
      const wScore = (mv: SearchMove) => Object.values(mv.weights).reduce((acc, w) => acc + (w ?? 0), 0);
      return wScore(b) - wScore(a);
    }
  }
}

type Weights = 'lc0' | 'acpl';

interface SearchMove {
  uci: Uci;
  score?: number;
  cpl?: number;
  weights: { [key in Weights]?: number };
  P?: number;
}
