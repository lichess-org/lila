import { CevalController, CevalOpts, Work, Step, Hovering, Started } from './types';

import * as m from 'mithril';
import Pool from './pool';
import { median } from './math';
import { storedProp, throttle } from 'common';
import { povChances } from './winningChances';

export default function(opts: CevalOpts): CevalController {

  var storageKey = function(k: string): string {
    return opts.storageKeyPrefix ? opts.storageKeyPrefix + '.' + k : k;
  };

  var pnaclSupported: boolean = !opts.failsafe && 'application/x-pnacl' in navigator.mimeTypes;
  var wasmSupported = !opts.failsafe && typeof WebAssembly === 'object' && WebAssembly.validate(Uint8Array.of(0x0, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00));
  var minDepth = 6;
  var maxDepth = storedProp<number>(storageKey('ceval.max-depth'), 18);
  var multiPv = storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  var threads = storedProp(storageKey('ceval.threads'), Math.ceil((navigator.hardwareConcurrency || 1) / 2));
  var hashSize = storedProp(storageKey('ceval.hash-size'), 128);
  var infinite = storedProp('ceval.infinite', false);
  var curEval: Tree.ClientEval | null = null;
  var enableStorage = window.lichess.storage.make(storageKey('client-eval-enabled'));
  var allowed = m.prop(true);
  var enabled = m.prop(opts.possible && allowed() && enableStorage.get() == '1' && !document.hidden);
  var started: Started | false = false;
  var lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  var hovering = m.prop<Hovering | null>(null);
  var isDeeper = m.prop(false);

  var pool = new Pool({
    asmjs: window.lichess.assetUrl('/assets/vendor/stockfish/stockfish.js', {sameDomain: true}),
    pnacl: pnaclSupported && window.lichess.assetUrl('/assets/vendor/stockfish/stockfish.nmf'),
    wasm: wasmSupported && window.lichess.assetUrl('/assets/vendor/stockfish/stockfish.wasm.js', {sameDomain: true}),
    onCrash: opts.onCrash
  }, {
    minDepth: minDepth,
    variant: opts.variant.key,
    threads: pnaclSupported && threads,
    hashSize: pnaclSupported && hashSize
  });

  // adjusts maxDepth based on nodes per second
  var npsRecorder = (function() {
    var values: number[] = [];
    var applies = function(ev: Tree.ClientEval) {
      return ev.knps && ev.depth >= 16 &&
        typeof ev.cp !== 'undefined' && Math.abs(ev.cp) < 500 &&
        (ev.fen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1) >= 10;
    }
    return function(ev: Tree.ClientEval) {
      if (!applies(ev)) return;
      values.push(ev.knps);
      if (values.length >= 5) {
        var depth = 18,
          knps = median(values) || 0;
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
        if (values.length > 20) values.shift();
      }
    };
  })();

  var throttledEmit = throttle(150, false, opts.emit);

  var onEmit = function(ev: Tree.ClientEval, work: Work) {
    sortPvsInPlace(ev.pvs, (work.ply % 2 === (work.threatMode ? 1 : 0)) ? 'white' : 'black');
    npsRecorder(ev);
    curEval = ev;
    throttledEmit(ev, work);
    publish(ev);
  };

  var publish = function(ev: Tree.ClientEval) {
    if (ev.depth === 12) window.lichess.storage.set('ceval.fen', ev.fen);
  };

  var effectiveMaxDepth = function(): number {
    return (isDeeper() || infinite()) ? 99 : parseInt(maxDepth());
  };

  var sortPvsInPlace = function(pvs: Tree.PvData[], color: Color) {
    pvs.sort(function(a, b) {
      return povChances(color, b) - povChances(color, a);
    });
  };

  var start = function(path: string, steps: Step[], threatMode: boolean, deeper: boolean) {

    if (!enabled() || !opts.possible) return;

    isDeeper(deeper);
    var maxD = effectiveMaxDepth();

    var step = steps[steps.length - 1];

    var existing = threatMode ? step.threat : step.ceval;
    if (existing && existing.depth >= maxD) return;

    var work: Work = {
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path: path,
      ply: step.ply,
      maxDepth: maxD,
      multiPv: parseInt(multiPv()),
      threatMode: threatMode,
      emit: function(ev: Tree.ClientEval) {
        if (enabled()) onEmit(ev, work);
      }
    };

    if (threatMode) {
      var c = step.ply % 2 === 1 ? 'w' : 'b';
      var fen = step.fen.replace(/ (w|b) /, ' ' + c + ' ');
      work.currentFen = fen;
      work.initialFen = fen;
    } else {
      // send fen after latest castling move and the following moves
      for (var i = 1; i < steps.length; i++) {
        var s = steps[i];
        if (s.san!.indexOf('O-O') === 0) {
          work.moves = [];
          work.initialFen = s.fen;
        } else work.moves.push(s.uci!);
      }
    }

    pool.start(work);

    started = {
      path: path,
      steps: steps,
      threatMode: threatMode
    };
  };

  var goDeeper = function() {
    var s = started || lastStarted;
    if (s) {
      stop();
      start(s.path, s.steps, s.threatMode, true);
    }
  };

  var stop = function() {
    if (!enabled() || !started) return;
    pool.stop();
    lastStarted = started;
    started = false;
  };

  return {
    pnaclSupported: pnaclSupported,
    wasmSupported: wasmSupported,
    start: start,
    stop: stop,
    allowed: allowed,
    possible: opts.possible,
    enabled: enabled,
    multiPv: multiPv,
    threads: threads,
    hashSize: hashSize,
    infinite: infinite,
    hovering: hovering,
    setHovering: function(fen: string, uci: string) {
      hovering(uci ? {
        fen: fen,
        uci: uci
      } : null);
      opts.setAutoShapes();
    },
    toggle: function() {
      if (!opts.possible || !allowed()) return;
      stop();
      enabled(!enabled());
      if (document.visibilityState !== 'hidden')
        enableStorage.set(enabled() ? '1' : '0');
    },
    curDepth: function() {
      return curEval ? curEval.depth : 0;
    },
    effectiveMaxDepth: effectiveMaxDepth,
    variant: opts.variant,
    isDeeper: isDeeper,
    goDeeper: goDeeper,
    canGoDeeper: function() {
      return !isDeeper() && !infinite();
    },
    isComputing: function() {
      return !!started && pool.isComputing();
    },
    destroy: pool.destroy.bind(pool),
    env: function() {
      return {
        pnacl: !!pnaclSupported,
        wasm: !!wasmSupported,
        multiPv: parseInt(multiPv()),
        threads: parseInt(threads()),
        hashSize: parseInt(hashSize()),
        maxDepth: effectiveMaxDepth()
      };
    }
  };
};
