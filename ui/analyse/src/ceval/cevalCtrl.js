var m = require('mithril');
var makePool = require('./cevalPool');
var dict = require('./cevalDict');
var util = require('../util');
var stockfishProtocol = require('./stockfishProtocol');
var sunsetterProtocol = require('./sunsetterProtocol');

module.exports = function(root, possible, variant, emit) {

  var pnaclSupported = true || navigator.mimeTypes['application/x-pnacl'];
  var minDepth = 6;
  var maxDepth = util.storedProp('ceval.max-depth', 18);
  var multiPv = util.storedProp('ceval.multipv', 1);
  var threads = util.storedProp('ceval.threads', Math.ceil((navigator.hardwareConcurrency || 1) / 2));
  var hashSize = util.storedProp('ceval.hash-size', 128);
  var showPvs = util.storedProp('ceval.show-pvs', false);
  var curDepth = 0;
  var enableStorage = lichess.storage.make('client-eval-enabled');
  var allowed = m.prop(true);
  var enabled = m.prop(possible() && allowed() && enableStorage.get() == '1');
  var started = false;
  var hoveringUci = m.prop(null);

  var pool;
  if (variant.key !== 'crazyhouse') {
    pool = makePool(stockfishProtocol, {
      asmjs: '/assets/vendor/stockfish.js/stockfish.js',
      pnacl: pnaclSupported && '/assets/vendor/stockfish.pexe/nacl/stockfish.nmf',
      onCrash: function() {
        root.instanciateCeval();
      }
    }, {
      minDepth: minDepth,
      maxDepth: maxDepth,
      variant: variant,
      multiPv: multiPv,
      threads: pnaclSupported && threads,
      hashSize: pnaclSupported && hashSize
    });
  } else {
    pool = makePool(sunsetterProtocol, {
      asmjs: '/assets/vendor/Sunsetter/sunsetter.js'
    });
  }

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
    emit(res);
  }

  var start = function(path, steps, threatMode) {
    if (!enabled() || !possible()) return;
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
        var step = steps[i];
        if (step.san.indexOf('O-O') === 0) {
          work.moves = [];
          work.initialFen = step.fen;
        } else work.moves.push(step.uci);
      }
    }

    var dictRes = dict(work, variant, multiPv());
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
    possible: possible,
    enabled: enabled,
    multiPv: multiPv,
    threads: threads,
    hashSize: hashSize,
    showPvs: showPvs,
    hoveringUci: hoveringUci,
    setHoveringUci: function(uci) {
      hoveringUci(uci);
      root.setAutoShapes();
    },
    toggle: function() {
      if (!possible() || !allowed()) return;
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
