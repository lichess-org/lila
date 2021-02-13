import { CevalCtrl, CevalOpts, CevalTechnology, Work, Step, Hovering, Started } from './types';

import { Pool, officialStockfish } from './pool';
import { defined, prop } from 'common';
import { storedProp } from 'common/storage';
import throttle from 'common/throttle';
import { povChances } from './winningChances';
import { sanIrreversible } from './util';

const li = window.lichess;

function is64Bit(): boolean {
  const x64 = ['x86_64', 'x86-64', 'Win64','x64', 'amd64', 'AMD64'];
  for (const substr of x64) if (navigator.userAgent.includes(substr)) return true;
  return navigator.platform === 'Linux x86_64' || navigator.platform === 'MacIntel';
}

function sharedWasmMemory(initial: number, maximum: number): WebAssembly.Memory | undefined {
  // TODO: In theory 32 bit should be supported just the same, but some 32 bit
  // browser builds seem to have trouble with WASMX. So for now detect and
  // require a 64 bit platform.
  if (!is64Bit()) return;

  // Atomics
  if (typeof Atomics !== 'object') return;

  // SharedArrayBuffer
  if (typeof SharedArrayBuffer !== 'function') return;

  // Shared memory
  const mem = new WebAssembly.Memory({shared: true, initial, maximum} as WebAssembly.MemoryDescriptor);
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

export default function(opts: CevalOpts): CevalCtrl {
  const storageKey = (k: string) => {
    return opts.storageKeyPrefix ? `${opts.storageKeyPrefix}.${k}` : k;
  };

  // select wasmx with growable shared mem > wasmx > wasm > asmjs
  let technology: CevalTechnology = 'asmjs';
  let growableSharedMem = false;
  const source = Uint8Array.of(0x0, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00);
  if (typeof WebAssembly === 'object' && typeof WebAssembly.validate === 'function' && WebAssembly.validate(source)) {
    technology = 'wasm'; // WebAssembly 1.0
    const sharedMem = sharedWasmMemory(8, 16);
    if (sharedMem) {
      technology = 'wasmx';
      if (!defined(window['crossOriginIsolated'])) window['crossOriginIsolated'] = true; // polyfill
      try {
        sharedMem.grow(8);
        growableSharedMem = true;
      } catch (e) { }
    }
  }

  const initialAllocationMaxThreads = officialStockfish(opts.variant.key) ? 2 : 1;
  const maxThreads = Math.min(Math.max((navigator.hardwareConcurrency || 1) - 1, 1), growableSharedMem ? 16 : initialAllocationMaxThreads);
  const threads = storedProp(storageKey('ceval.threads'), Math.min(Math.ceil((navigator.hardwareConcurrency || 1) / 4), maxThreads));

  const maxHashSize = Math.min((navigator.deviceMemory || 0.25) * 1024 / 8, growableSharedMem ? 1024 : 16);
  const hashSize = storedProp(storageKey('ceval.hash-size'), 16);

  const minDepth = 6;
  const maxDepth = storedProp<number>(storageKey('ceval.max-depth'), 18);
  const multiPv = storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  const infinite = storedProp('ceval.infinite', false);
  let curEval: Tree.ClientEval | null = null;
  const enableStorage = li.storage.makeBoolean(storageKey('client-eval-enabled'));
  const allowed = prop(true);
  const enabled = prop(opts.possible && allowed() && enableStorage.get() && !document.hidden);
  let started: Started | false = false;
  let lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  const hovering = prop<Hovering | null>(null);
  const isDeeper = prop(false);

  const pool = new Pool({
    technology,
    asmjs: 'vendor/stockfish.js/stockfish.js',
    wasm: 'vendor/stockfish.js/stockfish.wasm.js',
    wasmx: officialStockfish(opts.variant.key) ? 'vendor/stockfish.wasm/stockfish.js' : 'vendor/stockfish-mv.wasm/stockfish.js',
  }, {
    minDepth,
    variant: opts.variant.key,
    threads: technology == 'wasmx' && (() => Math.min(parseInt(threads()), maxThreads)),
    hashSize: technology == 'wasmx' && (() => Math.min(parseInt(hashSize()), maxHashSize)),
  });

  // adjusts maxDepth based on nodes per second
  const npsRecorder = (function() {
    const values: number[] = [];
    const applies = (ev: Tree.ClientEval) => {
      return ev.knps && ev.depth >= 16 &&
        typeof ev.cp !== 'undefined' && Math.abs(ev.cp) < 500 &&
        (ev.fen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1) >= 10;
    };
    return function(ev: Tree.ClientEval) {
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
        maxDepth(depth);
        if (values.length > 40) values.shift();
      }
    };
  })();

  let lastEmitFen: string | null = null;

  const onEmit = throttle(200, (ev: Tree.ClientEval, work: Work) => {
    sortPvsInPlace(ev.pvs, (work.ply % 2 === (work.threatMode ? 1 : 0)) ? 'white' : 'black');
    npsRecorder(ev);
    curEval = ev;
    opts.emit(ev, work);
    if (ev.fen !== lastEmitFen) {
      lastEmitFen = ev.fen;
      li.storage.fire('ceval.fen', ev.fen);
    }
  });

  const effectiveMaxDepth = () => (isDeeper() || infinite()) ? 99 : parseInt(maxDepth());

  const sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort(function(a, b) {
      return povChances(color, b) - povChances(color, a);
    });

  const start = (path: Tree.Path, steps: Step[], threatMode: boolean, deeper: boolean) => {

    if (!enabled() || !opts.possible) return;

    isDeeper(deeper);
    const maxD = effectiveMaxDepth();

    const step = steps[steps.length - 1];

    const existing = threatMode ? step.threat : step.ceval;
    if (existing && existing.depth >= maxD) return;

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
      }
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

    pool.start(work);

    started = {
      path,
      steps,
      threatMode
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
    pool.stop();
    lastStarted = started;
    started = false;
  }

  // ask other tabs if a game is in progress
  if (enabled()) {
    li.storage.fire('ceval.fen', 'start');
    li.storage.make('round.ongoing').listen(_ => {
      enabled(false);
      opts.redraw();
    });
  }

  return {
    technology,
    start,
    stop,
    allowed,
    possible: opts.possible,
    enabled,
    multiPv,
    threads: technology == 'wasmx' ? threads : undefined,
    hashSize: technology == 'wasmx' ? hashSize : undefined,
    maxThreads,
    maxHashSize,
    infinite,
    hovering,
    setHovering(fen: Fen, uci?: Uci) {
      hovering(uci ? {
        fen,
        uci
      } : null);
      opts.setAutoShapes();
    },
    toggle() {
      if (!opts.possible || !allowed()) return;
      stop();
      enabled(!enabled());
      if (document.visibilityState !== 'hidden')
        enableStorage.set(enabled());
    },
    curDepth: () => curEval ? curEval.depth : 0,
    effectiveMaxDepth,
    variant: opts.variant,
    isDeeper,
    goDeeper,
    canGoDeeper: () => !isDeeper() && !infinite() && !pool.isComputing(),
    isComputing: () => !!started && pool.isComputing(),
    engineName: pool.engineName,
    destroy: pool.destroy,
    redraw: opts.redraw
  };
}
