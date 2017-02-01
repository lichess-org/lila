var throttle = require('common').throttle;

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

module.exports = function(opts) {
  return {
    onCeval: throttle(1000, false, function() {
      var eval = opts.getNode().ceval;
      if (eval && !eval.cloud && !eval.dict && opts.canPut() && (eval.depth >= evalPutMinDepth || eval.nodes > evalPutMinNodes)) {
        console.log(eval, 'to cloud');
        opts.send("evalPut", makeEvalPutData(eval));
      }
    }),
    mutateAnaDestsReq: function(req) {
      console.log(req, opts.canGet(), opts.getCeval().enabled());
      if (opts.canGet() && opts.getCeval().enabled()) req.multiPv = parseInt(opts.getCeval().multiPv());
    },
    onDests: function(data) {
      if (data.eval) {
        data.eval.cloud = data.eval.depth;
        console.log(data.eval, 'from cloud');
      }
    }
  };
};
