import { CevalCtrl, CevalOpts, Work, Step, Hovering, Started } from './types';
import { parseVariant } from './scanProtocol';

import Pool from './pool';
import { prop } from 'common';
import { storedProp } from 'common/storage';
import throttle from 'common/throttle';
import { povChances } from './winningChances';

const li = window.lidraughts;

function median(values: number[]): number {
  values.sort((a, b) => a - b);
  const half = Math.floor(values.length / 2);
  return values.length % 2 ? values[half] : (values[half - 1] + values[half]) / 2.0;
}

export default function(opts: CevalOpts): CevalCtrl {

  const storageKey = function(k: string): string {
    return opts.storageKeyPrefix ? opts.storageKeyPrefix + '.' + k : k;
  };

  const pnaclSupported: boolean = false; // Disabled until stability issues are resolved !opts.failsafe && 'application/x-pnacl' in navigator.mimeTypes;
  const wasmSupported = typeof WebAssembly === 'object' && WebAssembly.validate(Uint8Array.of(0x0, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00));
  const minDepth = 6;
  const maxDepth = storedProp<number>(storageKey('ceval.max-depth'), 16);
  const multiPv = prop('1'); //storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  const threads = storedProp(storageKey('ceval.threads'), Math.ceil((navigator.hardwareConcurrency || 1) / 2));
  const hashSize = storedProp(storageKey('ceval.hash-size'), 128);
  const infinite = storedProp('ceval.infinite', false);
  let curEval: Tree.ClientEval | null = null;
  const enableStorage = li.storage.makeBoolean(storageKey('client-eval-enabled'));
  const allowed = prop(true);
  const enabled = prop(opts.possible && allowed() && enableStorage.get() && !document.hidden);
  let started: Started | false = false;
  let lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  const hovering = prop<Hovering | null>(null);
  const isDeeper = prop(false);

  const scanVariant = parseVariant(opts.variant.key);
  const scanPath = 'vendor/scan/scan';
  const pool = new Pool({
    asmjs: li.assetUrl(scanPath + '_' + scanVariant + '.js', {sameDomain: true}),
    pnacl: pnaclSupported && li.assetUrl(scanPath + '.nmf'),
    wasm: wasmSupported && li.assetUrl(scanPath + '_' + scanVariant + '.wasm.js', {sameDomain: true}),
    onCrash: opts.onCrash
  }, {
    minDepth,
    variant: opts.variant.key,
    threads: pnaclSupported && threads,
    hashSize: pnaclSupported && hashSize
  });

  // adjusts maxDepth based on nodes per second
  const npsRecorder = (function() {
    const values: number[] = [];
    const applies = function(ev: Tree.ClientEval) {
      return ev.knps && ev.depth >= 12 &&
        typeof ev.cp !== 'undefined' && Math.abs(ev.cp) < 500 &&
        (ev.fen.split(',').length - 1) >= 10;
    }
    return function (ev: Tree.ClientEval) {
      if (!applies(ev)) return;
      values.push(ev.knps);
      if (values.length > 9) {
        let depth = 16,
          knps = median(values) || 0;
        if (knps > 150) depth = 17;
        if (knps > 250) depth = 18;
        if (knps > 500) depth = 19;
        if (knps > 1000) depth = 20;
        if (knps > 2000) depth = 21;
        if (knps > 3000) depth = 22;
        if (knps > 5000) depth = 23;
        if (knps > 8000) depth = 24;
        if (knps > 11000) depth = 25;
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
      li.storage.set('ceval.fen', ev.fen);
    }
  });

  const effectiveMaxDepth = () => (isDeeper() || infinite()) ? 99 : (parseInt(maxDepth()) - (opts.variant.key === 'antidraughts' ? 10 : 0));

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
      const c = step.ply % 2 === 1 ? 'W' : 'B';
      const fen = c + step.fen.slice(1);
      work.currentFen = fen;
      work.initialFen = fen;
    } else {
      // send fen after last capture and the following moves
      for (let i = 1; i < steps.length; i++) {
        let s = steps[i];
        if (s.san!.includes('x')) {
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
  };

  function stop() {
    if (!enabled() || !started) return;
    pool.stop();
    lastStarted = started;
    started = false;
  };

  // ask other tabs if a game is in progress
  if (enabled()) {
    li.storage.set('ceval.fen', 'start:' + Math.random());
    li.storage.make('round.ongoing').listen(_ => {
      enabled(false);
      opts.redraw();
    });
  }

  return {
    pnaclSupported,
    wasmSupported,
    start,
    stop,
    allowed,
    possible: opts.possible,
    enabled,
    multiPv,
    threads,
    hashSize,
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
};
