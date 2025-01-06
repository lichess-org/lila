import * as co from 'chessops';
import { zip, clamp } from 'common/algo';
import { normalize, interpolate, domain } from './filter';
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
  private traces: string[];
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
      traces: { value: [], writable: true },
      openings: {
        get: () => Promise.all(this.books?.flatMap(b => env.assets.getBook(b.key)) ?? []),
      },
    });
  }

  static viable(info: BotInfo): boolean {
    return Boolean(info.uid && info.name && (info.zero || info.fish));
  }

  get traceMove(): string {
    return this.traces.join('\n');
  }

  get statsText(): string {
    return this.stats.cplMoves ? `acpl ${Math.round(this.stats.cpl / this.stats.cplMoves)}` : '';
  }

  get needsScore(): boolean {
    return Object.values(this.filters ?? {}).some(o => o.by === 'score');
  }

  async move(args: MoveArgs): Promise<MoveResult> {
    const { pos, chess } = args;
    const { fish, zero } = this;

    this.trace([`  ${env.game.live.ply}. '${this.name}' at '${co.fen.makeFen(chess.toSetup())}'`]);
    this.trace(
      `[move] - ${args.avoid?.length ? 'avoid = [' + args.avoid.join(', ') + '], ' : ''}` +
        (args.cp ? `cp = ${args.cp?.toFixed(2)}, ` : ''),
    );
    const opening = await this.bookMove(chess);
    args.thinkTime = this.thinkTime(args);

    // i need a better way to handle thinkTime, we probably need to adjust it in chooseMove
    if (opening) return { uci: opening, thinkTime: args.thinkTime };

    const zeroSearch = zero
      ? {
          nodes: zero.nodes,
          multipv: Math.max(zero.multipv, args.avoid.length + 1), // avoid threefold
          net: {
            key: this.name + '-' + zero.net,
            fetch: () => env.assets.getNet(zero.net),
          },
        }
      : undefined;
    if (fish) this.trace(`[move] - fish: ${stringify(fish)}`);
    if (zeroSearch) this.trace(`[move] - zero: ${stringify(zeroSearch)}`);
    const { uci, cpl, thinkTime } = this.chooseMove(
      await Promise.all([
        fish && env.bot.zerofish.goFish(pos, fish),
        zeroSearch && env.bot.zerofish.goZero(pos, zeroSearch),
      ]),
      args,
    );
    if (cpl !== undefined && cpl < 1000) {
      this.stats.cplMoves++; // debug stats
      this.stats.cpl += cpl;
    }
    this.trace(`[move] - chose ${uci} in ${thinkTime.toFixed(1)}s`);
    return { uci, thinkTime: thinkTime };
  }

  private filter(op: string, { chess, cp, thinkTime }: MoveArgs): undefined | number {
    const f = this.filters?.[op];
    if (!f) return undefined;
    const val = interpolate(
      f,
      f.by === 'move'
        ? chess.fullmoves
        : f.by === 'score'
          ? outcomeExpectancy(chess.turn, cp ?? 0)
          : thinkTime
            ? Math.log2(thinkTime)
            : domain(f).max,
    );
    this.trace(`[filter] - ${op} by ${f.by} = ${val.toFixed(2)}`);
    return val;
  }

  private thinkTime({ initial, remaining, increment }: MoveArgs): number {
    initial ??= Infinity;
    increment ??= 0;
    if (!remaining || !Number.isFinite(initial)) return 60;
    const pace = 45 * (remaining < initial / Math.log2(initial) && !increment ? 2 : 1);
    const quickest = Math.min(initial / 150, 1);
    const variateMax = Math.min(remaining, increment + initial / pace);
    const thinkTime = quickest + Math.random() * variateMax;
    this.trace(
      `[thinkTime] - remaining = ${remaining.toFixed(1)}s, thinktime = ${thinkTime.toFixed(1)}s, pace = ` +
        `${pace.toFixed(1)}, quickest = ${quickest.toFixed(1)}s, variateMax = ${variateMax.toFixed(1)}`,
    );
    return thinkTime;
  }

  private async bookMove(chess: co.Chess) {
    // first use book relative weights to choose from the subset of books with moves for
    // the current position.
    if (!this.books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; book: Book }[] = [];
    let bookChance = 0;
    for (const [book, opening] of zip(this.books, await this.openings)) {
      const moves = opening(chess);
      if (moves.length === 0) continue;
      moveList.push({ moves, book });
      bookChance += book.weight;
    }
    bookChance = Math.random() * bookChance;
    for (const { moves, book } of moveList) {
      bookChance -= book.weight;
      const key = book.key;
      if (bookChance <= 0) {
        // then choose the move from that book
        let chance = Math.random();
        for (const { uci, weight } of moves) {
          chance -= weight;
          if (chance > 0) continue;
          this.trace(`[bookMove] - chose ${uci} from book '${key}'`);
          return uci;
        }
      }
    }
    return undefined;
  }

  private chooseMove(
    results: (SearchResult | undefined)[],
    args: MoveArgs,
  ): { uci: Uci; cpl?: number; thinkTime: number } {
    const moves = this.parseMoves(results, args);
    this.trace(`[chooseMove] - parsed = ${stringify(moves)}`);
    let thinkTime = args.thinkTime ?? 0;
    if (this.filters?.cplTarget) {
      this.scoreByCpl(moves, args);
      this.trace(`[chooseMove] - cpl scored = ${stringify(moves)}`);
    }

    moves.sort(weightSort);
    if (args.pos.moves?.length) {
      const last = args.pos.moves[args.pos.moves.length - 1].slice(2, 4);
      // if the current favorite is a capture of the opponent's last moved piece,
      // always take it, regardless of move quality decay
      if (moves[0].uci.slice(2, 4) === last) {
        this.trace(`[chooseMove] - short-circuit = ${stringify(moves[0])}`);
        return { ...moves[0], thinkTime: thinkTime / 4 };
      }
    }
    const filtered = moves.filter(mv => !args.avoid.includes(mv.uci));
    this.trace(`[chooseMove] - sorted & filtered = ${stringify(filtered)}`);
    const decayed =
      moveQualityDecay(filtered, this.filter('moveDecay', args) ?? 0) ?? filtered[0] ?? moves[0];
    return { ...decayed, thinkTime };
  }

  private parseMoves([fish, zero]: (SearchResult | undefined)[], args: MoveArgs): SearchMove[] {
    if (fish) this.trace(`[parseMoves] - ${stringify(fish)}`);
    if (zero) this.trace(`[parseMoves] - ${stringify(zero)}`);
    if ((!fish || fish.bestmove === '0000') && (!zero || zero.bestmove === '0000')) {
      this.trace('    parseMoves: no moves found!');
      return [{ uci: '0000', weights: {} }];
    }
    const parsed: SearchMove[] = [];
    const cp = fish?.lines[0] ? score(fish.lines[0]) : (args.cp ?? 0);
    const lc0bias = this.filter('lc0bias', args) ?? 0;
    const stockfishVariate = lc0bias ? (this.filters?.cplTarget ? 0 : Math.random()) : 0;
    this.trace(
      `[parseMoves] - cp = ${cp.toFixed(2)}, lc0bias = ${lc0bias.toFixed(2)}, stockfishVariate = ${stockfishVariate.toFixed(2)}`,
    );
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

  private trace(msg: string | string[]) {
    if (Array.isArray(msg)) this.traces = msg;
    else this.traces.push('      ' + msg);
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

function stringify(obj: any) {
  if (!obj) return '';
  return JSON.stringify(obj, (k, v) => (typeof v === 'number' ? v.toFixed(2) : v));
}
