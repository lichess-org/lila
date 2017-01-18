var winningChances = require('ceval').winningChances;
var treePath = require('tree').path;
var winningChances = require('ceval').winningChances;
var pv2san = require('ceval').pv2san;
var m = require('mithril');

module.exports = function(root) {

  if (!root.ceval.enabled()) root.toggleCeval();

  var running = m.prop(true);
  var comment = m.prop();
  var hovering = m.prop();
  var hinting = m.prop();

  var commentable = function(ceval) {
    return ceval && (ceval.depth >= 15 || (ceval.depth >= 14 && ceval.millis > 4000));
  };
  var playable = function(ceval) {
    return ceval && (ceval.depth >= 18 || (ceval.depth >= 16 && ceval.millis > 7000));
  };

  var isMyTurn = function() {
    return turnColor() === root.bottomColor();
  };

  var checkCeval = function() {
    if (!running()) return;
    var node = root.vm.node;
    if (isMyTurn()) {
      var h = hinting();
      if (h) {
        h.uci = node.ceval.best;
        root.setAutoShapes();
      }
    } else {
      comment(null);
      if (commentable(node.ceval)) {
        var parentNode = root.tree.nodeAtPath(treePath.init(root.vm.path));
        if (commentable(parentNode.ceval))
          comment(makeComment(parentNode, node, root.vm.path));
      }
      if (playable(node.ceval)) root.playUci(node.ceval.best);
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
      hinting(null);
      // because running(false) is called after the jump
      setTimeout(checkCeval, 50)
    },
    close: root.togglePractice,
    trans: root.trans,
    turnColor: turnColor,
    isMyTurn: isMyTurn,
    comment: comment,
    running: running,
    hovering: hovering,
    hinting: hinting,
    resume: function() {
      running(true);
      checkCeval();
    },
    onUserJump: function(from, to) {
      running(false);
      comment(null);
    },
    playCommentBest: function() {
      var c = comment();
      if (!c) return;
      root.jump(treePath.init(c.path));
      root.playUci(c.best.uci);
    },
    commentShape: function(enable) {
      var c = comment();
      if (!enable || !c) hovering(null);
      else hovering({
        uci: c.best.uci
      });
      root.setAutoShapes();
    },
    hint: function() {
      var best = root.vm.node.ceval ? root.vm.node.ceval.best : null;
      var prev = hinting();
      if (!best || (prev && prev.mode === 'move')) hinting(null);
      else hinting({
        mode: prev ? 'move' : 'piece',
        uci: best
      });
      root.setAutoShapes();
    }
  };
};
