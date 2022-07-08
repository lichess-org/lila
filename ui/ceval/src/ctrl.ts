import { CevalCtrl, CevalOpts, CevalTechnology, Work, Step, Hovering, PvBoard, Started } from './types';

import { Result } from '@badrap/result';
import { AbstractWorker, WebWorker, ThreadedWasmWorker, ExternalWorker, ExternalWorkerOpts } from './worker';
import { prop } from 'common';
import { storedBooleanProp, storedIntProp } from 'common/storage';
import throttle from 'common/throttle';
import { povChances } from './winningChances';
import { sanIrreversible } from './util';
import { Cache } from './cache';
import { parseFen } from 'chessops/fen';
import { isStandardMaterial } from 'chessops/chess';
import { defaultPosition, setupPosition } from 'chessops/variant';
import { lichessRules } from 'chessops/compat';

function sharedWasmMemory(initial: number, maximum: number): WebAssembly.Memory {
  return new WebAssembly.Memory({ shared: true, initial, maximum } as WebAssembly.MemoryDescriptor);
}

function sendableSharedWasmMemory(initial: number, maximum: number): WebAssembly.Memory | undefined {
  // Atomics
  if (typeof Atomics !== 'object') return;

  // SharedArrayBuffer
  if (typeof SharedArrayBuffer !== 'function') return;

  // Shared memory
  const mem = sharedWasmMemory(initial, maximum);
  if (!(mem.buffer instanceof SharedArrayBuffer)) return;

  // Structured cloning
  try {
    window.postMessage(mem, '*');
  } catch (e) {
    return;
  }

  return mem;
}

function defaultDepth(technology: CevalTechnology, threads: number, multiPv: number): number {
  const extraDepth = Math.min(Math.max(threads - multiPv, 0), 6);
  switch (technology) {
    case 'asmjs':
      return 18;
    case 'wasm':
      return 20;
    case 'external':
      return 24 + extraDepth;
    default:
      return 22 + extraDepth;
  }
}

function engineName(technology: CevalTechnology, externalOpts: ExternalWorkerOpts | null): string {
  switch (technology) {
    case 'external':
      return externalOpts!.name;
    case 'wasm':
    case 'asmjs':
      return 'Stockfish 10+';
    case 'hce':
      return 'Stockfish 11+';
    case 'nnue':
      return 'Stockfish 14+';
  }
}

const cevalDisabledSentinel = '1';

function enabledAfterDisable() {
  const enabledAfter = lichess.tempStorage.get('ceval.enabled-after');
  const disable = lichess.storage.get('ceval.disable') || cevalDisabledSentinel;
  return enabledAfter === disable;
}

export default function (opts: CevalOpts): CevalCtrl {
  const storageKey = (k: string) => {
    return opts.storageKeyPrefix ? `${opts.storageKeyPrefix}.${k}` : k;
  };
  const enableNnue = storedBooleanProp('ceval.enable-nnue', !(navigator as any).connection?.saveData);

  // check root position
  const rules = lichessRules(opts.variant.key);
  const pos = opts.initialFen
    ? parseFen(opts.initialFen).chain(setup => setupPosition(rules, setup))
    : Result.ok(defaultPosition(rules));
  const analysable = pos.isOk;

  // select nnue > hce > wasm > asmjs
  const officialStockfish = rules == 'chess' && (!analysable || isStandardMaterial(pos.value));
  let technology: CevalTechnology = 'asmjs';
  let growableSharedMem = false;
  let supportsNnue = false;
  const source = Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0]);
  if (typeof WebAssembly === 'object' && typeof WebAssembly.validate === 'function' && WebAssembly.validate(source)) {
    technology = 'wasm'; // WebAssembly 1.0
    const sharedMem = sendableSharedWasmMemory(8, 16);
    if (sharedMem) {
      technology = 'hce';

      // i32x4.dot_i16x8_s, i32x4.trunc_sat_f64x2_u_zero
      const sourceWithSimd = Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0, 1, 12, 2, 96, 2, 123, 123, 1, 123, 96, 1, 123, 1, 123, 3, 3, 2, 0, 1, 7, 9, 2, 1, 97, 0, 0, 1, 98, 0, 1, 10, 19, 2, 9, 0, 32, 0, 32, 1, 253, 186, 1, 11, 7, 0, 32, 0, 253, 253, 1, 11]); // prettier-ignore
      supportsNnue = WebAssembly.validate(sourceWithSimd);
      if (supportsNnue && officialStockfish && enableNnue()) technology = 'nnue';

      try {
        sharedMem.grow(8);
        growableSharedMem = true;
      } catch (e) {
        // memory growth not supported
      }
    }
  }

  const externalOpts: ExternalWorkerOpts | null = JSON.parse(lichess.storage.get('ceval.external') || 'null');
  if (externalOpts && (officialStockfish || externalOpts.variants?.includes(rules))) technology = 'external';

  const initialAllocationMaxThreads = officialStockfish ? 2 : 1;
  const maxThreads =
    technology == 'external'
      ? externalOpts!.maxThreads
      : technology == 'nnue' || technology == 'hce'
      ? Math.min(
          Math.max((navigator.hardwareConcurrency || 1) - 1, 1),
          growableSharedMem ? 32 : initialAllocationMaxThreads
        )
      : 1;
  const threads = () => {
    const stored = lichess.storage.get(storageKey('ceval.threads'));
    return Math.min(maxThreads, stored ? parseInt(stored, 10) : Math.ceil((navigator.hardwareConcurrency || 1) / 4));
  };

  const estimatedMinMemory = technology == 'hce' || technology == 'nnue' ? 2.0 : 0.5;
  const maxHashSize =
    technology == 'external'
      ? externalOpts!.maxHash || 16
      : Math.min(((navigator.deviceMemory || estimatedMinMemory) * 1024) / 8, growableSharedMem ? 1024 : 16);
  const hashSize = () => {
    const stored = lichess.storage.get(storageKey('ceval.hash-size'));
    return Math.min(maxHashSize, stored ? parseInt(stored, 10) : 16);
  };

  const multiPv = storedIntProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  const infinite = storedBooleanProp('ceval.infinite', false);
  let curEval: Tree.LocalEval | null = null;
  const allowed = prop(true);
  const enabled = prop(opts.possible && analysable && allowed() && enabledAfterDisable());
  const downloadProgress = prop(0);
  let running = false;
  let lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  const hovering = prop<Hovering | null>(null);
  const pvBoard = prop<PvBoard | null>(null);
  const isDeeper = prop(false);

  let worker: AbstractWorker<unknown> | undefined;

  let lastEmitFen: string | null = null;
  const onEmit = throttle(200, (ev: Tree.LocalEval, work: Work) => {
    sortPvsInPlace(ev.pvs, work.ply % 2 === (work.threatMode ? 1 : 0) ? 'white' : 'black');
    curEval = ev;
    opts.emit(ev, work);
    if (ev.fen !== lastEmitFen && enabledAfterDisable()) {
      // amnesty while auto disable not processed
      lastEmitFen = ev.fen;
      lichess.storage.fire('ceval.fen', ev.fen);
    }
  });

  const curDepth = () => (curEval ? curEval.depth : 0);

  const effectiveMaxDepth = () => (isDeeper() || infinite() ? 99 : defaultDepth(technology, threads(), multiPv()));

  const sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort(function (a, b) {
      return povChances(color, b) - povChances(color, a);
    });

  const start = (path: Tree.Path, steps: Step[], threatMode: boolean) => {
    if (!enabled() || !opts.possible || !enabledAfterDisable()) return;

    const maxDepth = effectiveMaxDepth();

    const step = steps[steps.length - 1];

    const existing = threatMode ? step.threat : step.ceval;
    if (existing && existing.depth >= maxDepth) {
      lastStarted = {
        path,
        steps,
        threatMode,
      };
      return;
    }

    const work: Work = {
      variant: opts.variant.key,
      threads: threads(),
      hashSize: hashSize(),
      stopRequested: false,

      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path,
      ply: step.ply,
      maxDepth,
      multiPv: multiPv(),
      threatMode,
      emit(ev: Tree.LocalEval) {
        if (enabled()) onEmit(ev, work);
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
        if (sanIrreversible(opts.variant.key, s.san!)) {
          work.moves = [];
          work.initialFen = s.fen;
        } else work.moves.push(s.uci!);
      }
    }

    // Notify all other tabs to disable ceval.
    lichess.storage.fire('ceval.disable');
    lichess.tempStorage.set('ceval.enabled-after', lichess.storage.get('ceval.disable')!);

    if (!worker) {
      if (technology == 'external') worker = new ExternalWorker(externalOpts!);
      else if (technology == 'nnue')
        worker = new ThreadedWasmWorker({
          baseUrl: 'vendor/stockfish-nnue.wasm/',
          module: 'Stockfish',
          downloadProgress: throttle(200, mb => {
            downloadProgress(mb);
            opts.redraw();
          }),
          version: 'b6939d',
          wasmMemory: sharedWasmMemory(2048, growableSharedMem ? 32768 : 2048),
          cache: new Cache('ceval-wasm-cache'),
        });
      else if (technology == 'hce')
        worker = new ThreadedWasmWorker({
          baseUrl: officialStockfish ? 'vendor/stockfish.wasm/' : 'vendor/stockfish-mv.wasm/',
          module: officialStockfish ? 'Stockfish' : 'StockfishMv',
          version: 'a022fa',
          wasmMemory: sharedWasmMemory(1024, growableSharedMem ? 32768 : 1088),
        });
      else
        worker = new WebWorker({
          url: technology == 'wasm' ? 'vendor/stockfish.js/stockfish.wasm.js' : 'vendor/stockfish.js/stockfish.js',
        });
    }

    worker.start(work);

    running = true;
    lastStarted = {
      path,
      steps,
      threatMode,
    };
  };

  function goDeeper() {
    isDeeper(true);
    if (lastStarted) {
      if (infinite()) {
        if (curEval) opts.emit(curEval, lastStarted);
      } else {
        stop();
        start(lastStarted.path, lastStarted.steps, lastStarted.threatMode);
      }
    }
  }

  function stop() {
    if (!enabled() || !running) return;
    worker?.stop();
    running = false;
  }

  const showingCloud = (): boolean => {
    if (!lastStarted) return false;
    const curr = lastStarted.steps[lastStarted.steps.length - 1];
    return !!curr.ceval?.cloud;
  };

  return {
    technology,
    start: (path, steps, threadMode) => {
      isDeeper(false);
      start(path, steps, !!threadMode);
    },
    stop,
    allowed,
    possible: opts.possible,
    enabled,
    downloadProgress,
    multiPv,
    threads,
    setThreads(threads: number) {
      lichess.storage.set(storageKey('ceval.threads'), threads.toString());
    },
    maxThreads,
    hashSize,
    setHashSize(hash: number) {
      lichess.storage.set(storageKey('ceval.hash-size'), hash.toString());
    },
    maxHashSize,
    infinite,
    supportsNnue,
    enableNnue,
    hovering,
    setHovering(fen: Fen, uci?: Uci) {
      hovering(
        uci
          ? {
              fen,
              uci,
            }
          : null
      );
      opts.setAutoShapes();
    },
    pvBoard,
    setPvBoard(_pvBoard: PvBoard | null) {
      pvBoard(_pvBoard);
      opts.redraw();
    },
    toggle() {
      if (!opts.possible || !allowed()) return;
      stop();
      if (!enabled() && !document.hidden) {
        const disable = lichess.storage.get('ceval.disable') || cevalDisabledSentinel;
        if (disable) lichess.tempStorage.set('ceval.enabled-after', disable);
        enabled(true);
      } else {
        lichess.tempStorage.set('ceval.enabled-after', '');
        enabled(false);
      }
    },
    curDepth,
    effectiveMaxDepth,
    variant: opts.variant,
    isDeeper,
    goDeeper,
    canGoDeeper: () => curDepth() < 99 && !isDeeper() && ((!infinite() && !worker?.isComputing()) || showingCloud()),
    isComputing: () => !!running && !!worker?.isComputing(),
    engineName: engineName(technology, externalOpts),
    longEngineName: () => worker?.engineName(),
    destroy: () => worker?.destroy(),
    redraw: opts.redraw,
    cachable:
      technology == 'nnue' || technology == 'hce' || (technology == 'external' && externalOpts!.officialStockfish),
    analysable,
    disconnectExternalEngine() {
      lichess.storage.remove('ceval.external');
    },
  };
}
