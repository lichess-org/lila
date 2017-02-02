var m = require('mithril');
var throttle = require('common').throttle;
var defined = require('common').defined;
var isEvalBetter = require('ceval').isEvalBetter;

var evalPutMinDepth = 20;
var evalPutMinNodes = 3e6;
var evalPutMaxMoves = 8;

function qualityCheck(eval) {
  return eval.depth >= evalPutMinDepth || eval.nodes > evalPutMinNodes;
}

function makeEvalPutData(eval) {
  return {
    fen: eval.fen,
    nodes: eval.nodes,
    depth: eval.depth,
    pvs: eval.pvs.map(function(pv) {
      return {
        cp: pv.cp,
        mate: pv.mate,
        moves: pv.pv.split(' ', evalPutMaxMoves).join(' ')
      };
    })
  };
}

function expandCloudEval(e) {
  if (defined(e.pvs[0].cp)) e.cp = e.pvs[0].cp;
  else e.mate = e.pvs[0].mate;
  e.pvs.forEach(function(pv) {
    pv.best = pv.pv.split(' ', 1)[0];
  });
  e.best = e.pvs[0].best;
  e.cloud = e.depth;
}

module.exports = function(opts) {
  return {
    onCeval: throttle(1000, false, function() {
      var node = opts.getNode();
      var eval = node.ceval;
      if (eval && !eval.cloud && qualityCheck(eval) && opts.canPut(node)) {
        var data = makeEvalPutData(eval);
        console.log(data, 'to cloud');
        opts.send("evalPut", data);
      }
    }),
    fetch: function(path, multiPv) {
      var node = opts.getNode();
      if ((!node.ceval || !node.ceval.cloud) && opts.canGet(node)) opts.send("evalGet", {
        fen: node.fen,
        mpv: multiPv,
        path: path
      });
    },
    onCloudEval: function(eval) {
      expandCloudEval(eval);
      var path = eval.path;
      delete eval.path;
      opts.receive(eval, path);
    }
  };
};
