import { Result } from '@badrap/result';
import { prop } from 'common/common';
import { EngineCode, engineCode, engineName } from 'common/engineName';
import { isImpasse } from 'common/impasse';
import { isAndroid, isIOS, isIPad } from 'common/mobile';
import { storedProp } from 'common/storage';
import throttle from 'common/throttle';
import { parseSfen } from 'shogiops/sfen';
import { defaultPosition } from 'shogiops/variant/variant';
import { Cache } from './cache';
import { CevalCtrl, CevalOpts, CevalTechnology, Hovering, PvBoard, Started, Step, Work } from './types';
import { unsupportedVariants } from './util';
import { povChances } from './winningChances';
import { AbstractWorker, ThreadedWasmWorker } from './worker';

const sharedWasmMemory = (initial: number, maximum: number): WebAssembly.Memory =>
  new WebAssembly.Memory({ shared: true, initial, maximum } as WebAssembly.MemoryDescriptor);

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
    window.postMessage(mem.buffer, '*');
  } catch (e) {
    return undefined;
  }

  return mem;
}

function defaultDepth(technology: CevalTechnology, threads: number, multiPv: number): number {
  const extraDepth = Math.min(Math.max(threads - multiPv, 0), 6);
  switch (technology) {
    case 'none':
      return 0;
    case 'hce':
      return 18 + extraDepth;
    case 'nnue':
      return 20 + extraDepth;
  }
}

const cevalDisabledSentinel = '1';

function enabledAfterDisable() {
  const enabledAfter = window.lishogi.tempStorage.get('ceval.enabled-after');
  const disable = window.lishogi.storage.get('ceval.disable') || cevalDisabledSentinel;
  return enabledAfter === disable;
}

export default function (opts: CevalOpts): CevalCtrl {
  const storageKey = (k: string) => {
    return opts.storageKeyPrefix ? `${opts.storageKeyPrefix}.${k}` : k;
  };
  const enableNnue = storedProp('ceval.enable-nnue', true);

  // check root position
  const pos = opts.initialSfen
    ? parseSfen(opts.variant.key, opts.initialSfen, false)
    : Result.ok(defaultPosition(opts.variant.key));
  const analysable = pos.isOk && !unsupportedVariants.includes(opts.variant.key);

  // select nnue > hce > none
  const useYaneuraou = analysable && engineCode(opts.variant.key, opts.initialSfen) === EngineCode.YaneuraOu,
    fairySupports = analysable && !useYaneuraou;
  let supportsNnue = false,
    technology: CevalTechnology = 'none',
    growableSharedMem = false;
  const source = Uint8Array.from([0, 97, 115, 109, 1, 0, 0, 0]);
  if (
    (useYaneuraou || fairySupports) &&
    typeof WebAssembly === 'object' &&
    typeof WebAssembly.validate === 'function' &&
    WebAssembly.validate(source)
  ) {
    const sharedMem = sendableSharedWasmMemory(1, 2);
    if (sharedMem) {
      technology = 'hce';

      const sourceWithSimd = Uint8Array.from([
        0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 96, 0, 1, 123, 3, 2, 1, 0, 7, 8, 1, 4, 116, 101, 115, 116, 0, 0, 10, 15,
        1, 13, 0, 65, 0, 253, 17, 65, 0, 253, 17, 253, 186, 1, 11,
      ]);
      supportsNnue = WebAssembly.validate(sourceWithSimd);
      if (useYaneuraou && enableNnue()) technology = 'nnue';

      try {
        sharedMem.grow(1);
        growableSharedMem = true;
      } catch (e) {
        // memory growth not supported
      }
    }
  }

  const initialAllocationMaxThreads = useYaneuraou ? 2 : 1,
    maxThreads = Math.min(
      Math.max((navigator.hardwareConcurrency || 1) - 1, 1),
      growableSharedMem ? 32 : initialAllocationMaxThreads
    );
  const threads = () => {
    const stored = window.lishogi.storage.get(storageKey('ceval.threads'));
    return Math.min(maxThreads, stored ? parseInt(stored, 10) : Math.ceil((navigator.hardwareConcurrency || 1) / 4));
  };
  const pow2floor = (n: number) => {
    let pow2 = 1;
    while (pow2 * 2 <= n) pow2 *= 2;
    return pow2;
  };
  const maxWasmPages = (minPages: number): number => {
    if (!growableSharedMem) return minPages;
    let maxPages = 32768; // hopefully desktop browser, 2 GB max shared
    if (isAndroid())
      maxPages = 8192; // 512 MB max shared
    else if (isIPad())
      maxPages = 8192; // 512 MB max shared
    else if (isIOS()) maxPages = 4096; // 256 MB max shared
    return Math.max(minPages, maxPages);
  };
  const maxHashMB = (): number => {
    let maxHash = 256; // this is conservative but safe, mostly desktop firefox / mac safari users here
    if (navigator.deviceMemory) maxHash = pow2floor(navigator.deviceMemory * 128); // chrome/edge/opera

    if (isAndroid())
      maxHash = 64; // budget androids are easy to crash @ 128
    else if (isIPad())
      maxHash = 64; // ipados safari pretends to be desktop but acts more like iphone
    else if (isIOS()) maxHash = 32;

    return maxHash;
  };
  const maxHashSize = maxHashMB();

  const hashSize = () => {
    const stored = window.lishogi.storage.get(storageKey('ceval.hash-size'));
    return Math.min(maxHashSize, stored ? parseInt(stored, 10) : 16);
  };
  const multiPv = storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  const enteringKingRule = storedProp(storageKey('ceval.enteringKingRule'), true);
  const infinite = storedProp('ceval.infinite', false);
  let curEval: Tree.LocalEval | null = null;
  const allowed = prop(true);
  const enabled = prop(technology !== 'none' && opts.possible && analysable && allowed() && enabledAfterDisable());
  const downloadProgress = prop(0);
  let running = false;
  let lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  const hovering = prop<Hovering | null>(null);
  const pvBoard = prop<PvBoard | null>(null);
  const isDeeper = prop(false);

  let worker: AbstractWorker<unknown> | undefined;

  let lastEmitSfen: string | null = null;
  const onEmit = throttle(200, (ev: Tree.LocalEval, work: Work) => {
    sortPvsInPlace(ev.pvs, work.ply % 2 === (work.threatMode ? 1 : 0) ? 'sente' : 'gote');
    curEval = ev;
    opts.emit(ev, work);
    if (ev.sfen !== lastEmitSfen && enabledAfterDisable()) {
      // amnesty while auto disable not processed
      lastEmitSfen = ev.sfen;
      window.lishogi.storage.fire('ceval.sfen', ev.sfen);
    }
  });

  const curDepth = () => (curEval ? curEval.depth : 0);

  const effectiveMaxDepth = () =>
    isDeeper() || infinite() ? 99 : defaultDepth(technology, threads(), parseInt(multiPv()));

  const sortPvsInPlace = (pvs: Tree.PvData[], color: Color) =>
    pvs.sort(function (a, b) {
      return povChances(color, b) - povChances(color, a);
    });

  const start = (path: Tree.Path, steps: Step[], threatMode: boolean) => {
    const step = steps[steps.length - 1];

    if (!step) return;

    const impassePosition = isImpasse(opts.variant.key, step.sfen, steps[0].sfen);

    if (!enabled() || !opts.possible || !enabledAfterDisable() || (impassePosition && enteringKingRule())) return;

    const maxDepth = effectiveMaxDepth();

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
      enteringKingRule: enteringKingRule(),
      stopRequested: false,
      initialSfen: steps[0].sfen,
      moves: [],
      currentSfen: step.sfen,
      path,
      ply: step.ply,
      maxDepth,
      multiPv: parseInt(multiPv()),
      threatMode,
      emit(ev: Tree.LocalEval) {
        if (enabled()) onEmit(ev, work);
      },
    };

    if (threatMode) {
      const c = step.ply % 2 === 1 ? 'b' : 'w';
      const sfen = step.sfen.replace(/ (b|w) /, ' ' + c + ' ');
      work.currentSfen = sfen;
      work.initialSfen = sfen;
    } else {
      for (let i = 1; i < steps.length; i++) {
        const s = steps[i];
        work.moves.push(s.usi!);
      }
    }

    // Notify all other tabs to disable ceval.
    window.lishogi.storage.fire('ceval.disable');
    window.lishogi.tempStorage.set('ceval.enabled-after', window.lishogi.storage.get('ceval.disable')!);

    if (!worker) {
      if (technology == 'nnue')
        worker = new ThreadedWasmWorker({
          baseName: 'yaneuraou.k-p',
          baseUrl: 'vendor/yaneuraou.k-p/lib/',
          module: 'YaneuraOu_K_P',
          downloadProgress: throttle(200, mb => {
            downloadProgress(mb);
            opts.redraw();
          }),
          version: 'e01aa3c',
          wasmMemory: sharedWasmMemory(2048, maxWasmPages(2048)),
          cache: window.indexedDB && new Cache('ceval-wasm-cache'),
        });
      else if (technology == 'hce')
        worker = new ThreadedWasmWorker({
          baseName: 'stockfish',
          baseUrl: 'vendor/fairy/',
          module: 'Stockfish',
          version: 'b02c911',
          wasmMemory: sharedWasmMemory(2048, maxWasmPages(2048)),
        });
    }

    if (worker) worker.start(work);

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
    enteringKingRule,
    threads,
    setThreads(threads: number) {
      window.lishogi.storage.set(storageKey('ceval.threads'), threads.toString());
    },
    maxThreads,
    hashSize,
    setHashSize(hash: number) {
      window.lishogi.storage.set(storageKey('ceval.hash-size'), hash.toString());
    },
    maxHashSize,
    infinite,
    supportsNnue,
    enableNnue,
    hovering,
    setHovering(sfen: Sfen, usi?: Usi) {
      hovering(
        usi
          ? {
              sfen,
              usi,
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
        const disable = window.lishogi.storage.get('ceval.disable') || cevalDisabledSentinel;
        if (disable) window.lishogi.tempStorage.set('ceval.enabled-after', disable);
        enabled(true);
      } else {
        window.lishogi.tempStorage.set('ceval.enabled-after', '');
        enabled(false);
      }
    },
    curDepth,
    effectiveMaxDepth,
    variant: opts.variant,
    isDeeper,
    goDeeper,
    canGoDeeper: () =>
      technology !== 'none' &&
      curDepth() < 99 &&
      !isDeeper() &&
      ((!infinite() && !worker?.isComputing()) || showingCloud()),
    isComputing: () => !!running && !!worker?.isComputing(),
    engineName: technology !== 'none' ? engineName(opts.variant.key, opts.initialSfen) : undefined,
    destroy: () => worker?.destroy(),
    redraw: opts.redraw,
    shouldUseYaneuraou: useYaneuraou,
    cachable: technology === 'nnue' || (!useYaneuraou && technology === 'hce'),
    analysable,
  };
}
