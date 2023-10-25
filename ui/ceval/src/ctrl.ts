import throttle from 'common/throttle';
import { Engines } from './engines/engines';
import { CevalOpts, CevalState, CevalEngine, Work, Step, Hovering, PvBoard, Started } from './types';
import { defaultDepth, sanIrreversible } from './util';
import { defaultPosition, setupPosition } from 'chessops/variant';
import { parseFen } from 'chessops/fen';
import { lichessRules, lichessVariant } from 'chessops/compat';
import { povChances } from './winningChances';
import { prop, Toggle, toggle } from 'common';
import { hasFeature } from 'common/device';
import { Result } from '@badrap/result';
import { storedBooleanProp, storedIntProp, StoredProp } from 'common/storage';
import { Rules } from 'chessops';

const cevalDisabledSentinel = '1';

function enabledAfterDisable() {
  const enabledAfter = lichess.tempStorage.get('ceval.enabled-after');
  const disable = lichess.storage.get('ceval.disable') || cevalDisabledSentinel;
  return enabledAfter === disable;
}

export default class CevalCtrl {
  rules: Rules;
  analysable: boolean;
  possible: boolean;
  cachable: boolean;

  engines = new Engines(this);
  enableNnue = storedBooleanProp('ceval.enable-nnue', !(navigator as any).connection?.saveData);
  infinite = storedBooleanProp('ceval.infinite', false);
  multiPv: StoredProp<number>;
  allowed = toggle(true);
  enabled: Toggle;
  download?: { bytes: number; total: number };
  hovering = prop<Hovering | null>(null);
  pvBoard = prop<PvBoard | null>(null);
  isDeeper = toggle(false);
  showEnginePrefs = toggle(false);

  curEval: Tree.LocalEval | null = null;
  lastStarted: Started | false = false; // last started object (for going deeper even if stopped)

  private worker: CevalEngine | undefined;

  constructor(readonly opts: CevalOpts) {
    this.possible = this.opts.possible;

    // check root position
    this.rules = lichessRules(this.opts.variant.key);
    const pos = this.opts.initialFen
      ? parseFen(this.opts.initialFen).chain(setup => setupPosition(this.rules, setup))
      : Result.ok(defaultPosition(this.rules));
    this.analysable = pos.isOk;
    this.enabled = toggle(this.possible && this.analysable && this.allowed() && enabledAfterDisable());
    this.multiPv = storedIntProp(this.storageKey('ceval.multipv'), this.opts.multiPvDefault || 1);
  }

  storageKey = (k: string) => (this.opts.storageKeyPrefix ? `${this.opts.storageKeyPrefix}.${k}` : k);

  private lastEmitFen: string | null = null;
  private sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort((a, b) => povChances(color, b) - povChances(color, a));

  onEmit = throttle(200, (ev: Tree.LocalEval, work: Work) => {
    this.sortPvsInPlace(ev.pvs, work.ply % 2 === (work.threatMode ? 1 : 0) ? 'white' : 'black');
    this.curEval = ev;
    this.opts.emit(ev, work);
    if (ev.fen !== this.lastEmitFen && enabledAfterDisable()) {
      // amnesty while auto disable not processed
      this.lastEmitFen = ev.fen;
      lichess.storage.fire('ceval.fen', ev.fen);
    }
  });

  curDepth = () => this.curEval?.depth || 0;

  effectiveMaxDepth = () =>
    this.isDeeper() || this.infinite()
      ? 99
      : defaultDepth(this.engines.active?.requires, this.threads(), this.multiPv());

  private doStart = (path: Tree.Path, steps: Step[], threatMode: boolean) => {
    if (!this.enabled() || !this.possible || !enabledAfterDisable()) return;

    const maxDepth = this.effectiveMaxDepth();

    const step = steps[steps.length - 1];

    const existing = threatMode ? step.threat : step.ceval;
    if (existing && existing.depth >= maxDepth) {
      this.lastStarted = {
        path,
        steps,
        threatMode,
      };
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
      maxDepth,
      multiPv: this.multiPv(),
      threatMode,
      emit: (ev: Tree.LocalEval) => {
        if (this.enabled()) this.onEmit(ev, work);
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

    if (!this.worker) this.worker = this.engines.make({ variant: lichessVariant(this.rules) });

    this.worker.start(work);

    this.lastStarted = {
      path,
      steps,
      threatMode,
    };
  };

  goDeeper = () => {
    this.isDeeper(true);
    if (this.lastStarted) {
      if (this.infinite()) {
        if (this.curEval) this.opts.emit(this.curEval, this.lastStarted);
      } else {
        this.stop();
        this.doStart(this.lastStarted.path, this.lastStarted.steps, this.lastStarted.threatMode);
      }
    }
    this.opts.redraw();
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

  threads = () => {
    const stored = lichess.storage.get(this.storageKey('ceval.threads'));
    return Math.min(
      this.engines.active?.maxThreads ?? 96, // Can haz threadripper?
      stored ? parseInt(stored, 10) : Math.ceil((navigator.hardwareConcurrency ?? 1) / 4),
    );
  };

  hashSize = () => {
    const stored = lichess.storage.get(this.storageKey('ceval.hash-size'));
    return Math.min(this.engines.active?.maxHash ?? 16, stored ? parseInt(stored, 10) : 16);
  };

  setThreads = (threads: number) => lichess.storage.set(this.storageKey('ceval.threads'), threads.toString());

  setHashSize = (hash: number) => lichess.storage.set(this.storageKey('ceval.hash-size'), hash.toString());

  maxThreads = () =>
    this.engines.external?.maxThreads ?? (hasFeature('sharedMem') ? navigator.hardwareConcurrency ?? 4 : 1);

  maxHash = () => this.engines.active?.maxHash ?? 16;

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

  selectEngine = (id: string) => {
    this.engines.select(id);
    lichess.reload();
  };

  canGoDeeper = () =>
    this.curDepth() < 99 &&
    !this.isDeeper() &&
    ((!this.infinite() && this.getState() !== CevalState.Computing) || this.showingCloud());

  destroy = () => this.worker?.destroy();
}
