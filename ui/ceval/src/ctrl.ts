import { CevalCtrl, CevalOpts, CevalTechnology, Work, Step, Hovering, Started } from './types';

import { AbstractWorker, WebWorker, ThreadedWasmWorker, officialStockfish } from './worker';
import { prop } from 'common';
import { storedProp } from 'common/storage';
import throttle from 'common/throttle';
import { povChances } from './winningChances';
import { sanIrreversible } from './util';

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

function median(values: number[]): number {
  values.sort((a, b) => a - b);
  const half = Math.floor(values.length / 2);
  return values.length % 2 ? values[half] : (values[half - 1] + values[half]) / 2.0;
}

function enabledAfterDisable() {
  const enabledAfter = lichess.tempStorage.get('ceval.enabled-after');
  const disable = lichess.storage.get('ceval.disable');
  return !disable || enabledAfter === disable;
}

export default function (opts: CevalOpts): CevalCtrl {
  const storageKey = (k: string) => {
    return opts.storageKeyPrefix ? `${opts.storageKeyPrefix}.${k}` : k;
  };
  const enableNnue = storedProp(storageKey('ceval.enable-nnue'), !(navigator as any).connection?.saveData);

  // select nnue > hce > wasm > asmjs
  let technology: CevalTechnology = 'asmjs';
  let growableSharedMem = false;
  let supportsNnue = false;
  const source = Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0]);
  if (typeof WebAssembly === 'object' && typeof WebAssembly.validate === 'function' && WebAssembly.validate(source)) {
    technology = 'wasm'; // WebAssembly 1.0
    const sharedMem = sendableSharedWasmMemory(8, 16);
    if (sharedMem) {
      technology = 'hce';

      // i32x4.dot_i16x8_s
      const sourceWithSimd = Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 96, 0, 1, 123, 3, 2, 1, 0, 7, 8, 1, 4, 116, 101, 115, 116, 0, 0, 10, 15, 1, 13, 0, 65, 0, 253, 17, 65, 0, 253, 17, 253, 186, 1, 11]); // prettier-ignore
      supportsNnue = WebAssembly.validate(sourceWithSimd);
      if (supportsNnue && enableNnue() && officialStockfish(opts.variant.key)) {
        technology = 'nnue';
      }

      try {
        sharedMem.grow(8);
        growableSharedMem = true;
      } catch (e) {}
    }
  }

  const initialAllocationMaxThreads = officialStockfish(opts.variant.key) ? 2 : 1;
  const maxThreads = Math.min(
    Math.max((navigator.hardwareConcurrency || 1) - 1, 1),
    growableSharedMem ? 32 : initialAllocationMaxThreads
  );
  const threads = storedProp(
    storageKey('ceval.threads'),
    Math.min(Math.ceil((navigator.hardwareConcurrency || 1) / 4), maxThreads)
  );

  const maxHashSize = Math.min(((navigator.deviceMemory || 0.25) * 1024) / 8, growableSharedMem ? 1024 : 16);
  const hashSize = storedProp(storageKey('ceval.hash-size'), 16);

  const maxDepth = storedProp<number>(storageKey('ceval.max-depth'), 18);
  const multiPv = storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  const infinite = storedProp('ceval.infinite', false);
  let curEval: Tree.ClientEval | null = null;
  const allowed = prop(true);
  const enabled = prop(opts.possible && allowed() && enabledAfterDisable());
  const downloadProgress = prop(0);
  let started: Started | false = false;
  let lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  const hovering = prop<Hovering | null>(null);
  const isDeeper = prop(false);

  const protocolOpts = {
    variant: opts.variant.key,
    threads: (technology == 'hce' || technology == 'nnue') && (() => Math.min(parseInt(threads()), maxThreads)),
    hashSize: (technology == 'hce' || technology == 'nnue') && (() => Math.min(parseInt(hashSize()), maxHashSize)),
  };

  let worker: AbstractWorker<unknown> | undefined;

  // adjusts maxDepth based on nodes per second
  const npsRecorder = (() => {
    const values: number[] = [];
    const applies = (ev: Tree.ClientEval) => {
      return (
        ev.knps &&
        ev.depth >= 16 &&
        typeof ev.cp !== 'undefined' &&
        Math.abs(ev.cp) < 500 &&
        ev.fen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1 >= 10
      );
    };
    return (ev: Tree.ClientEval) => {
      if (!applies(ev)) return;
      values.push(ev.knps);
      if (values.length > 9) {
        const knps = median(values) || 0;
        let depth = 18;
        if (knps > 100) depth = 19;
        if (knps > 150) depth = 20;
        if (knps > 250) depth = 21;
        if (knps > 500) depth = 22;
        if (knps > 1000) depth = 23;
        if (knps > 2000) depth = 24;
        if (knps > 3500) depth = 25;
        if (knps > 5000) depth = 26;
        if (knps > 7000) depth = 27;
        // TODO: Maybe we want to get deeper for slow NNUE?
        depth += 2 * Number(technology === 'nnue');
        maxDepth(depth);
        if (values.length > 40) values.shift();
      }
    };
  })();

  let lastEmitFen: string | null = null;

  const onEmit = throttle(200, (ev: Tree.ClientEval, work: Work) => {
    sortPvsInPlace(ev.pvs, work.ply % 2 === (work.threatMode ? 1 : 0) ? 'white' : 'black');
    npsRecorder(ev);
    curEval = ev;
    opts.emit(ev, work);
    if (ev.fen !== lastEmitFen && enabledAfterDisable()) {
      // amnesty while auto disable not processed
      lastEmitFen = ev.fen;
      lichess.storage.fire('ceval.fen', ev.fen);
    }
  });

  const effectiveMaxDepth = () => (isDeeper() || infinite() ? 99 : parseInt(maxDepth()));

  const sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort(function (a, b) {
      return povChances(color, b) - povChances(color, a);
    });

  const start = (path: Tree.Path, steps: Step[], threatMode: boolean, deeper: boolean) => {
    if (!enabled() || !opts.possible || !enabledAfterDisable()) return;

    isDeeper(deeper);
    const maxD = effectiveMaxDepth();

    const step = steps[steps.length - 1];

    const existing = threatMode ? step.threat : step.ceval;
    if (existing && existing.depth >= maxD) {
      lastStarted = {
        path,
        steps,
        threatMode,
      };
      return;
    }

    const work: Work = {
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path,
      ply: step.ply,
      maxDepth: maxD,
      multiPv: parseInt(multiPv()),
      threatMode,
      emit(ev: Tree.ClientEval) {
        if (enabled()) onEmit(ev, work);
      },
      stopRequested: false,
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
      if (technology == 'nnue')
        worker = new ThreadedWasmWorker(protocolOpts, {
          baseUrl: 'vendor/stockfish-nnue.wasm/',
          module: 'Stockfish',
          downloadProgress: throttle(200, mb => {
            downloadProgress(mb);
            opts.redraw();
          }),
          version: '85a969',
          wasmMemory: sharedWasmMemory(2048, growableSharedMem ? 32768 : 2048),
        });
      else if (technology == 'hce')
        worker = new ThreadedWasmWorker(protocolOpts, {
          baseUrl: officialStockfish(opts.variant.key) ? 'vendor/stockfish.wasm/' : 'vendor/stockfish-mv.wasm/',
          module: officialStockfish(opts.variant.key) ? 'Stockfish' : 'StockfishMv',
          wasmMemory: sharedWasmMemory(1024, growableSharedMem ? 32768 : 1088),
        });
      else
        worker = new WebWorker(protocolOpts, {
          url: technology == 'wasm' ? 'vendor/stockfish.js/stockfish.wasm.js' : 'vendor/stockfish.js/stockfish.js',
        });
    }

    worker.start(work);

    started = {
      path,
      steps,
      threatMode,
    };
  };

  function goDeeper() {
    const s = started || lastStarted;
    if (s) {
      stop();
      start(s.path, s.steps, s.threatMode, true);
    }
  }

  function stop() {
    if (!enabled() || !started) return;
    worker?.stop();
    lastStarted = started;
    started = false;
  }

  return {
    technology,
    start,
    stop,
    allowed,
    possible: opts.possible,
    enabled,
    downloadProgress,
    multiPv,
    threads: technology == 'hce' || technology == 'nnue' ? threads : undefined,
    hashSize: technology == 'hce' || technology == 'nnue' ? hashSize : undefined,
    maxThreads,
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
    toggle() {
      if (!opts.possible || !allowed()) return;
      stop();
      if (!enabled() && !document.hidden) {
        const disable = lichess.storage.get('ceval.disable');
        if (disable) lichess.tempStorage.set('ceval.enabled-after', disable);
        enabled(true);
      } else {
        lichess.tempStorage.set('ceval.enabled-after', '');
        enabled(false);
      }
    },
    curDepth: () => (curEval ? curEval.depth : 0),
    effectiveMaxDepth,
    variant: opts.variant,
    isDeeper,
    goDeeper,
    canGoDeeper: () => !isDeeper() && !infinite() && !worker?.isComputing(),
    isComputing: () => !!started && !!worker?.isComputing(),
    engineName: () => worker?.engineName(),
    destroy: () => worker?.destroy(),
    redraw: opts.redraw,
  };
}
