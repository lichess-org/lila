var m = require('mithril');
var throttle = require('common').throttle;
var defined = require('common').defined;
var isEvalBetter = require('ceval').isEvalBetter;

var evalPutMinDepth = 20;
var evalPutMinNodes = 3e6;
var evalPutMaxMoves = 10;

function qualityCheck(eval) {
  return eval.depth >= evalPutMinDepth || eval.nodes > evalPutMinNodes;
}

// from client eval to server eval
function toPutData(eval) {
  return {
    fen: eval.fen,
    knodes: Math.round(eval.nodes / 1000),
    depth: eval.depth,
    pvs: eval.pvs.map(function(pv) {
      return {
        cp: pv.cp,
        mate: pv.mate,
        moves: pv.moves.slice(0, evalPutMaxMoves).join(' ')
      };
    })
  };
}

// from server eval to client eval
function toCeval(e) {
  var res = {
    fen: e.fen,
    nodes: e.knodes * 1000,
    depth: e.depth,
    pvs: e.pvs.map(function(from) {
      var to = {
        moves: from.moves.split(' ')
      };
      if (defined(from.cp)) to.cp = from.cp;
      else to.mate = from.mate;
      return to;
    }),
    cloud: true
  };
  if (defined(res.pvs[0].cp)) res.cp = res.pvs[0].cp;
  else res.mate = res.pvs[0].mate;
  res.cloud = true;
  return res;
}

module.exports = function(opts) {
  var fenSent = [];
  return {
    onCeval: throttle(500, false, function() {
      var node = opts.getNode();
      var eval = node.ceval;
      if (eval && !eval.cloud && qualityCheck(eval) && opts.canPut(node)) {
        opts.send("evalPut", toPutData(eval));
      }
    }),
    fetch: function(path, multiPv) {
      var node = opts.getNode();
      if ((node.ceval && node.ceval.cloud) || !opts.canGet(node)) return;
      if (fenSent.indexOf(node.fen) !== -1) return;
      fenSent.push(node.fen);
      opts.send("evalGet", {
        fen: node.fen,
        mpv: multiPv,
        path: path
      });
    },
    onCloudEval: function(serverEval) {
      opts.receive(toCeval(serverEval), serverEval.path);
    }
  };
};
