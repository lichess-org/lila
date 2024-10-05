import * as co from 'chessops';
import { zip, clamp } from 'common/algo';
import { normalize, interpolate } from './filter';
import type { FishSearch, SearchResult, Line } from 'zerofish';
import type { OpeningBook } from 'bits/polyglot';
import { env } from './localEnv';
import type {
  BotInfo,
  ZeroSearch,
  Filters,
  Book,
  MoveSource,
  MoveArgs,
  MoveResult,
  SoundEvents,
  Ratings,
} from './types';

export function score(pv: Line, depth: number = pv.scores.length - 1): number {
  const sc = pv.scores[clamp(depth, { min: 0, max: pv.scores.length - 1 })];
  return isNaN(sc) ? 0 : clamp(sc, { min: -10000, max: 10000 });
}

export class Bot implements BotInfo, MoveSource {
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
  filters?: Filters;
  zero?: ZeroSearch;
  fish?: FishSearch;

  constructor(info: BotInfo) {
    Object.assign(this, structuredClone(info));
    if (this.filters) Object.values(this.filters).forEach(normalize);

    // keep these from being stored or cloned with the bot
    Object.defineProperties(this, {
      stats: { value: { cplMoves: 0, cpl: 0 } },
      openings: {
        get: () => Promise.all(this.books?.flatMap(b => env.assets.getBook(b.key)) ?? []),
      },
    });
  }

  static viable(info: BotInfo): boolean {
    return Boolean(info.uid && info.name && (info.zero || info.fish));
  }

  get statsText(): string {
    return this.stats.cplMoves ? `acpl ${Math.round(this.stats.cpl / this.stats.cplMoves)}` : '';
  }

  get needsScore(): boolean {
    return Object.values(this.filters ?? {}).some(o => o.by === 'score');
  }

  async move(args: MoveArgs): Promise<MoveResult> {
    const moveArgs = { ...args, thinktime: this.thinktime(args) };
    const { pos, chess } = moveArgs;
    const { fish, zero } = this;
    const opening = await this.bookMove(chess);
    if (opening) return { uci: opening, thinktime: moveArgs.thinktime };
    const { uci, cpl } = this.chooseMove(
      await Promise.all([
        fish && env.bot.zerofish.goFish(pos, fish),
        zero &&
          env.bot.zerofish.goZero(pos, {
            ...zero,
            net: {
              key: this.name + '-' + zero.net,
              fetch: () => env.assets.getNet(zero.net),
            },
          }),
      ]),
      moveArgs,
    );
    if (cpl !== undefined && cpl < 1000) {
      this.stats.cplMoves++; // debug stats
      this.stats.cpl += cpl;
    }
    return { uci, thinktime: moveArgs.thinktime };
  }

  private filter(op: string, { chess, cp, thinktime }: MoveArgs): undefined | number {
    const o = this.filters?.[op];
    if (!o) return undefined;
    const val = interpolate(
      o,
      o.by === 'move'
        ? chess.fullmoves
        : o.by === 'score'
          ? outcomeExpectancy(chess.turn, cp ?? 0)
          : thinktime
            ? Math.log2(thinktime)
            : 240,
    );
    return val;
  }

  private thinktime({ initial, remaining, increment }: MoveArgs): number | undefined {
    initial ??= Infinity;
    increment ??= 0;
    if (!remaining || !Number.isFinite(initial)) return undefined;
    const pace = 45 * (remaining < initial / Math.log2(initial) && !increment ? 2 : 1);
    const quickest = Math.min(initial / 150, 1);
    const variateMax = Math.min(remaining, increment + initial / pace);
    return quickest + Math.random() * variateMax;
  }

  private async bookMove(chess: co.Chess) {
    // first decide on a book from those with a move for the current position using book
    // relative weights. then choose a move within that book using the move weights
    if (!this.books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; bookWeight: number }[] = [];
    let bookChance = 0;
    for (const [book, opening] of zip(this.books, await this.openings)) {
      const moves = opening(chess);
      if (moves.length === 0) continue;
      moveList.push({ moves, bookWeight: book.weight });
      bookChance += book.weight;
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

  private chooseMove(results: (SearchResult | undefined)[], args: MoveArgs): { uci: Uci; cpl?: number } {
    const moves = this.parseMoves(results, args);

    if (this.filters?.cplTarget) this.scoreByCpl(moves, args);

    moves.sort(weightSort);

    if (args.pos.moves?.length) {
      const last = args.pos.moves[args.pos.moves.length - 1].slice(2, 4);

      // our one global rule - if the current favorite is a capture of the opponent's last moved piece,
      // always take it, regardless of move quality decay
      if (moves[0].uci.slice(2, 4) === last) return moves[0];
    }
    return moveQualityDecay(moves, this.filter('moveDecay', args) ?? 0) ?? moves[0];
  }

  private parseMoves([fish, zero]: (SearchResult | undefined)[], args: MoveArgs): SearchMove[] {
    if ((!fish || fish.bestmove === '0000') && (!zero || zero.bestmove === '0000'))
      return [{ uci: '0000', weights: {} }];

    const parsed: SearchMove[] = [];
    const cp = fish?.lines[0] ? score(fish.lines[0]) : (args.cp ?? 0);
    const lc0bias = this.filter('lc0bias', args) ?? 0;
    const stockfishVariate = lc0bias ? (this.filters?.cplTarget ? 0 : Math.random()) : 0;

    fish?.lines
      .filter(line => line.moves[0])
      .forEach(line =>
        parsed.push({
          uci: line.moves[0],
          cpl: Math.abs(cp - score(line)),
          weights: { lc0bias: stockfishVariate },
        }),
      );

    zero?.lines
      .map(v => v.moves[0])
      .filter(Boolean)
      .forEach(uci => {
        const existing = parsed.find(move => move.uci === uci);
        if (existing) existing.weights.lc0bias = lc0bias;
        else parsed.push({ uci, weights: { lc0bias } });
      });
    return parsed;
  }

  private scoreByCpl(sorted: SearchMove[], args: MoveArgs) {
    if (!this.filters?.cplTarget) return;
    const mean = this.filter('cplTarget', args) ?? 0;
    const stdev = this.filter('cplStdev', args) ?? 80;
    const cplTarget = Math.abs(mean + stdev * getNormal());
    // folding the normal at zero skews the observed distribution mean a bit further from the target
    const gain = 0.06;
    const threshold = 80; // we could go with something like clamp(stdev, { min: 50, max: 100 }) here
    for (const mv of sorted) {
      if (mv.cpl === undefined) continue;
      const distance = Math.abs((mv.cpl ?? 0) - cplTarget);
      // cram cpl into [0, 1] with sigmoid
      mv.weights.acpl = distance === 0 ? 1 : 1 / (1 + Math.E ** (gain * (distance - threshold)));
    }
  }
}

type Weights = 'lc0bias' | 'acpl';

interface SearchMove {
  uci: Uci;
  score?: number;
  cpl?: number;
  weights: { [key in Weights]?: number };
  P?: number;
}

function moveQualityDecay(sorted: SearchMove[], decay: number) {
  // in this weighted random selection, each move's weight is given by a quality decay parameter
  // raised to the power of the move's index as sorted by previous filters. a random number between
  // 0 and the sum of all weights will identify the final move

  let variate = sorted.reduce((sum, mv, i) => (sum += mv.P = decay ** i), 0) * Math.random();
  return sorted.find(mv => (variate -= mv.P!) <= 0);
}

function weightSort(a: SearchMove, b: SearchMove) {
  const wScore = (mv: SearchMove) => Object.values(mv.weights).reduce((acc, w) => acc + (w ?? 0), 0);
  return wScore(b) - wScore(a);
}

function outcomeExpectancy(turn: Color, cp: number): number {
  return 1 / (1 + 10 ** ((turn === 'black' ? cp : -cp) / 400));
}

let nextNormal: number | undefined;

function getNormal(): number {
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
