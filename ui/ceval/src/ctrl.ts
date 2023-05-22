import throttle from 'common/throttle';
import { CevalState, CevalWorker, WebWorker, ThreadedWasmWorker, ExternalWorker } from './worker';
import { Cache } from './cache';
import { CevalOpts, Work, Step, Hovering, PvBoard, Started, EngineType, FallbackType } from './types';
import { defaultDepth, engineName, isEvalBetter, sanIrreversible, sharedWasmMemory } from './util';
import { defaultPosition, setupPosition } from 'chessops/variant';
import { parseFen } from 'chessops/fen';
import { isStandardMaterial } from 'chessops/chess';
import { lichessRules } from 'chessops/compat';
import { povChances } from './winningChances';
import { prop, toggle } from 'common';
import { Result } from '@badrap/result';
import { storedBooleanProp, storedIntProp, StoredProp, storedStringProp } from 'common/storage';
import { Rules } from 'chessops';
import { CevalPlatform, CevalTechnology, detectPlatform } from './platform';
import { Api as ChessgroundApi } from 'chessground/api';

export default class CevalCtrl {
  rules: Rules;
  analysable: boolean;
  possible: boolean;
  private officialStockfish: boolean;
  private initialEngineType: EngineType | undefined;

  platform: CevalPlatform;
  technology: CevalTechnology;

  enableNnue = storedBooleanProp('ceval.enable-nnue', !(navigator as any).connection?.saveData);
  infinite = storedBooleanProp('ceval.infinite', false);
  multiPv: StoredProp<number>;
  allowed = toggle(true);
  downloadProgress = prop(0);
  hovering = prop<Hovering | null>(null);
  pvBoard = prop<PvBoard | null>(null);
  isDeeper = toggle(false);
  actionMenu = toggle(false);

  curEval: Tree.LocalEval | null = null;
  lastStarted: Started | false = false; // last started object (for going deeper even if stopped)

  private workers = new Map<EngineType, CevalWorker>();

  private getWorker(): CevalWorker | undefined {
    return this.workers.get(this.getEngineType());
  }

  private findExternalEngine = () => {
    return this.opts.externalEngines?.find(
      e =>
        'external-' + e.id === this.getEngineType() &&
        (this.officialStockfish || e.variants.map(lichessRules).includes(this.rules))
    );
  };

  externalEngines() {
    return (
      this.opts.externalEngines?.map(({ id, name, variants }) => ({
        type: `external-${id}`,
        name: name,
        disabled: !variants.map(lichessRules).includes(this.rules),
      })) ?? []
    );
  }

  isCachable(): boolean {
    return this.technology === 'nnue' || this.technology === 'hce' || !!this.findExternalEngine()?.officialStockfish;
  }

  constructor(readonly opts: CevalOpts) {
    this.possible = this.opts.possible;

    this.initialEngineType = this.engineType();
    if (this.initialEngineType === 'disabled' || this.initialEngineType === 'server')
      this.initialEngineType = undefined;

    // check root position
    this.rules = lichessRules(this.opts.variant.key);
    const pos = this.opts.initialFen
      ? parseFen(this.opts.initialFen).chain(setup => setupPosition(this.rules, setup))
      : Result.ok(defaultPosition(this.rules));
    this.analysable = pos.isOk;
    this.officialStockfish = this.rules == 'chess' && (pos.isErr || isStandardMaterial(pos.value));

    this.platform = detectPlatform(this.officialStockfish, this.enableNnue(), this.findExternalEngine);
    this.technology = this.platform.technology;

    this.multiPv = storedIntProp(this.storageKey('ceval.multipv'), this.opts.multiPvDefault || 1);
  }

  storageKey = (k: string) => (this.opts.storageKeyPrefix ? `${this.opts.storageKeyPrefix}.${k}` : k);

  threads = () => {
    const stored = lichess.storage.get(this.storageKey('ceval.threads'));
    return Math.min(
      this.platform.maxThreads(),
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
    if (ev.fen !== this.lastEmitFen) {
      // amnesty while auto disable not processed
      this.lastEmitFen = ev.fen;
      lichess.storage.fire('ceval.fen', ev.fen);
    }
  });

  curDepth = () => this.curEval?.depth || 0;

  effectiveMaxDepth = () =>
    this.isDeeper() || this.infinite() ? 99 : defaultDepth(this.technology, this.threads(), this.multiPv());

  private doStart = (path: Tree.Path, steps: Step[], threatMode: boolean) => {
    if (!this.enabled() || !this.possible) return;

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

    const type = this.getEngineType();
    let worker: CevalWorker | undefined = this.getWorker();

    if (!worker) {
      if (type.startsWith('external-')) worker = new ExternalWorker(this.findExternalEngine()!, this.opts.redraw);
      else if (this.technology == 'nnue')
        worker = new ThreadedWasmWorker(
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
        worker = new ThreadedWasmWorker(
          {
            baseUrl: this.officialStockfish ? 'vendor/stockfish.wasm/' : 'vendor/stockfish-mv.wasm/',
            module: this.officialStockfish ? 'Stockfish' : 'StockfishMv',
            version: 'a022fa',
            wasmMemory: sharedWasmMemory(1024, this.platform.maxWasmPages(1088)),
          },
          this.opts.redraw
        );
      else
        worker = new WebWorker(
          {
            url:
              this.technology == 'wasm' ? 'vendor/stockfish.js/stockfish.wasm.js' : 'vendor/stockfish.js/stockfish.js',
          },
          this.opts.redraw
        );
      this.workers.set(type, worker);
    }

    worker.start(work);

    this.lastStarted = {
      path,
      steps,
      threatMode,
    };
  };

  continue = () => {
    if (this.curDepth() >= this.effectiveMaxDepth()) this.isDeeper(true);
    if (this.lastStarted) {
      if (this.infinite()) {
        if (this.curEval) this.opts.emit(this.curEval, this.lastStarted);
      } else {
        this.stop();
        this.doStart(this.lastStarted.path, this.lastStarted.steps, this.lastStarted.threatMode);
      }
    } else {
      this.startCeval();
    }
    this.opts.redraw();
  };

  stop = () => this.getWorker()?.stop();

  showingCloud = (): boolean => {
    if (!this.lastStarted) return false;
    const curr = this.lastStarted.steps[this.lastStarted.steps.length - 1];
    return !!curr.ceval?.cloud;
  };

  private start = (path: string, steps: Step[], threatMode?: boolean) => {
    this.isDeeper(false);
    this.doStart(path, steps, !!threatMode);
  };

  getState = () => this.getWorker()?.getState() ?? CevalState.Initial;

  setThreads = (threads: number) => lichess.storage.set(this.storageKey('ceval.threads'), threads.toString());
  setHashSize = (hash: number) => lichess.storage.set(this.storageKey('ceval.hash-size'), hash.toString());

  setHovering = (fen: Fen, uci?: Uci) => {
    this.hovering(uci ? { fen, uci } : null);
    this.opts.setAutoShapes();
  };
  setPvBoard = (pvBoard: PvBoard | null) => {
    this.pvBoard(pvBoard);
    this.opts.redraw();
  };

  private engineType: StoredProp<EngineType> = storedStringProp(
    'ceval.engine-type',
    'disabled'
  ) as StoredProp<EngineType>;

  setEngineType(type: EngineType) {
    if (!this.possible || !this.allowed()) return;
    if (this.engineType() !== type) this.stop();
    this.engineType(type);
    this.opts.engineChanged();
    if (type.startsWith('external-') || type === 'local') {
      if (!this.initialEngineType) this.initialEngineType = type;
      if (type !== this.initialEngineType) {
        location.reload();
        return;
      }
    } else {
      this.threatMode(false);
    }
    this.startCeval();
  }
  getEngineType(): EngineType {
    if (!this.possible || !this.allowed()) return 'disabled';
    const type = this.engineType();
    if (
      (type === 'server' && !this.opts.showServerAnalysis) ||
      (type.startsWith('external-') &&
        !this.opts.externalEngines?.some(
          e => 'external-' + e.id === type && e.variants.map(lichessRules).includes(this.rules)
        ))
    )
      return 'local';
    return type;
  }
  enable() {
    if (this.engineType() === 'disabled') this.setEngineType(this.initialEngineType ?? 'local');
  }
  enabled() {
    return this.getEngineType() !== 'disabled';
  }
  showClientEval() {
    return (
      this.enabled() &&
      this.getEngineType() !== 'server' &&
      (this.getFallbackType() !== 'complement' || !this.opts.getNode().eval || this.threatMode())
    );
  }

  useServerEval() {
    return this.getEngineType() === 'server' || (this.enabled() && this.getFallbackType() !== 'disabled');
  }

  private serverComments = storedBooleanProp('show-comments', true);
  setShowServerComments(show: boolean) {
    this.serverComments(show);
    if (!show) this.opts.tree.removeComputerVariations();
    this.opts.redraw();
  }
  showServerComments() {
    return this.serverComments() && this.useServerEval();
  }

  private fallbackType: StoredProp<FallbackType> = storedStringProp(
    'ceval.fallback-type',
    'overwrite'
  ) as StoredProp<FallbackType>;
  getFallbackType(): FallbackType {
    return this.fallbackType();
  }
  setFallbackType(type: FallbackType) {
    this.fallbackType(type);
    this.opts.engineChanged();
  }

  canPause = () => this.getState() === CevalState.Computing && !this.infinite() && !this.showingCloud();

  canContinue = () =>
    this.enabled() &&
    (this.curDepth() < 99 || !this.isDeeper()) &&
    ((!this.infinite() && this.getState() !== CevalState.Computing) || this.showingCloud());

  localEngineName = () => engineName(this.technology);
  longEngineName = () => this?.getWorker()?.engineName();

  destroy = () => {
    for (const worker of this.workers.values()) worker.destroy();
  };

  showAutoShapes = storedBooleanProp('show-auto-shapes', true);
  showGauge = storedBooleanProp('show-gauge', true);
  showMoveAnnotation = storedBooleanProp('show-move-annotation', true);

  cgVersion = {
    js: 1, // increment to recreate chessground
    dom: 1,
  };

  withCg = <A>(f: (cg: ChessgroundApi) => A): A | undefined => {
    const chessground = this.opts.getChessground();
    if (!chessground) return;
    if (this.cgVersion.js !== this.cgVersion.dom) return;
    return f(chessground);
  };

  onNewCeval = (ev: Tree.ClientEval, path: Tree.Path, isThreat?: boolean): void => {
    this.opts.tree.updateAt(path, (node: Tree.Node) => {
      if (node.fen !== ev.fen && !isThreat) return;
      if (isThreat) {
        const threat = ev as Tree.LocalEval;
        if (!node.threat || isEvalBetter(threat, node.threat) || node.threat.maxDepth < threat.maxDepth)
          node.threat = threat;
      } else if (!node.ceval || isEvalBetter(ev, node.ceval)) node.ceval = ev;
      else if (!ev.cloud) {
        if (node.ceval.cloud && this.isDeeper()) node.ceval = ev;
        else if (ev.maxDepth > node.ceval.maxDepth!) node.ceval.maxDepth = ev.maxDepth;
      }

      if (path === this.opts.getPath()) {
        this.opts.setAutoShapes();
        if (!isThreat) {
          const retro = this.opts.getRetro?.(),
            practice = this.opts.getPractice?.(),
            studyPractice = this.opts.getStudyPractice?.();
          if (retro) retro.onCeval();
          if (practice) practice.onCeval();
          if (studyPractice) studyPractice.onCeval();
          this.opts.evalCache.onCeval();
          if (ev.cloud && ev.depth >= this.effectiveMaxDepth()) this.stop();
        }
        this.opts.redraw();
      }
    });
  };

  private resetAutoShapes() {
    const chessground = this.opts.getChessground();
    if (this.showAutoShapes() || this.showMoveAnnotation()) this.opts.setAutoShapes();
    else if (chessground) chessground.setAutoShapes([]);
  }

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

  private cevalReset(): void {
    this.stop();
    this.enable();
    this.startCeval();
    this.opts.redraw();
  }

  cevalSetMultiPv = (v: number): void => {
    this.multiPv(v);
    this.opts.tree.removeCeval();
    this.opts.evalCache.clear();
    this.cevalReset();
  };

  cevalSetThreads = (v: number): void => {
    this.setThreads(v);
    this.cevalReset();
  };

  cevalSetHashSize = (v: number): void => {
    this.setHashSize(v);
    this.cevalReset();
  };

  cevalSetInfinite = (v: boolean): void => {
    this.infinite(v);
    this.cevalReset();
  };

  threatMode = prop(false);

  canUseCeval(): boolean {
    return !this.opts.getNode().threefold && !this.opts.outcome();
  }

  startCeval = throttle(800, () => {
    if (this.showClientEval()) {
      if (this.canUseCeval()) {
        this.start(this.opts.getPath(), this.opts.getNodeList(), this.threatMode());
        this.opts.evalCache?.fetch(this.opts.getPath(), this.multiPv());
      } else this.stop();
    }
  });

  toggleThreatMode = () => {
    if (this.opts.getNode().check) return;
    this.threatMode(!this.threatMode());
    if (this.threatMode()) {
      if (!this.enabled() || this.getEngineType() === 'server') this.setEngineType(this.initialEngineType ?? 'local');
      if (this.opts.getPractice?.()) this.opts.togglePractice?.();
    }
    this.opts.setAutoShapes();
    this.startCeval();
    this.opts.redraw();
  };
}
