var m = require('mithril');
var makePool = require('./cevalPool');
var dict = require('./cevalDict');
var util = require('../util');
var stockfishProtocol = require('./stockfishProtocol');
var sunsetterProtocol = require('./sunsetterProtocol');

module.exports = function(possible, variant, emit) {

  var instanceId = Math.random().toString(36).substring(2).slice(0, 4);
  var nbWorkers = 3;
  var minDepth = 7;
  var maxDepth = util.storedProp('ceval.max-depth', 18);
  var curDepth = 0;
  var storageKey = 'client-eval-enabled';
  var allowed = m.prop(true);
  var enabled = m.prop(possible() && allowed() && lichess.storage.get(storageKey) === '1');
  var started = false;

  var pool;
  if (variant.key !== 'crazyhouse') {
    pool = makePool(stockfishProtocol, {
      asmjs: '/assets/vendor/stockfish.js/stockfish.js',
      pnacl: '/assets/vendor/stockfish.pexe/nacl/stockfish.nmf'
    }, {
      minDepth: minDepth,
      maxDepth: maxDepth,
      variant: variant,
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
        if (knps > 200) depth = 21;
        if (knps > 250) depth = 22;
        if (knps > 500) depth = 23;
        if (knps > 800) depth = 24;
        if (knps > 1500) depth = 25;
        maxDepth(depth);
        if (values.length > 20) values.shift();
      }
    };
  })();

  var onEmit = function(res) {
    res.instanceId = instanceId;
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

    var dictRes = dict(work, variant);
    if (dictRes) {
      setTimeout(function() {
        // this has to be delayed, or it slows down analysis first render.
        work.emit({
          work: work,
          eval: {
            depth: maxDepth(),
            cp: dictRes.cp,
            best: dictRes.best,
            mate: 0,
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
    instanceId: instanceId,
    start: start,
    stop: stop,
    allowed: allowed,
    possible: possible,
    enabled: enabled,
    toggle: function() {
      if (!possible() || !allowed()) return;
      stop();
      enabled(!enabled());
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
    },
    curDepth: function() {
      return curDepth;
    },
    maxDepth: maxDepth
  };
};
