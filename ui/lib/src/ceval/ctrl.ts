// no side effects allowed due to re-export by index.ts

import type { Rules } from 'chessops';
import { lichessRules } from 'chessops/compat';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';

import { clamp } from '@/algo';
import { throttleWithFlush } from '@/async';
import { pubsub } from '@/pubsub';
import { storedIntProp, storedStringProp, storage } from '@/storage';
import type { LocalEval, TreePath } from '@/tree/types';

import { prop, type Prop, type Toggle, toggle } from '../index';
import { Engines } from './engines/engines';
import {
  type CevalOpts,
  type CevalEngine,
  type EvalMeta,
  type Work,
  type Step,
  type Hovering,
  type PvBoard,
  type Search,
  type CustomSearch,
  type EngineInfo,
  CevalState,
} from './types';
import { sanIrreversible, showEngineError, fewerCores } from './util';
import { povChances } from './winningChances';

interface SearchInfo {
  search: Search;
  engine: EngineInfo;
  threads: number;
  hashSize: number;
}

interface Started {
  path: TreePath;
  steps: Step[];
  gameId?: string;
  threatMode: boolean;
}

export class CevalCtrl {
  rules: Rules;
  analysable: boolean;
  engines: Engines;
  storedEngine: Prop<string>;
  storedPv: Prop<number> = storedIntProp('ceval.multipv', 1);
  storedMovetime: Prop<number> = storedIntProp('ceval.search-ms', 8000); // may be 'Infinity'
  download?: { bytes: number; total: number };
  hovering: Prop<Hovering | null> = prop<Hovering | null>(null);
  pvBoard: Prop<PvBoard | null> = prop<PvBoard | null>(null);
  isDeeper: Toggle = toggle(false);
  curEval: LocalEval | null = null;
  lastStarted?: Started;
  showEnginePrefs: Toggle = toggle(false);

  private worker: CevalEngine | undefined;

  constructor(public opts: CevalOpts) {
    this.engines = new Engines(this);
    this.storedEngine = storedStringProp('ceval.engine', this.engines.defaultId);
    this.init();

    // another tab has started ceval, we should stop:
    storage.make('ceval.fen').listen(() => {
      if (this.isBackground) return;
      this.worker?.destroy();
      this.worker = undefined; // release memory
      this.opts.redraw();
    });

    document.addEventListener('visibilitychange', () => {
      if (this.engines.external) return;
      if (this.curEval?.bestmove) return;
      if (!this.lastStarted) return;
      if (!this.analysable) return;
      if (this.isBackground) return;

      if (document.hidden) this.worker?.stop();
      else if (this.curEval) this.doStart(this.lastStarted);
    });
  }

  init(opts?: CevalOpts): void {
    if (opts) this.opts = opts;
    this.reset();
    this.rules = lichessRules(this.opts.variant.key);
    this.analysable =
      !this.opts.initialFen ||
      parseFen(this.opts.initialFen).chain(setup => setupPosition(this.rules, setup)).isOk;
    this.engines.setActive(this.opts.custom?.engine?.id ?? this.storedEngine());
    if (this.worker?.getInfo().id !== this.engines.active().id) this.unload();
  }

  available(): boolean {
    return (this.isBackground || !document.hidden) && this.analysable;
  }

  goDeeper = (): void => {
    if (!this.lastStarted) return;
    this.isDeeper(true);
    this.doStart(this.lastStarted);
  };

  reset = (): void => {
    this.worker?.stop();
    this.curEval = null;
    this.lastStarted = undefined;
    this.download = undefined;
  };

  start = (path: string, steps: Step[], gameId: string | undefined, threatMode = false): boolean => {
    if (!this.available() || this.wasUnloaded) return false;
    this.isDeeper(false);
    this.doStart({ path, steps, gameId, threatMode });
    return true;
  };

  setThreads = (threads: number): void => storage.set('ceval.threads', threads.toString());

  info(custom?: CustomSearch): SearchInfo {
    const maybeSearch = custom?.search?.();
    const maxTime = Number(maybeSearch) || this.engines.active().maxMovetime;
    return {
      threads: clamp(
        custom?.engine?.threads ?? (Number(storage.get('ceval.threads')) || this.recommendedThreads),
        { min: this.engines.active().minThreads, max: this.maxThreads },
      ),
      hashSize: clamp(custom?.engine?.hashSize ?? Number(storage.get('ceval.hash-size')), {
        min: 16,
        max: this.engines.active().maxHash,
      }),
      engine: (custom?.engine && this.engines.getEngine({ id: custom.engine.id })) || this.engines.active(),
      search:
        typeof maybeSearch === 'object'
          ? maybeSearch
          : {
              multiPv: this.storedPv(),
              by: { movetime: clamp(this.isDeeper() ? Infinity : this.storedMovetime(), { max: maxTime }) },
            },
    };
  }

  get search(): Search {
    return this.info(this.opts.custom).search;
  }

  get recommendedThreads(): number {
    return (
      this.engines.external?.maxThreads ??
      clamp(navigator.hardwareConcurrency - (navigator.hardwareConcurrency % 2 ? 0 : 1), {
        min: this.engines.active().minThreads ?? 1,
        max: this.maxThreads,
      })
    );
  }

  get maxThreads(): number {
    return (
      this.engines.external?.maxThreads ??
      (fewerCores()
        ? Math.min(this.engines.active().maxThreads ?? 32, navigator.hardwareConcurrency)
        : (this.engines.active().maxThreads ?? 32))
    );
  }

  get isInfinite(): boolean {
    return (
      this.storedMovetime() === Number.POSITIVE_INFINITY &&
      !Number.isFinite(this.engines.active().maxMovetime)
    );
  }

  get state(): CevalState {
    return this.worker?.getState() ?? CevalState.Initial;
  }

  get canGoDeeper(): boolean {
    return this.state !== CevalState.Computing && (this.curEval?.depth ?? 0) < 99;
  }

  get isComputing(): boolean {
    return this.state === CevalState.Computing;
  }

  get isCacheable(): boolean {
    return !!this.engines.active().capabilities?.includes('cloudEval');
  }

  get isBackground(): boolean {
    return this.opts.custom?.canBackground === true;
  }

  get wasUnloaded(): boolean {
    return !this.worker && !!this.lastStarted; // another tab started ceval
  }

  get showingCloud(): boolean {
    if (!this.lastStarted) return false;
    const curr = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    return !!curr.ceval?.cloud;
  }

  setHashSize = (hash: number): void => storage.set('ceval.hash-size', hash.toString());

  selectEngine = (id: string): void => {
    this.storedEngine(id);
    this.engines.setActive(id);
    this.opts.onSelectEngine?.();
  };

  setPvBoard = (pvBoard: PvBoard | null): void => {
    this.pvBoard(pvBoard);
    this.opts.redraw();
  };

  engineFailed(msg: string): void {
    if (msg.includes('Blocking on the main thread')) return; // mostly harmless
    showEngineError(this.engines.active().name, msg);
    this.reset();
    this.unload();
  }

  private readonly doStart = (s: Started) => {
    this.lastStarted = s;
    const step = s.steps[s.steps.length - 1];
    const { search, threads, hashSize } = this.info(this.opts.custom);
    const lastEvalMillis = (s.threatMode ? step.threat : step.ceval)?.millis ?? 0;
    if (!this.isDeeper() && 'movetime' in search.by && lastEvalMillis >= search.by.movetime) {
      return;
    }
    const work: Work = {
      variant: this.opts.variant.key,
      threads,
      hashSize,
      gameId: s.gameId,
      stopRequested: false,
      initialFen: s.steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path: s.path,
      ply: step.ply,
      search: search.by,
      multiPv: search.multiPv,
      threatMode: s.threatMode,
      emit: this.makeThrottledEmitter(),
    };
    if (s.threatMode) {
      const c = step.ply % 2 === 1 ? 'w' : 'b';
      const fen = step.fen.replace(/ (w|b) /, ' ' + c + ' ');
      work.currentFen = fen;
      work.initialFen = fen;
    } else {
      // send fen after latest castling move and the following moves
      for (let i = 1; i < s.steps.length; i++) {
        const step = s.steps[i];
        if (sanIrreversible(this.opts.variant.key, step.san!)) {
          work.moves = [];
          work.initialFen = step.fen;
        } else work.moves.push(step.uci!);
      }
    }

    if (this.worker?.getInfo().id !== this.engines.active().id) this.unload();
    this.worker ??= this.engines.makeEngine({ id: this.engines.active().id, variant: this.opts.variant.key });
    this.worker.start(work);
  };

  private unload(): void {
    this.worker?.stop();
    this.worker?.destroy();
    this.worker = undefined;
  }

  private makeThrottledEmitter() {
    // 'working' properties are bound for closure
    const working = {
      started: this.lastStarted!,
      fen: undefined as string | undefined,
      emit: this.opts.emit,
      background: this.isBackground,
      movetime: 'movetime' in this.search.by && this.search.by.movetime,
      dontStop: Boolean(this.engines.external || this.opts.custom || this.isDeeper() || this.isInfinite),
    };
    const emitter = throttleWithFlush(125, (ev: LocalEval, meta: EvalMeta) => {
      if (working.fen && working.fen !== ev.fen) return emitter.clear();

      this.curEval = ev;

      if (!working.fen && !working.background) {
        working.fen = this.curEval.fen;
        storage.fire('ceval.fen', this.curEval.fen); // will pause other tabs
      }
      const color = meta.ply % 2 === (meta.threatMode ? 1 : 0) ? 'white' : 'black';
      this.curEval.pvs.sort((a, b) => povChances(color, b) - povChances(color, a));

      if (this.lastStarted && !working.dontStop) {
        const evNode = working.started.steps[working.started.steps.length - 1];
        if (working.movetime && evNode.ceval?.cloud && ev.millis > 500) {
          const targetNodes = evNode.ceval.nodes;
          const likelyNodes = Math.round((working.movetime * ev.nodes) / ev.millis);

          if (likelyNodes < targetNodes) this.worker?.stop();
        }
      }
      working.emit(this.curEval, meta);
    });
    return (ev: LocalEval, meta: EvalMeta) => {
      pubsub.emit('analysis.eval', structuredClone(ev), meta);
      if (working.started !== this.lastStarted) emitter.clear();
      else if (ev.bestmove) emitter.flush(ev, meta);
      else emitter(ev, meta);
    };
  }
}
