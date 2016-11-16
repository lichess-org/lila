var m = require('mithril');
var makePool = require('./cevalPool');
var dict = require('./cevalDict');
var util = require('../util');
var stockfishProtocol = require('./stockfishProtocol');

module.exports = function(opts) {

  var pnaclSupported = !opts.failsafe && navigator.mimeTypes['application/x-pnacl'];
  var minDepth = 6;
  var maxDepth = util.storedProp('ceval.max-depth', 18);
  var multiPv = util.storedProp('ceval.multipv', 1);
  var threads = util.storedProp('ceval.threads', Math.ceil((navigator.hardwareConcurrency || 1) / 2));
  var hashSize = util.storedProp('ceval.hash-size', 128);
  var curDepth = 0;
  var enableStorage = lichess.storage.make('client-eval-enabled');
  var allowed = m.prop(true);
  var enabled = m.prop(opts.possible && allowed() && enableStorage.get() == '1');
  var started = false;
  var hoveringUci = m.prop(null);

  var pool = makePool(stockfishProtocol, {
    asmjs: '/assets/vendor/stockfish.js/stockfish.js?v=5',
    pnacl: pnaclSupported && '/assets/vendor/stockfish.pexe/nacl/stockfish.nmf?v=5',
    onCrash: opts.onCrash
  }, {
    minDepth: minDepth,
    maxDepth: maxDepth,
    variant: opts.variant,
    multiPv: multiPv,
    threads: pnaclSupported && threads,
    hashSize: pnaclSupported && hashSize
  });

  // adjusts maxDepth based on nodes per second
  var npsRecorder = (function() {
    var values = [];
    var applies = function(res) {
      return res.eval.nps && res.eval.depth >= 16 &&
        !res.eval.mate && Math.abs(res.eval.cp) < 500 &&
        (res.work.currentFen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1) >= 10;
    }
    return function(res) {
      if (!applies(res)) return;
      values.push(res.eval.nps);
      if (values.length >= 5) {
        var depth = 18,
          knps = util.median(values) / 1000;
        if (knps > 100) depth = 19;
        if (knps > 150) depth = 20;
        if (knps > 250) depth = 21;
        if (knps > 500) depth = 22;
        if (knps > 1000) depth = 23;
        if (knps > 2000) depth = 24;
        if (knps > 3500) depth = 25;
        maxDepth(depth);
        if (values.length > 20) values.shift();
      }
    };
  })();

  var onEmit = function(res) {
    res.eval.maxDepth = res.work.maxDepth;
    npsRecorder(res);
    curDepth = res.eval.depth;
    opts.emit(res);
  }

  var start = function(path, steps, threatMode) {
    if (!enabled() || !opts.possible) return;
    var step = steps[steps.length - 1];

    var existing = step[threatMode ? 'threat' : 'ceval'];
    if (existing && existing.depth >= maxDepth()) return;

    var work = {
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path: path,
      ply: step.ply,
      maxDepth: maxDepth(),
      threatMode: threatMode,
      emit: function(res) {
        if (enabled()) onEmit(res);
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
        if (s.san.indexOf('O-O') === 0) {
          work.moves = [];
          work.initialFen = s.fen;
        } else work.moves.push(s.uci);
      }
    }

    var dictRes = dict(work, opts.variant, multiPv());
    if (dictRes) {
      setTimeout(function() {
        // this has to be delayed, or it slows down analysis first render.
        work.emit({
          work: work,
          eval: {
            depth: maxDepth(),
            cp: dictRes.cp,
            best: dictRes.best,
            pvs: dictRes.pvs,
            dict: true
          }
        });
      }, 500);
      pool.warmup();
    } else pool.start(work);

    started = true;
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
    hoveringUci: hoveringUci,
    setHoveringUci: function(uci) {
      hoveringUci(uci);
      opts.setAutoShapes();
    },
    toggle: function() {
      if (!opts.possible || !allowed()) return;
      stop();
      enabled(!enabled());
      enableStorage.set(enabled() ? '1' : '0');
    },
    curDepth: function() {
      return curDepth;
    },
    maxDepth: maxDepth,
    destroy: pool.destroy
  };
};
