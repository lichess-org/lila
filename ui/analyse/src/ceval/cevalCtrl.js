var m = require('mithril');
var makePool = require('./cevalPool');

var initialPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

module.exports = function(allow, emit) {

  var nbWorkers = 3;
  var minDepth = 8;
  var maxDepth = 18;
  var curDepth = 0;
  var storageKey = 'client-eval-enabled';
  var allowed = m.prop(allow);
  var enabled = m.prop(allow && lichess.storage.get(storageKey) === '1');
  var started = false;
  var pool = makePool({
    path: '/assets/vendor/stockfish6.js', // Can't CDN because same-origin policy
    minDepth: minDepth,
    maxDepth: maxDepth
  }, nbWorkers);

  var onEmit = function(res) {
    curDepth = res.eval.depth;
    emit(res);
  }

  var start = function(path, steps) {
    if (!enabled()) return;
    var step = steps[steps.length - 1];
    if (step.ceval && step.ceval.depth >= maxDepth) return;
    var work = {
      position: steps[0].fen,
      moves: steps.slice(1).map(function(s) {
        return fixCastle(s.uci, s.san);
      }).join(' '),
      path: path,
      steps: steps,
      ply: step.ply,
      emit: function(res) {
        if (enabled()) onEmit(res);
      }
    };
    if (work.position === initialPosition && !work.moves.length) return work.emit({
      work: work,
      eval: {
        depth: maxDepth,
        cp: 15, // I made stockfish work hard on this one
        mate: 0, // so far, chess isn't solved
        best: 'e2e4' // best by test
      },
      name: name
    });
    pool.start(work);
    started = true;
  };

  var stop = function() {
    if (!enabled() || !started) return;
    pool.stop();
    started = false;
  };

  var fixCastle = function(uci, san) {
    if (san.indexOf('O-O') !== 0) return uci;
    switch (uci) {
      case 'e1h1':
        return 'e1g1';
      case 'e1a1':
        return 'e1c1';
      case 'e8h8':
        return 'e8g8';
      case 'e8a8':
        return 'e8c8';
    }
    return uci;
  };

  return {
    start: start,
    stop: stop,
    allowed: allowed,
    enabled: enabled,
    toggle: function() {
      if (!allowed()) return;
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
