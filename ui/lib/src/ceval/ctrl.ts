// no side effects allowed due to re-export by index.ts

import { isStandardMaterial } from 'chessops/chess';
import { lichessRules } from 'chessops/compat';
import { parseFen } from 'chessops/fen';
import { type Rules } from 'chessops/types';
import { setupPosition } from 'chessops/variant';

import { clamp } from '@/algo';
import { throttleWithFlush } from '@/async';
import { isTouchDevice } from '@/device';
import { pubsub } from '@/pubsub';
import { storedIntProp, storedStringProp, storage, storedMap } from '@/storage';
import type { ClientEval, LocalEval, TreePath } from '@/tree/types';

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
import { sanIrreversible, showEngineError } from './util';
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

type ThreadCount = number;
type NodesPerSecond = number;

export class CevalCtrl {
  rules: Rules;
  nonStandardMaterial: boolean;
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
  wasUnloadedByAnotherWindow = false;

  private readonly performanceMap = storedMap<Record<ThreadCount, NodesPerSecond[]>>(
    'ceval.perf',
    12,
    () => ({}),
  );
  private worker?: CevalEngine;

  constructor(public opts: CevalOpts) {
    this.engines = new Engines(this);
    this.storedEngine = storedStringProp(`ceval.engine.${opts.variant.key}`, '');
    this.init();

    // another tab has started ceval, we should stop:
    storage.make('ceval.fen').listen(() => {
      if (this.isBackground || !this.worker) return;
      this.worker.destroy();
      this.worker = undefined; // release memory
      this.wasUnloadedByAnotherWindow = true;
      this.opts.redraw();
    });

    document.addEventListener('visibilitychange', () => {
      if (
        this.engines.external() ||
        this.curEval?.bestmove ||
        !this.lastStarted ||
        !this.analysable ||
        this.isBackground ||
        !isTouchDevice()
      ) {
        return;
      }
      if (document.hidden) this.worker?.stop();
      else this.doStart(this.lastStarted);
    });
  }

  init(opts?: CevalOpts): void {
    if (opts) this.opts = opts;
    this.reset();
    this.rules = lichessRules(this.opts.variant.key);
    const pos = this.opts.initialFen
      ? parseFen(this.opts.initialFen).chain(x => setupPosition(this.rules, x))
      : undefined;
    this.nonStandardMaterial =
      this.rules === 'chess' &&
      !!pos?.unwrap(
        pos => !isStandardMaterial(pos),
        _ => false,
      );
    this.analysable =
      !pos?.isErr &&
      !!this.engines.getEngine({ rules: this.rules, nonStandardMaterial: this.nonStandardMaterial });
    this.engines.setActive(this.opts.custom?.engine?.id ?? this.storedEngine());
    if (this.worker?.getInfo().id !== this.engines.active()?.id) this.unload();
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
    this.wasUnloadedByAnotherWindow = false;
    this.curEval = null;
    this.lastStarted = undefined;
    this.download = undefined;
  };

  start = (path: string, steps: Step[], gameId: string | undefined, threatMode = false): boolean => {
    if (!this.available() || this.wasUnloadedByAnotherWindow) return false;
    this.isDeeper(false);
    this.doStart({ path, steps, gameId, threatMode });
    return true;
  };

  setThreads = (threads: number): void => storage.set('ceval.threads', threads.toString());

  info(customSearch?: CustomSearch): SearchInfo | undefined {
    const searchOverrides = customSearch?.search?.();
    const engine = this.engines.getEngine({ id: customSearch?.engine?.id });
    if (!engine) return undefined;
    const maxTime =
      (searchOverrides && 'maxMovetime' in searchOverrides && searchOverrides.maxMovetime) ||
      engine.maxMovetime;
    return {
      engine,
      threads: clamp(
        customSearch?.engine?.threads ?? (Number(storage.get('ceval.threads')) || this.recommendedThreads),
        { min: engine.minThreads, max: engine.maxThreads },
      ),
      hashSize: clamp(customSearch?.engine?.hashSize ?? Number(storage.get('ceval.hash-size')), {
        min: 16,
        max: engine.maxHash,
      }),
      search:
        searchOverrides && 'by' in searchOverrides
          ? searchOverrides
          : {
              multiPv: Math.min(this.storedPv(), searchOverrides?.maxMultiPv ?? Infinity),
              by: { movetime: clamp(this.isDeeper() ? Infinity : this.storedMovetime(), { max: maxTime }) },
            },
    };
  }

  get search(): Search {
    return this.info(this.opts.custom)?.search ?? { by: { movetime: 0 }, multiPv: 0 };
  }

  get recommendedThreads(): number {
    return (
      this.engines.external()?.maxThreads ??
      clamp(navigator.hardwareConcurrency - (navigator.hardwareConcurrency % 2 ? 0 : 1), {
        min: this.engines.active()?.minThreads ?? 1,
        max: this.engines.active()?.maxThreads,
      })
    );
  }

  get isInfinite(): boolean {
    return (
      this.storedMovetime() === Number.POSITIVE_INFINITY &&
      !Number.isFinite(this.engines.active()?.maxMovetime)
    );
  }

  get state(): CevalState {
    return this.worker?.getState() ?? CevalState.Initial;
  }

  get canGoDeeper(): boolean {
    // recently raised from 99. keep an eye out for screenshots of wasm exceptions in github issues and
    // feedback forum.
    return this.state !== CevalState.Computing && (this.opts.localEval?.()?.depth ?? 0) < 245;
  }

  get isComputing(): boolean {
    return this.state === CevalState.Computing;
  }

  get isCacheable(): boolean {
    return Boolean(this.engines.active()?.supportsCloudEval);
  }

  get engineVersion(): string | undefined {
    return (this.engines.external() && this.worker?.version?.()) || this.engines.active()?.name;
  }

  get isBackground(): boolean {
    return this.opts.custom?.canBackground === true;
  }

  get showingCloud(): boolean {
    if (!this.lastStarted) return false;
    const curr = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    return Boolean(curr.ceval?.cloud);
  }

  setHashSize = (hash: number): void => storage.set('ceval.hash-size', hash.toString());

  selectEngine = (id: string): void => {
    this.storedEngine(id);
    this.engines.setActive(id);
    this.opts.onSelectEngine?.();
  };

  private unload(): void {
    this.worker?.stop();
    this.worker?.destroy();
    this.worker = undefined;
  }

  setPvBoard = (pvBoard: PvBoard | null): void => {
    this.pvBoard(pvBoard);
    this.opts.redraw();
  };

  engineFailed(msg: string): void {
    if (msg.includes('Blocking on the main thread')) return; // mostly harmless
    if (!this.opts.hideErrors) showEngineError(String(this.engines.active()?.name), msg);
    this.reset();
    this.unload();
  }

  isFinished(search: Search, step: Step): boolean {
    return (
      !this.isDeeper() &&
      'movetime' in search.by &&
      !step.ceval?.cloud &&
      (step.threat?.millis ?? step.ceval?.millis ?? 0) >= search.by.movetime &&
      step.ceval?.pvs.length === search.multiPv &&
      step.ceval?.engineId === this.engines.active()?.id
    );
  }

  // Node counts, like depth, dont compare well across engines, but cloud evals have no engineId field.
  // So cross-engine comparisons are only allowed for cloud evals (a tradeoff that defers to their utility).
  // This function always prefers the latest unless:
  // - latest has the wrong multipv and stored eval has the right one
  // - stored eval has higher node count AND either stored and latest lack engineId or their engineIds match

  preferLatestEval(latest: ClientEval, stored: ClientEval | null | undefined): boolean {
    if (!stored) return true;
    const multipv = this.search.multiPv;
    if (stored.pvs.length === multipv && latest.pvs.length !== multipv) return false;
    if (latest.pvs.length === multipv && stored.pvs.length !== multipv) return true;
    if ('engineId' in stored && 'engineId' in latest && stored.engineId !== latest.engineId) return true;
    return latest.nodes >= stored.nodes;
  }

  nodesPerSecond(engineId: string, threads: number): number | undefined {
    const snapshots = this.performanceMap(engineId);
    if (!snapshots) return undefined;

    const average = (arr: number[]) => arr.reduce((a: number, b: number) => a + b, 0) / arr.length;
    if (snapshots[threads]?.length) return average(snapshots[threads]);

    const perfs: number[] = [];
    for (const thread in snapshots) {
      perfs.push((average(snapshots[thread]) * threads) / Number(thread));
    }
    return average(perfs);
  }

  private snapshotPerformance(ev: LocalEval) {
    const { engine, threads } = this.info()!;
    if (!engine) return;

    const snapshots = this.performanceMap(engine.id);
    (snapshots[threads] ??= []).push(ev.nodes / (ev.millis / 1000));
    snapshots[threads] = snapshots[threads].slice(-5);
    this.performanceMap(engine.id, snapshots);
  }

  private readonly doStart = (s: Started) => {
    this.lastStarted = s;
    const step = s.steps[s.steps.length - 1];
    const { search, threads, hashSize, engine } = this.info(this.opts.custom)!;
    if (this.isFinished(search, step)) return;

    const work: Work = {
      variant: this.rules,
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
      const fields = step.fen.split(' ');
      fields[1] = step.ply % 2 === 1 ? 'w' : 'b';
      fields[3] = '-'; // no en passant square in threat mode
      const fen = fields.join(' ');
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

    if (this.worker?.getInfo().id !== engine.id) this.unload();
    this.worker ??= this.engines.makeEngine({
      id: engine.id,
      rules: this.rules,
      nonStandardMaterial: this.nonStandardMaterial,
    });
    this.worker.start(work);
  };

  private makeThrottledEmitter() {
    // 'working' properties are bound for closure
    const working = {
      started: this.lastStarted!,
      fen: undefined as string | undefined,
      emit: this.opts.emit,
      background: this.isBackground,
      movetime: 'movetime' in this.search.by && this.search.by.movetime,
      dontStop: Boolean(this.engines.external() || this.opts.custom || this.isDeeper() || this.isInfinite),
    };
    const emitter = throttleWithFlush(125, (ev: LocalEval, meta: EvalMeta) => {
      this.curEval = ev;
      ev.engineId = this.engines.active()?.id;
      if (ev.bestmove && ev.bestmove !== '(none)' && working.movetime !== false) {
        this.snapshotPerformance(ev);
        ev.millis = Math.max(ev.millis, working.movetime); // ensure bestmove eval matches movetime target
      }
      if (!working.fen) {
        working.fen = ev.fen;
        storage.fire('ceval.fen', ev.fen); // will pause other tabs
      }
      const color = meta.ply % 2 === (meta.threatMode ? 1 : 0) ? 'white' : 'black';
      ev.pvs.sort((a, b) => povChances(color, b) - povChances(color, a));

      if (this.lastStarted && !working.dontStop) {
        const evNode = working.started.steps[working.started.steps.length - 1];
        if (working.movetime && evNode.ceval?.cloud && ev.millis > 500) {
          const targetNodes = evNode.ceval.nodes;
          const likelyNodes = Math.round((working.movetime * ev.nodes) / ev.millis);

          if (likelyNodes < targetNodes) this.worker?.stop();
        }
      }
      working.emit(ev, meta);
    });
    return (ev: LocalEval | undefined, meta: EvalMeta) => {
      if (!ev) {
        working.emit(undefined, meta); // report error
      } else if (working.started === this.lastStarted && (!working.fen || working.fen === ev.fen)) {
        pubsub.emit('analysis.eval', structuredClone(ev), meta);
        if (ev.bestmove) emitter.flush(ev, meta);
        else emitter(ev, meta);
      } else {
        emitter.clear();
      }
    };
  }
}
