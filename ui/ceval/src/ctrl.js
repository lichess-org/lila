var m = require('mithril');
var makePool = require('./pool');
var median = require('./math').median;
var storedProp = require('common').storedProp;
var throttle = require('common').throttle;
var stockfishProtocol = require('./stockfishProtocol');

module.exports = function(opts) {

  var storageKey = function(k) {
    return opts.storageKeyPrefix ? opts.storageKeyPrefix + '.' + k : k;
  };

  var pnaclSupported = !opts.failsafe && navigator.mimeTypes['application/x-pnacl'];
  var minDepth = 6;
  var maxDepth = storedProp(storageKey('ceval.max-depth'), 18);
  var multiPv = storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  var threads = storedProp(storageKey('ceval.threads'), Math.ceil((navigator.hardwareConcurrency || 1) / 2));
  var hashSize = storedProp(storageKey('ceval.hash-size'), 128);
  var infinite = storedProp('ceval.infinite', false);
  var curEval = null;
  var enableStorage = lichess.storage.make(storageKey('client-eval-enabled'));
  var allowed = m.prop(true);
  var enabled = m.prop(opts.possible && allowed() && enableStorage.get() == '1' && !document.hidden);
  var started = false;
  var hovering = m.prop(null);
  var isDeeper = m.prop(false);

  var pool = makePool(stockfishProtocol, {
    asmjs: '/assets/vendor/stockfish.js/stockfish.js?v=9',
    pnacl: pnaclSupported && '/assets/vendor/stockfish.pexe/nacl/stockfish.nmf?v=9',
    onCrash: opts.onCrash
  }, {
    minDepth: minDepth,
    variant: opts.variant,
    threads: pnaclSupported && threads,
    hashSize: pnaclSupported && hashSize
  });

  // adjusts maxDepth based on nodes per second
  var npsRecorder = (function() {
    var values = [];
    var applies = function(eval) {
      return eval.knps && eval.depth >= 16 &&
        !eval.mate && Math.abs(eval.cp) < 500 &&
        (eval.fen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1) >= 10;
    }
    return function(eval) {
      if (!applies(eval)) return;
      values.push(eval.knps);
      if (values.length >= 5) {
        var depth = 18,
          knps = median(values);
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

  var onEmit = function(eval, work) {
    npsRecorder(eval);
    curEval = eval;
    throttledEmit(eval, work);
    publish(eval);
  };

  var publish = function(eval) {
    if (eval.depth === 12) lichess.storage.set('ceval.fen', eval.fen);
  };

  var effectiveMaxDepth = function() {
    return (isDeeper() || infinite()) ? 99 : maxDepth();
  };

  var start = function(path, steps, threatMode, deeper) {

    if (!enabled() || !opts.possible) return;

    isDeeper(deeper);
    var maxD = effectiveMaxDepth();

    var step = steps[steps.length - 1];

    var existing = step[threatMode ? 'threat' : 'ceval'];
    if (existing && existing.depth >= maxD) return;

    var work = {
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path: path,
      ply: step.ply,
      maxDepth: maxD,
      multiPv: parseInt(multiPv()),
      threatMode: threatMode,
    };
    work.emit = function(eval) {
      if (enabled()) onEmit(eval, work);
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
        if (s.san.indexOf('O-O') === 0) {
          work.moves = [];
          work.initialFen = s.fen;
        } else work.moves.push(s.uci);
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
    var s = started;
    if (s) {
      stop();
      start(s.path, s.steps, s.threatMode, true);
    }
  };

  var stop = function() {
    if (!enabled() || !started) return;
    pool.stop();
    started = false;
  };

  return {
    pnaclSupported: pnaclSupported,
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
    setHovering: function(fen, uci) {
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
      return pnaclSupported && !isDeeper() && !infinite();
    },
    destroy: pool.destroy
  };
};
