var winningChances = require('ceval').winningChances;
var treePath = require('tree').path;
var winningChances = require('ceval').winningChances;
var pv2san = require('ceval').pv2san;
var m = require('mithril');

module.exports = function(root) {

  if (!root.ceval.enabled()) root.toggleCeval();

  var running = m.prop(true);
  var comment = m.prop();
  var WEAK = 16;
  var DEEP = 18;

  var hasCeval = function(node, minDepth) {
    return node.ceval && node.ceval.depth >= minDepth;
  };

  var isMyTurn = function() {
    return turnColor() === root.bottomColor();
  };

  var checkCeval = function() {
    if (!running()) return;
    var node = root.vm.node;
    if (!isMyTurn()) {
      comment(null);
      if (hasCeval(node, WEAK)) {
        var parentNode = root.tree.nodeAtPath(treePath.init(root.vm.path));
        if (hasCeval(parentNode, WEAK))
          comment(makeComment(parentNode, node, root.vm.path));
      }
      if (hasCeval(node, DEEP)) root.playUci(node.ceval.best);
      m.redraw();
    }
  };

  var makeComment = function(prev, node, path) {
    var c, shift = -winningChances.povDiff(root.bottomColor(), node.ceval, prev.ceval);
    if (shift < 0.03) c = 'best';
    else if (shift < 0.06) c = 'good';
    else if (shift < 0.1) c = 'inaccuracy';
    else if (shift < 0.2) c = 'mistake';
    else c = 'blunder';
    return {
      prev: prev,
      node: node,
      path: path,
      verdict: c,
      best: {
        uci: prev.ceval.best,
        san: pv2san(root.data.game.variant.key, prev.fen, false, prev.ceval.best)
      }
    };
  }

  var turnColor = function() {
    return root.chessground.data.movable.color;
  };

  return {
    onCeval: checkCeval,
    onJump: function() {
      // because running(false) is called after the jump
      setTimeout(checkCeval, 50)
    },
    close: root.togglePractice,
    trans: root.trans,
    turnColor: turnColor,
    isMyTurn: isMyTurn,
    comment: comment,
    running: running,
    resume: function() {
      running(true);
      checkCeval();
    },
    onUserJump: function(from, to) {
      running(false);
      comment(null);
    }
  };
};
