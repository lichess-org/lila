import { throttle } from '../async';
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
  CevalState,
} from './types';
import { sanIrreversible, showEngineError, fewerCores } from './util';
import { defaultPosition, setupPosition } from 'chessops/variant';
import { parseFen } from 'chessops/fen';
import { lichessRules } from 'chessops/compat';
import { povChances } from './winningChances';
import { prop, Prop, Toggle, toggle } from '../common';
import { clamp } from '../algo';
import { Result } from '@badrap/result';
import { storedIntProp, storage } from '../storage';
import type { Rules } from 'chessops';

export default class CevalCtrl {
  opts: CevalOpts;
  rules: Rules;
  analysable: boolean;
  engines: Engines;
  storedPv: Prop<number> = storedIntProp('ceval.multipv', 1);
  storedMovetime: Prop<number> = storedIntProp('ceval.search-ms', 8000); // may be 'Infinity'
  download?: { bytes: number; total: number };
  hovering: Prop<Hovering | null> = prop<Hovering | null>(null);
  pvBoard: Prop<PvBoard | null> = prop<PvBoard | null>(null);
  isDeeper: Toggle = toggle(false);
  curEval: Tree.LocalEval | null = null;
  lastStarted: Started | false = false;
  showEnginePrefs: Toggle = toggle(false);
  customSearch?: Search;

  private worker: CevalEngine | undefined;

  constructor(opts: CevalOpts) {
    this.init(opts);
    this.engines = new Engines(this);

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
    this.opts = opts;
    this.rules = lichessRules(this.opts.variant.key);
    const pos = this.opts.initialFen
      ? parseFen(this.opts.initialFen).chain(setup => setupPosition(this.rules, setup))
      : Result.ok(defaultPosition(this.rules));
    this.analysable = pos.isOk;
    this.customSearch = opts.search;
    if (this.worker?.getInfo().id !== this.engines?.activate()?.id) {
      this.worker?.destroy();
      this.worker = undefined;
    }
  }

  onEmit: (ev: Tree.LocalEval, work: Work) => void = throttle(200, (ev: Tree.LocalEval, work: Work) => {
    this.sortPvsInPlace(ev.pvs, work.ply % 2 === (work.threatMode ? 1 : 0) ? 'white' : 'black');
    this.curEval = ev;
    this.opts.emit(ev, work);
    if (ev.fen !== this.lastEmitFen) {
      // amnesty while auto disable not processed
      this.lastEmitFen = ev.fen;
      storage.fire('ceval.fen', ev.fen);
    }
    if (!this.lastStarted || this.isDeeper() || this.isInfinite || work.threatMode) return;
    const showingNode = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    const byMovetime = 'movetime' in this.search.by && this.search.by.movetime;
    if (byMovetime && showingNode.ceval?.cloud && ev.millis > 500 && !this.engines.external) {
      const targetNodes = showingNode.ceval.nodes;
      const likelyNodes = Math.round((byMovetime * ev.nodes) / ev.millis);

      if (likelyNodes < targetNodes) this.stop();
    }
  });

  available(): boolean {
    return !document.hidden && this.analysable;
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
      emit: (ev: Tree.LocalEval) => this.onEmit(ev, work),
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

    if (!this.worker) this.worker = this.engines.make({ variant: this.opts.variant.key });
    this.worker.start(work);

    this.lastStarted = {
      path,
      steps,
      gameId,
      threatMode,
    };
  };

  goDeeper = (): void => {
    if (!this.lastStarted) return;
    this.isDeeper(true);
    this.doStart(
      this.lastStarted.path,
      this.lastStarted.steps,
      this.lastStarted.gameId,
      this.lastStarted.threatMode,
    );
  };

  stop = (): void => {
    this.worker?.stop();
    this.download = undefined;
  };

  start = (path: string, steps: Step[], gameId: string | undefined, threatMode?: boolean): void => {
    if (!this.available()) return;
    this.isDeeper(false);
    this.doStart(path, steps, gameId, !!threatMode);
  };

  get state(): CevalState {
    return this.worker?.getState() ?? CevalState.Initial;
  }

  get search(): Search {
    const s = {
      multiPv: this.storedPv(),
      by: { movetime: Math.min(this.storedMovetime(), this.engines.maxMovetime) },
      ...this.customSearch,
    };
    if (this.isDeeper() || (this.isInfinite && !this.customSearch)) s.by = { depth: 99 };
    return s;
  }

  get safeMovetime(): number {
    return Math.min(this.storedMovetime(), this.engines.maxMovetime);
  }

  get isInfinite(): boolean {
    return this.safeMovetime === Number.POSITIVE_INFINITY;
  }

  get canGoDeeper(): boolean {
    return this.state !== CevalState.Computing && (this.curEval?.depth ?? 0) < 99;
  }

  get isComputing(): boolean {
    return this.state === CevalState.Computing;
  }

  get isCacheable(): boolean {
    return !!this.engines.active?.cloudEval;
  }

  get isPaused(): boolean {
    return !this.worker && !!this.lastStarted; // another tab started ceval
  }

  get showingCloud(): boolean {
    if (!this.lastStarted) return false;
    const curr = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    return !!curr.ceval?.cloud;
  }

  setThreads = (threads: number): void => storage.set('ceval.threads', threads.toString());

  get threads(): number {
    const stored = storage.get('ceval.threads');
    const desired = stored ? parseInt(stored) : this.recommendedThreads;
    return clamp(desired, { min: this.engines.active?.minThreads ?? 1, max: this.maxThreads });
  }

  get recommendedThreads(): number {
    return (
      this.engines.external?.maxThreads ??
      clamp(navigator.hardwareConcurrency - (navigator.hardwareConcurrency % 2 ? 0 : 1), {
        min: this.engines.active?.minThreads ?? 1,
        max: this.maxThreads,
      })
    );
  }

  get maxThreads(): number {
    return (
      this.engines.external?.maxThreads ??
      (fewerCores()
        ? Math.min(this.engines.active?.maxThreads ?? 32, navigator.hardwareConcurrency)
        : (this.engines.active?.maxThreads ?? 32))
    );
  }

  setHashSize = (hash: number): void => storage.set('ceval.hash-size', hash.toString());

  get hashSize(): number {
    const stored = storage.get('ceval.hash-size');
    return Math.min(this.maxHash, stored ? parseInt(stored, 10) : 16);
  }

  get maxHash(): number {
    return this.engines.active?.maxHash ?? 16;
  }

  selectEngine = (id: string): void => {
    this.engines.select(id);
    this.opts.onSelectEngine?.();
  };

  setPvBoard = (pvBoard: PvBoard | null): void => {
    this.pvBoard(pvBoard);
    this.opts.redraw();
  };

  engineFailed(msg: string): void {
    if (msg.includes('Blocking on the main thread')) return; // mostly harmless
    showEngineError(this.engines.active?.name ?? 'Engine', msg);
    this.worker?.destroy();
    this.worker = undefined;
  }

  private lastEmitFen: string | null = null;
  private sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort((a, b) => povChances(color, b) - povChances(color, a));
}
