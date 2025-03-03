import * as co from 'chessops';
import { zip, clamp } from 'common/algo';
import { clockToSpeed } from 'game';
import { quantizeFilter, filterParameter, filterFacets, combine, type FilterValue } from './filter';
import type { FishSearch, SearchResult, Line } from 'zerofish';
import type { OpeningBook } from 'bits/polyglot';
import { env } from './localEnv';
import { movetime } from './movetime';
import type {
  BotInfo,
  ZeroSearch,
  Filters,
  FilterType,
  Book,
  MoveSource,
  MoveArgs,
  MoveResult,
  LocalSpeed,
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

  static viable(info: BotInfo): boolean {
    return Boolean(info.uid && info.name && (info.zero || info.fish)); // TODO moar validate
  }

  static isSame(a: BotInfo | undefined, b: BotInfo | undefined): boolean {
    if (!closeEnough(a, b, ['filters', 'version'])) return false;
    const [aFilters, bFilters] = [a, b].map(bot =>
      Object.entries(bot?.filters ?? {}).filter(([_, v]) => v.move || v.time || v.score),
    );
    return closeEnough(aFilters, bFilters); // allow empty filter craplets to differ
  }

  static migrate(oldDbVersion: number, oldBot: any): BotInfo {
    // ...
    return oldDbVersion, oldBot;
  }

  static rating(bot: BotInfo | undefined, speed: LocalSpeed): number {
    return bot?.ratings[speed] ?? bot?.ratings.classical ?? 1500;
  }

  constructor(info: BotInfo) {
    Object.assign(this, structuredClone(info));
    if (this.filters) Object.values(this.filters).forEach(quantizeFilter);

    // keep these from being stored or cloned with the bot
    Object.defineProperties(this, {
      stats: { value: { cplMoves: 0, cpl: 0 } },
      traces: { value: [], writable: true },
      openings: {
        get: () => Promise.all(this.books?.flatMap(b => env.assets.getBook(b.key)) ?? []),
      },
    });
  }

  get traceMove(): string {
    return this.traces.join('\n');
  }

  get statsText(): string {
    return this.stats.cplMoves ? `acpl ${Math.round(this.stats.cpl / this.stats.cplMoves)}` : '';
  }

  get needsScore(): boolean {
    return Object.values(this.filters ?? {}).some(o => o.score?.length);
  }

  async move(args: MoveArgs): Promise<MoveResult> {
    const { pos, chess } = args;
    const { fish, zero } = this;

    this.trace([`  ${env.game.live.ply}. '${this.name}' at '${co.fen.makeFen(chess.toSetup())}'`]);
    if (args.avoid?.length || args.cp)
      this.trace(
        `[move] - ${args.avoid?.length ? 'avoid = [' + args.avoid.join(', ') + '], ' : ''}` +
          (args.cp ? `cp = ${args.cp?.toFixed(2)}, ` : ''),
      );
    const openingMove = await this.bookMove(chess);
    args.movetime = movetime(args, Bot.rating(this, clockToSpeed(args.initial, args.increment)));
    // i need a better way to handle thinkTime, we probably need to adjust it in chooseMove
    if (openingMove) return { uci: openingMove, movetime: args.movetime };

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
    return { uci, movetime: thinkTime };
  }

  private hasFilter(op: FilterType): boolean {
    const f = this.filters?.[op];
    return Boolean(f && (f.move?.length || f.score?.length || f.time?.length));
  }

  private filter(op: FilterType, { chess, cp, movetime: thinkTime }: MoveArgs): number | undefined {
    if (!this.hasFilter(op)) return undefined;
    const f = this.filters![op];
    const x: FilterValue = Object.fromEntries(
      filterFacets
        .filter(k => f[k])
        .map(k => {
          if (k === 'move') return [k, chess.fullmoves];
          else if (k === 'score') return [k, outcomeExpectancy(chess.turn, cp ?? 0)];
          else if (k === 'time') return [k, Math.log2(thinkTime ?? 64)];
          else return [k, undefined];
        }),
    );

    const vals = filterParameter(f, x);
    const y = combine(vals, f.by);
    this.trace(`[filter] - ${op} ${stringify(x)} -> ${stringify(vals)} by ${f.by} yields ${y.toFixed(2)}`);
    return y;
  }

  private async bookMove(chess: co.Chess) {
    const books = this.books?.filter(b => !b.color || b.color === chess.turn);
    // first use book relative weights to choose from the subset of books with moves for the current position
    if (!books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; book: Book }[] = [];
    let bookChance = 0;
    for (const [book, opening] of zip(books, await this.openings)) {
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
          this.trace(`[bookMove] - chose ${uci} from ${book.color ? book.color + ' ' : ''}book '${key}'`);
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
    let thinkTime = args.movetime ?? 0;
    if (this.hasFilter('cplTarget')) {
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
    const stockfishVariate = lc0bias ? (this.hasFilter('cplTarget') ? 0 : Math.random()) : 0;
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
    const threshold = 80; // something like clamp(stdev, { min: 50, max: 100 }) here?
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

let nextNormal: number | undefined = undefined;

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
  return JSON.stringify(obj, (_, v) => (typeof v === 'number' ? v.toFixed(2) : v));
}

function closeEnough(a: any, b: any, ignore: string[] = []): boolean {
  if (a === b) return true;
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a)) {
    return Array.isArray(b) && a.length === b.length && a.every((x, i) => closeEnough(x, b[i], ignore));
  }
  if (typeof a !== 'object') return false;

  const [aKeys, bKeys] = [filteredKeys(a, ignore), filteredKeys(b, ignore)];
  if (aKeys.length !== bKeys.length) return false;

  for (const key of aKeys) {
    if (!bKeys.includes(key) || !closeEnough(a[key], b[key], ignore)) return false;
  }
  return true;
}

function isEmpty(prop: any): boolean {
  return Array.isArray(prop)
    ? false //prop.length === 0
    : typeof prop === 'object'
      ? Object.keys(prop).length === 0
      : false;
}

function filteredKeys(obj: any, ignore: string[] = []): string[] {
  if (typeof obj !== 'object') return obj;
  return Object.entries(obj)
    .filter(([k, v]) => !ignore.includes(k) && !isEmpty(v))
    .map(([k]) => k);
}
