import throttle from 'common/throttle';
import { Engines } from './engines/engines';
import { CevalOpts, CevalState, CevalEngine, Work, Step, Hovering, PvBoard, Started } from './types';
import { sanIrreversible, showEngineError, fewerCores, constrain } from './util';
import { defaultPosition, setupPosition } from 'chessops/variant';
import { parseFen } from 'chessops/fen';
import { lichessRules } from 'chessops/compat';
import { povChances } from './winningChances';
import { prop, readonlyProp, Prop, Toggle, toggle } from 'common';
import { Result } from '@badrap/result';
import { storedIntProp } from 'common/storage';
import { Rules } from 'chessops';

const cevalDisabledSentinel = '1';

function enabledAfterDisable() {
  const enabledAfter = lichess.tempStorage.get('ceval.enabled-after');
  const disable = lichess.storage.get('ceval.disable') || cevalDisabledSentinel;
  return enabledAfter === disable;
}

export default class CevalCtrl {
  opts: CevalOpts;
  rules: Rules;
  analysable: boolean;
  possible: boolean;

  engines: Engines;
  multiPv: Prop<number>;
  allowed = toggle(true);
  enabled: Toggle;
  download?: { bytes: number; total: number };
  hovering = prop<Hovering | null>(null);
  pvBoard = prop<PvBoard | null>(null);
  isDeeper = toggle(false);

  curEval: Tree.LocalEval | null = null;
  lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  searchMs: Prop<number>;
  showEnginePrefs = toggle(false);

  private worker: CevalEngine | undefined;

  constructor(opts: CevalOpts) {
    this.configure(opts);
    this.engines = new Engines(this);
  }

  configure(opts: CevalOpts) {
    this.opts = opts;
    this.possible = this.opts.possible;
    this.rules = lichessRules(this.opts.variant.key);
    const pos = this.opts.initialFen
      ? parseFen(this.opts.initialFen).chain(setup => setupPosition(this.rules, setup))
      : Result.ok(defaultPosition(this.rules));
    this.analysable = pos.isOk;
    this.enabled = toggle(this.possible && this.analysable && this.allowed() && enabledAfterDisable());
    this.setSearch(this.opts.search);
    if (this.worker?.getInfo().id !== this.engines?.activate()?.id) {
      this.worker?.destroy();
      this.worker = undefined;
    }
  }

  setSearch(s?: { searchMs?: number; multiPv?: number }) {
    this.searchMs = s?.searchMs ? readonlyProp<number>(s.searchMs) : storedIntProp('ceval.search-ms', 8000);
    this.multiPv = s?.multiPv ? readonlyProp<number>(s.multiPv) : storedIntProp('ceval.multipv', 1);
  }

  onEmit = throttle(200, (ev: Tree.LocalEval, work: Work) => {
    this.sortPvsInPlace(ev.pvs, work.ply % 2 === (work.threatMode ? 1 : 0) ? 'white' : 'black');
    this.curEval = ev;
    this.opts.emit(ev, work);
    if (ev.fen !== this.lastEmitFen && enabledAfterDisable()) {
      // amnesty while auto disable not processed
      this.lastEmitFen = ev.fen;
      lichess.storage.fire('ceval.fen', ev.fen);
    }
    if (!this.lastStarted || this.isDeeper() || this.infinite() || work.threatMode) return;

    const showingNode = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    if (showingNode.ceval?.cloud && ev.millis > 500) {
      const targetNodes = showingNode.ceval.nodes;
      const likelyNodes = Math.round((this.searchMs() * ev.nodes) / ev.millis);

      // nps varies with positional complexity so this is rough, but save planet earth
      if (likelyNodes < targetNodes) this.stop(); // let them click plus
    }
  });

  curDepth = () => this.curEval?.depth || 0;

  private doStart = (path: Tree.Path, steps: Step[], threatMode: boolean) => {
    if (!this.enabled() || !this.possible || !enabledAfterDisable()) return;

    const step = steps[steps.length - 1];

    if (!this.isDeeper() && ((threatMode ? step.threat : step.ceval)?.millis ?? 0) >= this.searchMs()) {
      this.lastStarted = { path, steps, threatMode };
      return;
    }

    const work: Work = {
      variant: this.opts.variant.key,
      threads: this.threads(),
      hashSize: this.hashSize(),
      stopRequested: false,
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path,
      ply: step.ply,
      searchMs: this.isDeeper() ? Number.POSITIVE_INFINITY : this.searchMs(),
      multiPv: this.multiPv(),
      threatMode,
      emit: (ev: Tree.LocalEval) => {
        if (!this.enabled()) return;
        this.onEmit(ev, work);
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
    lichess.storage.fire('ceval.disable');
    lichess.tempStorage.set('ceval.enabled-after', lichess.storage.get('ceval.disable')!);

    if (!this.worker) this.worker = this.engines.make({ variant: this.opts.variant.key });

    this.worker.start(work);

    this.lastStarted = {
      path,
      steps,
      threatMode,
    };
  };

  goDeeper = () => {
    if (!this.lastStarted) return;
    this.isDeeper(true);
    this.doStart(this.lastStarted.path, this.lastStarted.steps, this.lastStarted.threatMode);
  };

  stop = () => {
    this.worker?.stop();
  };

  showingCloud = (): boolean => {
    if (!this.lastStarted) return false;
    const curr = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    return !!curr.ceval?.cloud;
  };

  start = (path: string, steps: Step[], threatMode?: boolean) => {
    this.isDeeper(false);
    this.doStart(path, steps, !!threatMode);
  };

  getState() {
    return this.worker?.getState() ?? CevalState.Initial;
  }

  setThreads = (threads: number) => lichess.storage.set('ceval.threads', threads.toString());

  threads = () => {
    const stored = lichess.storage.get('ceval.threads');
    const desired = stored ? parseInt(stored) : this.recommendedThreads();
    return constrain(desired, { min: this.engines.active?.minThreads ?? 1, max: this.maxThreads() });
  };

  recommendedThreads = () =>
    this.engines.external?.maxThreads ??
    constrain(navigator.hardwareConcurrency - (navigator.hardwareConcurrency % 2 ? 0 : 1), {
      min: this.engines.active?.minThreads ?? 1,
      max: this.maxThreads(),
    });

  maxThreads = () =>
    this.engines.external?.maxThreads ??
    (fewerCores()
      ? Math.min(this.engines.active?.maxThreads ?? 32, navigator.hardwareConcurrency)
      : this.engines.active?.maxThreads ?? 32);

  setHashSize = (hash: number) => lichess.storage.set('ceval.hash-size', hash.toString());

  hashSize = () => {
    const stored = lichess.storage.get('ceval.hash-size');
    return Math.min(this.maxHash(), stored ? parseInt(stored, 10) : 16);
  };

  maxHash = () => this.engines.active?.maxHash ?? 16;

  selectEngine = (id: string) => {
    this.engines.select(id);
    this.opts.onSelectEngine?.();
  };

  setHovering = (fen: Fen, uci?: Uci) => {
    this.hovering(uci ? { fen, uci } : null);
    this.opts.setAutoShapes();
  };

  setPvBoard = (pvBoard: PvBoard | null) => {
    this.pvBoard(pvBoard);
    this.opts.redraw();
  };

  toggle = () => {
    if (!this.possible || !this.allowed()) return;
    this.stop();
    if (!this.enabled() && !document.hidden) {
      const disable = lichess.storage.get('ceval.disable') || cevalDisabledSentinel;
      if (disable) lichess.tempStorage.set('ceval.enabled-after', disable);
      this.enabled(true);
    } else {
      lichess.tempStorage.set('ceval.enabled-after', '');
      this.enabled(false);
      this.download = undefined;
    }
  };

  canGoDeeper = () => this.getState() !== CevalState.Computing && this.curDepth() < 99;

  infinite = () => this.searchMs() === Number.POSITIVE_INFINITY;
  computing = () => this.getState() === CevalState.Computing;
  destroy = () => {
    this.worker?.destroy();
    this.worker = undefined;
  };

  engineFailed(msg: string) {
    if (msg.includes('Blocking on the main thread')) return; // mostly harmless
    showEngineError(this.engines.active?.name ?? 'Engine', msg);
  }

  cacheable() {
    return this.engines.active?.tech === 'NNUE';
  }

  private lastEmitFen: string | null = null;
  private sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort((a, b) => povChances(color, b) - povChances(color, a));
}
