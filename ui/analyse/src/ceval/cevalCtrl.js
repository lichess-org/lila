var m = require('mithril');
var makePool = require('./cevalPool');
var dict = require('./cevalDict');
var util = require('../util');
var stockfishWorker = require('./stockfishWorker');
var sunsetterWorker = require('./sunsetterWorker');

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
  var engine = variant.key !== 'crazyhouse' ? stockfishWorker : sunsetterWorker;

  var pool = makePool({
    minDepth: minDepth,
    maxDepth: maxDepth,
    variant: variant
  }, engine, nbWorkers);

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
      if (values.length >= 10) {
        var depth = 18, knps = util.median(values) / 1000;
        if (knps > 100) depth = 19;
        if (knps > 150) depth = 20;
        if (knps > 200) depth = 21;
        if (knps > 250) depth = 22;
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

  var start = function(path, steps) {
    if (!enabled() || !possible()) return;
    var step = steps[steps.length - 1];
    if (step.ceval && step.ceval.depth >= maxDepth()) return;

    var work = {
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path: path,
      steps: steps,
      ply: step.ply,
      maxDepth: maxDepth(),
      emit: function(res) {
        if (enabled()) onEmit(res);
      }
    };

    // send fen after latest castling move and the following moves
    for (var i = 1; i < steps.length; i++) {
      var step = steps[i];
      if (step.san.indexOf('O-O') === 0) {
        work.moves = [];
        work.initialFen = step.fen;
      } else {
        work.moves.push(step.uci);
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
          },
          name: name
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
