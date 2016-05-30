var m = require('mithril');
var makePool = require('./cevalPool');
var initialFen = require('../util').initialFen;

module.exports = function(possible, variant, emit) {

  var nbWorkers = 3;
  var minDepth = 8;
  var maxDepth = 18;
  var curDepth = 0;
  var storageKey = 'client-eval-enabled';
  var allowed = m.prop(true);
  var enabled = m.prop(possible() && allowed() && lichess.storage.get(storageKey) === '1');
  var started = false;
  var pool = makePool({
    path: '/assets/vendor/stockfish7.js', // Can't CDN because same-origin policy
    minDepth: minDepth,
    maxDepth: maxDepth,
    variant: variant
  }, nbWorkers);

  var onEmit = function(res) {
    curDepth = res.eval.depth;
    emit(res);
  }

  var start = function(path, steps) {
    if (!enabled() || !possible()) return;
    var step = steps[steps.length - 1];
    if (step.ceval && step.ceval.depth >= maxDepth) return;

    var work = {
      position: steps[0].fen,
      moves: [],
      path: path,
      steps: steps,
      ply: step.ply,
      emit: function(res) {
        if (enabled()) onEmit(res);
      }
    };

    // send fen after latest castling move and the following moves
    for (var i = 1; i < steps.length; i++) {
      var step = steps[i];
      if (step.san.indexOf('O-O') === 0) {
        work.moves = [];
        work.position = step.fen;
      } else {
        work.moves.push(step.uci);
      }
    }

    if (work.position === initialFen && !work.moves.length) {
      setTimeout(function() {
        // this has to be delayed, or it slows down analysis first render.
        work.emit({
          work: work,
          eval: {
            depth: maxDepth,
            cp: 15, // I made stockfish work hard on this one
            mate: 0, // so far, chess isn't solved
            best: 'e2e4' // best by test
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
