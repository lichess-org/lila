var throttle = require('common').throttle;
var initialBoardFen = require('chessground').fen.initial;
var defined = require('common').defined;

var evalPutMinDepth = 20;
var evalPutMinNodes = 3e6;
// var evalPutMinDepth = 16;
// var evalPutMinNodes = 1e6;
var evalPutMaxMoves = 8;

/**
 * remembers the cloud eval depths seen for each FEN
 * so that we don't try to put an eval with a depth
 * lower than what the cloud already sent us.
 */
var fenCloudDepths = {};

function makeEvalPutData(eval) {
  return {
    fen: eval.fen,
    nodes: eval.nodes,
    depth: eval.depth,
    pvs: eval.pvs.map(function(pv) {
      return {
        cp: pv.cp,
        mate: pv.mate,
        moves: pv.pv.split(' ').slice(0, evalPutMaxMoves).join(' ')
      };
    })
  };
}

function expandCloudEval(e) {
  if (defined(e.pvs[0].cp)) e.cp = e.pvs[0].cp;
  if (defined(e.pvs[0].mate)) e.mate = e.pvs[0].mate;
  e.best = e.pvs[0].best;
  e.cloud = e.depth;
  return e;
}

module.exports = function(opts) {
  return {
    onCeval: throttle(1000, false, function() {
      var node = opts.getNode();
      var eval = node.ceval;
      if (eval && !eval.cloud && opts.canPut(node) &&
        (eval.depth >= evalPutMinDepth || eval.nodes > evalPutMinNodes) &&
        (node.fen.split(' ', 1)[0] !== initialBoardFen || eval.depth >= 27)
      ) {
        var data = makeEvalPutData(eval);
        console.log(data, 'to cloud');
        opts.send("evalPut", data);
      }
    }),
    mutateAnaDestsReq: function(req) {
      if (opts.canGet(opts.getNode()) && opts.getCeval().enabled())
        req.multiPv = parseInt(opts.getCeval().multiPv());
    },
    onDests: function(data) {
      if (data.eval) {
        expandCloudEval(data.eval);
        console.log(data.eval, 'from cloud');
      }
    },
    onNode: function(data) {
      if (data.eval) {
        data.node.ceval = expandCloudEval(data.eval);
        console.log(data.eval, 'from cloud');
      }
    }
  };
};
