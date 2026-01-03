/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { Engines } from './engines/engines';
import {
  type CevalOpts,
  type CevalEngine,
  type Work,
  type Step,
  type Hovering,
  type PvBoard,
  type Started,
  type Search,
  type CustomSearch,
  type EngineInfo,
  CevalState,
} from './types';
import { sanIrreversible, showEngineError, fewerCores } from './util';
import { setupPosition } from 'chessops/variant';
import { parseFen } from 'chessops/fen';
import { lichessRules } from 'chessops/compat';
import { prop, type Prop, type Toggle, toggle } from '../common';
import { clamp } from '../algo';
import { storedIntProp, storedStringProp, storage } from '../storage';
import type { Rules } from 'chessops';
import { throttleWithFlush } from '../async';
import { povChances } from './winningChances';

export type SearchInfo = {
  search: Search;
  engine: EngineInfo;
  threads: number;
  hashSize: number;
};

export class CevalCtrl {
  opts: CevalOpts;
  rules: Rules;
  analysable: boolean;
  engines: Engines;
  storedEngine: Prop<string>;
  storedPv: Prop<number> = storedIntProp('ceval.multipv', 1);
  storedMovetime: Prop<number> = storedIntProp('ceval.search-ms', 8000); // may be 'Infinity'
  hovering: Prop<Hovering | null> = prop<Hovering | null>(null);
  pvBoard: Prop<PvBoard | null> = prop<PvBoard | null>(null);
  download?: { bytes: number; total: number };
  isDeeper: Toggle = toggle(false);
  curEval: Tree.LocalEval | null = null;
  lastStarted: Started | false = false;
  showEnginePrefs: Toggle = toggle(false);

  private lastEmitFen: string | null = null;
  private worker: CevalEngine | undefined;
  private emitter: ReturnType<typeof throttleWithFlush> | undefined;

  constructor(opts: CevalOpts) {
    this.init(opts);
    this.engines = new Engines(this);
    this.storedEngine = storedStringProp('ceval.engine', this.engines.defaultId);

    storage.make('ceval.disable').listen(() => {
      this.stop();
      this.worker?.destroy();
      this.worker = undefined; // release memory
      this.opts.redraw();
    });
  }

  setOpts(opts: Partial<CevalOpts>): void {
    this.init({ ...this.opts, ...opts });
  }

  init(opts: CevalOpts): void {
    this.abort();
    this.opts = opts;
    this.rules = lichessRules(this.opts.variant.key);
    this.analysable =
      !this.opts.initialFen ||
      parseFen(this.opts.initialFen).chain(setup => setupPosition(this.rules, setup)).isOk;
    this.engines?.current(opts.custom?.engine?.id ?? this.storedEngine());
    if (this.worker?.getInfo().id !== this.engines?.current()?.id) {
      this.worker?.destroy();
      this.worker = undefined; // release memory
      this.lastStarted = false;
    }
  }

  resume(work?: Work): void {
    this.worker ??= this.engines?.makeEngine({
      id: this.engines?.current()?.id,
      variant: this.opts.variant.key,
    });
    if (work) this.worker.start(work);
  }

  available(): boolean {
    return (!document.hidden || !!this.opts.custom?.canBackground) && this.analysable;
  }

  goDeeper = (): void => {
    if (!this.lastStarted) return;
    const { path, steps, gameId, threatMode } = this.lastStarted;
    this.isDeeper(true);
    this.doStart(path, steps, gameId, threatMode);
  };

  stop = (): void => {
    this.worker?.stop();
    this.download = undefined;
  };

  start = (path: string, steps: Step[], gameId: string | undefined, threatMode?: boolean): boolean => {
    if (!this.available() || this.isPaused) return false;
    this.isDeeper(false);
    this.doStart(path, steps, gameId, !!threatMode);
    return true;
  };

  abort = (): void => {
    this.emitter = undefined;
    this.stop();
  };

  setThreads = (threads: number): void => storage.set('ceval.threads', threads.toString());

  // call this to preflight your eventual search parameters
  searchInfoOf(custom?: CustomSearch): SearchInfo {
    const search = custom?.search?.();
    return {
      threads: clamp(
        custom?.engine?.threads ?? (Number(storage.get('ceval.threads')) || this.recommendedThreads),
        { min: this.engines.current()?.minThreads ?? 1, max: this.maxThreads },
      ),
      hashSize: Math.min(
        this.maxHash,
        custom?.engine?.hashSize ?? (Number(storage.get('ceval.hash-size')) || 16),
      ),
      engine:
        this.engines.get({ id: custom?.engine?.id, variant: this.opts.variant.key }) ??
        this.engines.current()!,
      search:
        search && typeof search !== 'number' // number puts a cap on movetime
          ? search
          : {
              multiPv: typeof search === 'number' ? 1 : this.storedPv(),
              by:
                !search && (this.isDeeper() || this.isInfinite)
                  ? { depth: 99 }
                  : {
                      movetime: Math.min(
                        this.storedMovetime(),
                        search ?? 30 * 1000,
                        this.engines.maxMovetime(),
                      ),
                    },
            },
    };
  }

  get search(): Search {
    return this.searchInfoOf(this.opts.custom).search;
  }

  get threads(): number {
    return this.searchInfoOf().threads;
  }

  get recommendedThreads(): number {
    return (
      this.engines.external()?.maxThreads ??
      clamp(navigator.hardwareConcurrency - (navigator.hardwareConcurrency % 2 ? 0 : 1), {
        min: this.engines.current()?.minThreads ?? 1,
        max: this.maxThreads,
      })
    );
  }

  get maxThreads(): number {
    return (
      this.engines.external()?.maxThreads ??
      (fewerCores()
        ? Math.min(this.engines.current()?.maxThreads ?? 32, navigator.hardwareConcurrency)
        : (this.engines.current()?.maxThreads ?? 32))
    );
  }

  get safeMovetime(): number {
    return Math.min(this.storedMovetime(), this.engines.maxMovetime());
  }

  get isInfinite(): boolean {
    return this.safeMovetime === Number.POSITIVE_INFINITY;
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
    return !!this.engines.current()?.trustedFor?.includes('cloudEval');
  }

  get isPaused(): boolean {
    return !this.worker && !!this.lastStarted; // another tab started ceval
  }

  get showingCloud(): boolean {
    if (!this.lastStarted) return false;
    const curr = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    return !!curr.ceval?.cloud;
  }

  setHashSize = (hash: number): void => storage.set('ceval.hash-size', hash.toString());

  get hashSize(): number {
    return this.searchInfoOf().hashSize;
  }

  get maxHash(): number {
    return this.engines.current()?.maxHash ?? 16;
  }

  selectEngine = (id: string): void => {
    this.storedEngine(id);
    this.engines.current(id);
    this.opts.onSelectEngine?.();
  };

  setPvBoard = (pvBoard: PvBoard | null): void => {
    this.pvBoard(pvBoard);
    this.opts.redraw();
  };

  engineFailed(msg: string): void {
    if (msg.includes('Blocking on the main thread')) return; // mostly harmless
    showEngineError(this.engines.current()?.name ?? 'Engine', msg);
    this.worker?.destroy();
    this.worker = undefined;
    this.lastStarted = false;
  }

  private doStart = (path: Tree.Path, steps: Step[], gameId: string | undefined, threatMode: boolean) => {
    const step = steps[steps.length - 1];
    if (
      !this.isDeeper() &&
      'movetime' in this.search.by &&
      ((threatMode ? step.threat : step.ceval)?.millis ?? 0) >= this.search.by.movetime
    ) {
      this.lastStarted = { path, steps, gameId, threatMode };
      return;
    }
    const clientEmit = this.opts.emit; // this closure ensures we dont mix up emitters (xhrReload)
    const workEmitter = (this.emitter = throttleWithFlush(100, (ev: Tree.LocalEval, work: Work) =>
      this.triggerEmit(clientEmit, ev, work),
    ));
    const work: Work = {
      variant: this.opts.variant.key,
      threads: this.threads,
      hashSize: this.hashSize,
      gameId,
      stopRequested: false,
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path,
      ply: step.ply,
      search: this.search.by,
      multiPv: this.search.multiPv,
      threatMode,
      emit: (ev: Tree.LocalEval) => {
        if (workEmitter === this.emitter)
          return ev.bestmove !== undefined ? workEmitter.flush(ev, work) : workEmitter(ev, work);
        // if bestmove, flush don't throttle
      },
    };
    if (threatMode) {
      const c = step.ply % 2 === 1 ? 'w' : 'b';
      const fen = step.fen.replace(/ (w|b) /, ' ' + c + ' ');
      work.currentFen = fen;
      work.initialFen = fen;
    } else {
      // send fen after latest castling move and the following moves
      for (let i = 1; i < steps.length; i++) {
        const s = steps[i];
        if (sanIrreversible(this.opts.variant.key, s.san!)) {
          work.moves = [];
          work.initialFen = s.fen;
        } else work.moves.push(s.uci!);
      }
    }

    // Notify all other tabs to disable ceval.
    storage.fire('ceval.disable');

    this.resume(work);

    this.lastStarted = {
      path,
      steps,
      gameId,
      threatMode,
    };
  };

  private triggerEmit(clientEmit: (ev: Tree.LocalEval, work: Work) => void, ev: Tree.LocalEval, work: Work) {
    const color = work.ply % 2 === (work.threatMode ? 1 : 0) ? 'white' : 'black';
    ev.pvs.sort((a, b) => povChances(color, b) - povChances(color, a));
    this.curEval = ev;
    clientEmit(ev, work);
    if (ev.fen !== this.lastEmitFen) {
      // amnesty while auto disable not processed
      this.lastEmitFen = ev.fen;
      storage.fire('ceval.fen', ev.fen);
    }
    if (!this.lastStarted || this.isDeeper() || this.isInfinite || work.threatMode) return;
    const showingNode = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    const byMovetime = 'movetime' in this.search.by && this.search.by.movetime;
    if (byMovetime && showingNode.ceval?.cloud && ev.millis > 500 && !this.engines.external()) {
      const targetNodes = showingNode.ceval.nodes;
      const likelyNodes = Math.round((byMovetime * ev.nodes) / ev.millis);

      if (likelyNodes < targetNodes) this.stop();
    }
  }
}
