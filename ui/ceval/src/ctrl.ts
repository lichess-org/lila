import throttle from 'common/throttle';
import { CevalState, CevalWorker, WebWorker, ThreadedWasmWorker, ExternalEngine, ExternalWorker } from './worker';
import { Cache } from './cache';
import { CevalOpts, Work, Step, Hovering, PvBoard, Started } from './types';
import { defaultDepth, engineName, sanIrreversible, sharedWasmMemory } from './util';
import { defaultPosition, setupPosition } from 'chessops/variant';
import { parseFen } from 'chessops/fen';
import { isStandardMaterial } from 'chessops/chess';
import { lichessRules } from 'chessops/compat';
import { povChances } from './winningChances';
import { Prop, prop, toggle } from 'common';
import { Result } from '@badrap/result';
import { storedBooleanProp, storedIntProp, StoredProp, storedStringProp } from 'common/storage';
import { Rules } from 'chessops';
import { CevalPlatform, CevalTechnology, detectPlatform } from './platform';

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
  private officialStockfish: boolean;

  platform: CevalPlatform;
  technology: CevalTechnology;

  selectedEngine: StoredProp<string> = storedStringProp('ceval.engine', 'lichess');
  externalEngine?: ExternalEngine; // if selected, available, and usable for current rules

  enableNnue = storedBooleanProp('ceval.enable-nnue', !(navigator as any).connection?.saveData);
  infinite = storedBooleanProp('ceval.infinite', false);
  multiPv: StoredProp<number>;
  setAllowed = toggle(true);
  allowed = () => !this.opts.disallowed?.() && this.setAllowed();
  downloadProgress = prop(0);
  hovering = prop<Hovering | null>(null);
  pvBoard = prop<PvBoard | null>(null);
  isDeeper = toggle(false);
  actionMenu = toggle(false);

  showServer = storedBooleanProp('ceval.show-server', true);
  showLive = storedBooleanProp('ceval.show-live', false);
  show = () => this.showServer() || this.showLive();

  curEval: Tree.LocalEval | null = null;
  lastStarted: Started | false = false; // last started object (for going deeper even if stopped)

  private worker: CevalWorker | undefined;

  constructor(readonly opts: CevalOpts) {
    this.possible = this.opts.possible;

    // check root position
    this.rules = lichessRules(this.opts.variant.key);
    const pos = this.opts.initialFen
      ? parseFen(this.opts.initialFen).chain(setup => setupPosition(this.rules, setup))
      : Result.ok(defaultPosition(this.rules));
    this.analysable = pos.isOk;
    this.officialStockfish = this.rules == 'chess' && (pos.isErr || isStandardMaterial(pos.value));

    this.externalEngine = this.opts.externalEngines?.find(
      e =>
        e.id == this.selectedEngine() && (this.officialStockfish || e.variants.map(lichessRules).includes(this.rules))
    );
    this.platform = detectPlatform(this.officialStockfish, this.enableNnue(), this.externalEngine);
    this.technology = this.platform.technology;

    this.multiPv = storedIntProp(this.storageKey('ceval.multipv'), this.opts.multiPvDefault || 1);
    this.cachable = this.technology == 'nnue' || this.technology == 'hce' || !!this.externalEngine?.officialStockfish;
  }

  storageKey = (k: string) => (this.opts.storageKeyPrefix ? `${this.opts.storageKeyPrefix}.${k}` : k);

  threads = () => {
    const stored = lichess.storage.get(this.storageKey('ceval.threads'));
    return Math.min(
      this.platform.maxThreads,
      stored ? parseInt(stored, 10) : Math.ceil((navigator.hardwareConcurrency || 1) / 4)
    );
  };

  hashSize = () => {
    const stored = lichess.storage.get(this.storageKey('ceval.hash-size'));
    return Math.min(this.platform.maxHashSize(), stored ? parseInt(stored, 10) : 16);
  };

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
    this.isDeeper() || this.infinite() ? 99 : defaultDepth(this.technology, this.threads(), this.multiPv());

  private doStart = (path: Tree.Path, steps: Step[], threatMode: boolean) => {
    if (!this.showLive() || !this.possible || !enabledAfterDisable()) return;

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
        if (this.showLive()) this.onEmit(ev, work);
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

    if (!this.worker) {
      if (this.externalEngine) this.worker = new ExternalWorker(this.externalEngine, this.opts.redraw);
      else if (this.technology == 'nnue')
        this.worker = new ThreadedWasmWorker(
          {
            baseUrl: 'vendor/stockfish-nnue.wasm/',
            module: 'Stockfish',
            downloadProgress: throttle(200, mb => {
              this.downloadProgress(mb);
              this.opts.redraw();
            }),
            version: 'b6939d',
            wasmMemory: sharedWasmMemory(2048, this.platform.maxWasmPages(2048)),
            cache: window.indexedDB && new Cache('ceval-wasm-cache'),
          },
          this.opts.redraw
        );
      else if (this.technology == 'hce')
        this.worker = new ThreadedWasmWorker(
          {
            baseUrl: this.officialStockfish ? 'vendor/stockfish.wasm/' : 'vendor/stockfish-mv.wasm/',
            module: this.officialStockfish ? 'Stockfish' : 'StockfishMv',
            version: 'a022fa',
            wasmMemory: sharedWasmMemory(1024, this.platform.maxWasmPages(1088)),
          },
          this.opts.redraw
        );
      else
        this.worker = new WebWorker(
          {
            url:
              this.technology == 'wasm' ? 'vendor/stockfish.js/stockfish.wasm.js' : 'vendor/stockfish.js/stockfish.js',
          },
          this.opts.redraw
        );
    }

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

  private start = (path: string, steps: Step[], threatMode?: boolean) => {
    this.isDeeper(false);
    this.doStart(path, steps, !!threatMode);
  };

  getState() {
    return this.worker ? this.worker.getState() : CevalState.Initial;
  }

  private setThreadsStorage = (threads: number) => lichess.storage.set(this.storageKey('ceval.threads'), threads.toString());
  private setHashSizeStorage = (hash: number) => lichess.storage.set(this.storageKey('ceval.hash-size'), hash.toString());

  setHovering = (fen: Fen, uci?: Uci) => {
    this.hovering(uci ? { fen, uci } : null);
    this.setAutoShapes();
  };
  setPvBoard = (pvBoard: PvBoard | null) => {
    this.pvBoard(pvBoard);
    this.opts.redraw();
  };
  private toggle = () => {
    if (!this.possible || !this.allowed()) return;
    this.stop();
    if (!this.showLive() && !document.hidden) {
      const disable = lichess.storage.get('ceval.disable') || cevalDisabledSentinel;
      if (disable) lichess.tempStorage.set('ceval.enabled-after', disable);
      this.showLive(true);
    } else {
      lichess.tempStorage.set('ceval.enabled-after', '');
      this.showLive(false);
    }
  };
  selectEngine = (id: string) => {
    this.selectedEngine(this.opts.externalEngines?.find(e => e.id == id) ? id : 'lichess');
    lichess.reload();
  };
  canGoDeeper = () =>
    this.curDepth() < 99 &&
    !this.isDeeper() &&
    ((!this.infinite() && this.getState() !== CevalState.Computing) || this.showingCloud());
  shortEngineName = () => engineName(this.technology, this.externalEngine);
  longEngineName = () => this.worker?.engineName();
  destroy = () => this.worker?.destroy();

  showAutoShapes = storedBooleanProp('show-auto-shapes', true);

  startCeval = throttle(800, () => {
    if (this.allowed() && !this.opts.getNode().threefold && !this.opts.outcome()) {
      if (this.showLive()) {
        this.start(this.opts.getPath(), this.opts.getNodeList(), this.threatMode());
        this.opts.evalCache.fetch(this.opts.getPath(), this.multiPv());
      }
    } else this.stop();
  });

  private cevalReset(): void {
    this.stop();
    this.startCeval();
    this.opts.redraw();
  }

  showGauge = storedBooleanProp('show-gauge', false);
  showMoveAnnotation = storedBooleanProp('show-move-annotation', true);
  keyboardHelp: boolean = location.hash === '#keyboard';
  threatMode: Prop<boolean> = prop(false);

  setMultiPv = (v: number): void => {
    this.multiPv(v);
    this.opts.tree.removeCeval();
    this.opts.evalCache.clear();
    this.cevalReset();
  };

  setThreads = (v: number): void => {
    this.setThreadsStorage(v);
    this.cevalReset();
  };

  setHashSize = (v: number): void => {
    this.setHashSizeStorage(v);
    this.cevalReset();
  };

  setInfinite = (v: boolean): void => {
    this.infinite(v);
    this.cevalReset();
  };

  resetAutoShapes() {
    const chessground = this.opts.getChessground();
    if (this.showAutoShapes() || this.showMoveAnnotation()) this.setAutoShapes();
    else if (chessground) chessground.setAutoShapes([]);
  }

  setAutoShapes = (): void => {
    this.opts.getChessground()?.setAutoShapes(this.opts.computeAutoShapes());
  };

  toggleAutoShapes = (v: boolean): void => {
    this.showAutoShapes(v);
    this.resetAutoShapes();
  };

  toggleGauge = () => {
    this.showGauge(!this.showGauge());
  };

  toggleMoveAnnotation = (v: boolean): void => {
    this.showMoveAnnotation(v);
    this.resetAutoShapes();
  };

  toggleLive = () => {
    this.toggle();
    this.setAutoShapes();
    this.startCeval();
    if (!this.showLive())
      this.threatMode(false);
    this.opts.liveToggled?.();
    this.opts.redraw();
  };

  toggleServer = (v: boolean = !this.showServer()) => {
    this.showServer(v);
    if (!this.showServer())
      this.opts.tree.removeComputerVariations();
    this.opts.redraw();
  }

  toggleThreatMode = () => {
    if (this.opts.disableThreatMode?.()) return;
    if (this.opts.getNode().check) return;
    if (!this.showLive()) this.toggle();
    if (!this.showLive()) return;
    this.threatMode(!this.threatMode());
    this.setAutoShapes();
    this.startCeval();
    this.opts.redraw();
  };
}
