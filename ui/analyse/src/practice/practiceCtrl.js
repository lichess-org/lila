var winningChances = require('ceval').winningChances;
var treePath = require('tree').path;
var winningChances = require('ceval').winningChances;
var pv2san = require('ceval').pv2san;
var m = require('mithril');

module.exports = function(root) {

  var running = m.prop(true);
  var comment = m.prop();
  var hovering = m.prop();
  var hinting = m.prop();

  var ensureCevalRunnning = function() {
    if (!root.vm.showComputer()) root.toggleComputer();
    if (!root.ceval.enabled()) root.toggleCeval();
    if (root.vm.threatMode) root.toggleThreatMode();
  };

  var commentable = function(ceval) {
    return ceval && (ceval.depth >= 14 || (ceval.depth >= 13 && ceval.millis > 3000));
  };
  var playable = function(ceval) {
    return ceval && (ceval.depth >= 18 || (ceval.depth >= 16 && ceval.millis > 7000));
  };

  var altCastles = {
    e1a1: 'e1c1',
    e1h1: 'e1g1',
    e8a8: 'e8c8',
    e8h8: 'e8g8'
  };

  var makeComment = function(prev, node, path) {
    var c, shift = -winningChances.povDiff(root.bottomColor(), node.ceval, prev.ceval);

    var best = prev.ceval.best;
    if (best === node.uci || best === altCastles[node.uci]) best = null;

    if (!best) c = 'good';
    else if (shift < 0.025) c = 'good';
    else if (shift < 0.06) c = 'inaccuracy';
    else if (shift < 0.14) c = 'mistake';
    else c = 'blunder';

    return {
      prev: prev,
      node: node,
      path: path,
      verdict: c,
      best: best ? {
        uci: best,
        san: pv2san(root.data.game.variant.key, prev.fen, false, best)
      } : null
    };
  }

  var turnColor = function() {
    return root.vm.node.ply % 2 === 0 ? 'white' : 'black';
  };

  var isMyTurn = function() {
    return turnColor() === root.bottomColor();
  };

  var checkCeval = function() {
    if (!running() || root.gameOver()) {
      comment(null);
      m.redraw();
      return;
    }
    ensureCevalRunnning();
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
    }
  };

  checkCeval();

  return {
    onCeval: checkCeval,
    onJump: function() {
      hinting(null);
      // because running(false) is called after the jump
      setTimeout(checkCeval, 50)
    },
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
