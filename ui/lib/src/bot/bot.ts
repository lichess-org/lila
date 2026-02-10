import * as co from 'chessops';
import { zip } from '../algo';
import { clockToSpeed } from '@/game';
import {
  type FilterFacetValue,
  type FilterSpec,
  type FilterName,
  type Filters,
  quantizeFilter,
  evaluateFilter,
  filterFacetKeys,
  combine,
} from './filter';
import type { SearchResult } from '@lichess-org/zerofish';
import type { OpeningBook } from '../game/polyglot';
import { movetime as getMovetime } from './movetime';
import type { BotLoader } from './botLoader';
import type {
  BotInfo,
  FishSearch,
  ZeroSearch,
  Book,
  MoveSource,
  MoveArgs,
  MoveResult,
  SearchMove,
  LocalSpeed,
  SoundEvent,
  SoundEvents,
  Ratings,
} from './types';

export type * from './types';

export class Bot implements BotInfo, MoveSource {
  private static filterRegistryMap: Map<string, FilterSpec>;
  static rating(bot: BotInfo | undefined, speed: LocalSpeed): number {
    return bot?.ratings?.[speed] ?? bot?.ratings?.classical ?? 1500;
  }
  static isValid(maybeBot: any): boolean {
    return Boolean(maybeBot?.zero || maybeBot?.fish);
  }
  static registerFilter(name: string, spec: FilterSpec): void {
    Bot.filterRegistry.set(name, spec); // TODO move this
  }
  static registeredFilters(): [string, FilterSpec][] {
    return [...Bot.filterRegistry.entries()]; // TODO move this
  }
  private static get filterRegistry(): Map<string, FilterSpec> {
    Bot.filterRegistryMap ??= new Map(); // TODO move this
    return Bot.filterRegistryMap;
  }

  readonly uid: string;
  readonly version: number = 0;

  name: string;
  description: string;
  vision: string;
  image: string;
  ratings: Ratings;
  books?: Book[];
  sounds?: SoundEvents;
  filters?: Filters;
  zero?: ZeroSearch;
  fish?: FishSearch;

  private openings: Promise<OpeningBook[]>;
  private stats: { cplMoves: number; cpl: number };
  private traces: string[];
  private cp: number;
  private ctrl: BotLoader;

  constructor(info: BotInfo, ctrl: BotLoader) {
    Object.assign(this, structuredClone(info));
    if (this.filters) Object.values(this.filters).forEach(quantizeFilter);

    // keep these from being stored or cloned with the bot
    Object.defineProperties(this, {
      cp: { value: 0, writable: true },
      stats: { value: { cplMoves: 0, cpl: 0 } },
      ctrl: { value: ctrl },
      traces: { value: [], writable: true },
      openings: {
        get: () => Promise.all(this.books?.flatMap(b => ctrl.getBook(b.key)) ?? []),
      },
    });
  }

  get traceMove(): string {
    return this.traces.join('\n');
  }

  get statsText(): string {
    return this.stats.cplMoves ? `acpl ${Math.round(this.stats.cpl / this.stats.cplMoves)}` : '';
  }

  async move(args: MoveArgs): Promise<MoveResult> {
    const { pos, chess } = args;
    const { fish, zero } = this;
    this.trace([`  ${args.ply}. '${this.name}' at '${co.fen.makeFen(chess.toSetup())}'`]);
    if (args.avoid?.length) this.trace(`[move] - avoid = [${args.avoid.join(', ')}]`);
    const openingMove = await this.bookMove(args);
    args.movetime = getMovetime(args, Bot.rating(this, clockToSpeed(args.initial, args.increment)));

    if (openingMove) return { uci: openingMove, movetime: args.movetime };

    const zeroSearch = zero
      ? {
          nodes: zero.nodes,
          multipv: Math.max(zero.multipv, args.avoid.length + 1), // avoid threefold
          net: { key: this.name + '-' + zero.net, fetch: () => this.ctrl.getNet(zero.net) },
        }
      : undefined;
    if (zeroSearch) this.trace(`[move] - zero: ${stringify(zeroSearch)}`);

    const fishSearch = { multipv: fish?.multipv ?? 1, by: { depth: Math.max(10, fish?.depth ?? 10) } };
    if (fish) this.trace(`[move] - fish: ${stringify(fish)}`);

    const [fishResults, zeroResults] = await Promise.all([
      this.ctrl.zerofish.goFish(pos, fishSearch),
      zeroSearch && this.ctrl.zerofish.goZero(pos, zeroSearch),
    ]);
    this.cp = fishResults.lines[fishResults.lines.length - 1][0].score;

    const { uci, cpl, movetime } = this.chooseMove(fishResults, fish?.depth ?? 0, zeroResults, args);
    if (cpl !== undefined && cpl < 1000) {
      this.stats.cplMoves++; // debug stats
      this.stats.cpl += cpl;
    }
    this.trace(`[move] - chose ${uci} in ${movetime.toFixed(1)}s`);
    return { uci, movetime };
  }

  playSound(eventList: SoundEvent[]): number {
    const prioritized = soundPriority.filter(e => eventList.includes(e));
    for (const soundList of prioritized.map(priority => this.sounds?.[priority] ?? [])) {
      let r = Math.random();
      for (const { key, chance, delay, mix } of soundList) {
        r -= chance / 100;
        if (r > 0) continue;
        // right now we play at most one sound per move, might want to revisit this.
        // also definitely need cancelation of the timeout
        site.sound
          .load(key, this.ctrl.getSoundUrl(key))
          .then(() => setTimeout(() => site.sound.play(key, Math.min(1, mix * 2)), delay * 1000));
        return Math.min(1, (1 - mix) * 2);
      }
    }
    return 1;
  }

  private hasFilter(op: FilterName): boolean {
    const f = this.filters?.[op];
    return Boolean(f && (f.move?.length || f.score?.length || f.time?.length));
  }

  private facetWeight(op: FilterName, { chess, movetime }: MoveArgs): number | undefined {
    if (!this.hasFilter(op)) return undefined;
    const f = this.filters![op];
    const x: FilterFacetValue = Object.fromEntries(
      filterFacetKeys
        .filter(k => f[k])
        .map(k => {
          if (k === 'move') return [k, chess.fullmoves];
          else if (k === 'score') return [k, outcomeExpectancy(chess.turn, this.cp)];
          else if (k === 'time') return [k, Math.log2(movetime ?? 64)];
          else return [k, undefined];
        }),
    );

    const vals = evaluateFilter(f, x);
    const y = combine(vals, f.by);
    this.trace(`[filter] - ${op} ${stringify(x)} -> ${stringify(vals)} by ${f.by} yields ${y.toFixed(2)}`);
    return y;
  }

  private async bookMove({ chess, initial, increment }: MoveArgs) {
    const speed = clockToSpeed(initial, increment);
    const books = this.books?.filter(b => !b.color || b.color === chess.turn);
    // first use book relative weights to choose from the subset of books with moves for the current position
    if (!books?.length) return undefined;
    const moveList: { moves: { uci: Uci; weight: number }[]; book: Book }[] = [];
    let bookChance = 0;
    for (const [book, opening] of zip(books, await this.openings)) {
      const moves = await opening(chess, this.ratings[speed] ?? this.ratings.classical ?? 1500, speed);
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
    fishResults: SearchResult,
    fishDepth: number,
    zeroResults: SearchResult | undefined,
    args: MoveArgs,
  ): { uci: Uci; cpl?: number; movetime: number } {
    const moves = this.parseMoves(fishResults, fishDepth, zeroResults, args);
    const movetime = args.movetime ?? 0;

    this.trace(`[chooseMove] - parsed = ${stringify(moves)}`);

    this.scoreByFilters(moves, args);

    moves.sort(weightSort);

    if (args.pos.moves?.length) {
      const last = args.pos.moves[args.pos.moves.length - 1].slice(2, 4);
      // if the current favorite is a capture of the opponent's last moved piece,
      // always take it, regardless of move quality decay
      if (moves[0].uci.slice(2, 4) === last) {
        this.trace(`[chooseMove] - short-circuit = ${stringify(moves[0])}`);
        return { ...moves[0], movetime: movetime / 2 };
      }
    }
    const filtered = moves.filter(mv => !args.avoid.includes(mv.uci));
    this.trace(`[chooseMove] - sorted & filtered = ${stringify(filtered)}`);
    const decayed =
      this.scoreByMoveQualityDecay(filtered, this.facetWeight('moveDecay', args) ?? 0) ??
      filtered[0] ??
      moves[0];
    return { ...decayed, movetime };
  }

  private parseMoves(
    fish: SearchResult,
    fishDepth: number,
    zero: SearchResult | undefined,
    args: MoveArgs,
  ): SearchMove[] {
    if (fishDepth) this.trace(`[parseMoves] - ${stringify(fish)}`);
    if (zero) this.trace(`[parseMoves] - ${stringify(zero)}`);
    if (fish.bestmove === '0000' && (!zero || zero.bestmove === '0000')) {
      this.trace('    parseMoves: no moves found!');
      this.cp = 0;
      return [{ uci: '0000', weights: {} }];
    }
    const parsed: SearchMove[] = [];
    const lc0bias = this.facetWeight('lc0bias', args) ?? 0;
    const cp = fishDepth ? fish.lines[fishDepth - 1][0].score : this.cp;
    this.trace(`[parseMoves] - cp = ${cp.toFixed(2)}, lc0bias = ${lc0bias.toFixed(2)}`);
    if (fishDepth)
      fish.lines[fishDepth - 1]
        .filter(line => line.moves[0])
        .forEach(line =>
          parsed.push({
            uci: line.moves[0],
            cpl: Math.abs(cp - line.score),
            weights: { lc0bias: 0 },
          }),
        );

    if (zero)
      zero.lines[0]
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
    const mean = this.facetWeight('cplTarget', args)!;
    const stdev = this.facetWeight('cplStdev', args) ?? 80;
    const cplTarget = Math.abs(mean + stdev * getNormal());
    // folding the normal at zero skews the distribution mean a bit further from the target
    const gain = 0.06;
    const threshold = 80; // something like clamp(stdev, { min: 50, max: 100 }) here?
    for (const mv of sorted) {
      if (mv.cpl === undefined) continue;
      const distance = Math.abs((mv.cpl ?? 0) - cplTarget);
      // cram cpl into [0, 1] with sigmoid
      mv.weights.cplBias = distance === 0 ? 1 : 1 / (1 + Math.E ** (gain * (distance - threshold)));
    }
  }

  private scoreByFilters(sorted: SearchMove[], args: MoveArgs) {
    if (this.hasFilter('cplTarget')) {
      this.scoreByCpl(sorted, args);
      this.trace(`[chooseMove] - cpl scored = ${stringify(sorted)}`);
    }
    const customFilterKeys = Object.keys(this.filters ?? {}).filter(
      key => this.hasFilter(key) && !['cplTarget', 'cplStdev', 'lc0bias', 'moveDecay'].includes(key),
    );
    for (const key of customFilterKeys) {
      const { info, score } = Bot.filterRegistry.get(key) ?? {};
      if (!info || !score) {
        throw new Error(`undefined filter: ${key}, registry: ${stringify(Bot.filterRegistry)}`);
      }
      const filterResult = score(sorted, args, this.facetWeight(key, args)!);
      for (const [uci, result] of Object.entries(filterResult)) {
        sorted.find(mv => mv.uci === uci)!.weights[key] = result.weight; // no need for map, n^2 is fine
      }
      this.trace(`[scoreByFilters] - ${info.label ?? key} scored = ${stringify(sorted)}`);
    }
  }

  private scoreByMoveQualityDecay(sorted: SearchMove[], decay: number) {
    // in this weighted random selection, each move's weight is given by a quality decay parameter
    // raised to the power of the move's index as sorted by previous filters. a random number between
    // 0 and the sum of all weights will identify the final move

    let variate = sorted.reduce((sum, mv, i) => (sum += mv.P = decay ** i), 0) * Math.random();
    return sorted.find(mv => (variate -= mv.P!) <= 0);
  }

  private trace(msg: string | string[]) {
    if (Array.isArray(msg)) this.traces = msg;
    else this.traces.push('      ' + msg);
  }
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

const soundPriority: SoundEvent[] = [
  'playerWin',
  'botWin',
  'playerCheck',
  'botCheck',
  'playerCapture',
  'botCapture',
  'playerMove',
  'botMove',
  'greeting',
];
