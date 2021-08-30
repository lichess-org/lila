import { CevalCtrl, CevalOpts, CevalTechnology, Work, Step, Hovering, PvBoard, Started } from './types';

import { AbstractWorker, WebWorker, ThreadedWasmWorker } from './worker';
import { prop } from 'common';
import { storedProp } from 'common/storage';
import throttle from 'common/throttle';
import { povChances } from './winningChances';
import { sanIrreversible } from './util';
import { Cache } from './cache';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';
import { lichessRules } from 'chessops/compat';
import { COLORS } from 'chessops/types';
import { SquareSet } from 'chessops/squareSet';

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
  switch (technology) {
    case 'asmjs':
      return 18;
    case 'wasm':
      return 20;
    default:
      return 22 + Math.min(Math.max(threads - multiPv, 0), 6);
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
  const enableNnue = storedProp('ceval.enable-nnue', !(navigator as any).connection?.saveData);

  // check root position
  const setup = opts.initialFen ? parseFen(opts.initialFen).unwrap() : undefined;
  const rules = lichessRules(opts.variant.key);
  const analysable = setup ? setupPosition(rules, setup).isOk || true : true;
  const standardMaterial = setup
    ? COLORS.every(color => {
        const board = setup.board;
        const pieces = board[color];
        const promotedPieces =
          Math.max(board.queen.intersect(pieces).size() - 1, 0) +
          Math.max(board.rook.intersect(pieces).size() - 2, 0) +
          Math.max(board.knight.intersect(pieces).size() - 2, 0) +
          Math.max(board.bishop.intersect(pieces).intersect(SquareSet.lightSquares()).size() - 1, 0) +
          Math.max(board.bishop.intersect(pieces).intersect(SquareSet.darkSquares()).size() - 1, 0);
        return board.pawn.intersect(pieces).size() + promotedPieces <= 8;
      })
    : true;

  // select nnue > hce > wasm > asmjs
  const officialStockfish = standardMaterial && rules == 'chess';
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

  const initialAllocationMaxThreads = officialStockfish ? 2 : 1;
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

  const multiPv = storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  const infinite = storedProp('ceval.infinite', false);
  let curEval: Tree.ClientEval | null = null;
  const allowed = prop(true);
  const enabled = prop(opts.possible && analysable && allowed() && enabledAfterDisable());
  const downloadProgress = prop(0);
  let started: Started | false = false;
  let lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  const hovering = prop<Hovering | null>(null);
  const pvBoard = prop<PvBoard | null>(null);
  const isDeeper = prop(false);

  const protocolOpts = {
    variant: opts.variant.key,
    threads: (technology == 'hce' || technology == 'nnue') && (() => Math.min(parseInt(threads()), maxThreads)),
    hashSize: (technology == 'hce' || technology == 'nnue') && (() => Math.min(parseInt(hashSize()), maxHashSize)),
  };

  let worker: AbstractWorker<unknown> | undefined;

  let lastEmitFen: string | null = null;
  const onEmit = throttle(200, (ev: Tree.ClientEval, work: Work) => {
    sortPvsInPlace(ev.pvs, work.ply % 2 === (work.threatMode ? 1 : 0) ? 'white' : 'black');
    curEval = ev;
    opts.emit(ev, work);
    if (ev.fen !== lastEmitFen && enabledAfterDisable()) {
      // amnesty while auto disable not processed
      lastEmitFen = ev.fen;
      lichess.storage.fire('ceval.fen', ev.fen);
    }
  });

  const effectiveMaxDepth = () =>
    isDeeper() || infinite()
      ? 99
      : defaultDepth(technology, protocolOpts.threads ? protocolOpts.threads() : 1, parseInt(multiPv()));

  const sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort(function (a, b) {
      return povChances(color, b) - povChances(color, a);
    });

  const start = (path: Tree.Path, steps: Step[], threatMode: boolean, deeper: boolean) => {
    if (!enabled() || !opts.possible || !enabledAfterDisable()) return;

    isDeeper(deeper);
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
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path,
      ply: step.ply,
      maxDepth,
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
          version: 'b6939d',
          wasmMemory: sharedWasmMemory(2048, growableSharedMem ? 32768 : 2048),
          cache: new Cache('ceval-wasm-cache'),
        });
      else if (technology == 'hce')
        worker = new ThreadedWasmWorker(protocolOpts, {
          baseUrl: officialStockfish ? 'vendor/stockfish.wasm/' : 'vendor/stockfish-mv.wasm/',
          module: officialStockfish ? 'Stockfish' : 'StockfishMv',
          version: 'a022fa',
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
    analysable,
  };
}
