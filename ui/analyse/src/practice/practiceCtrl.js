var winningChances = require('ceval').winningChances;
var treePath = require('tree').path;
var winningChances = require('ceval').winningChances;
var pv2san = require('ceval').pv2san;
var defined = require('common').defined;
var m = require('mithril');

module.exports = function(root, playableDepth) {

  var running = m.prop(true);
  var comment = m.prop();
  var hovering = m.prop();
  var hinting = m.prop();
  var played = m.prop(false);

  var ensureCevalRunnning = function() {
    if (!root.vm.showComputer()) root.toggleComputer();
    if (!root.ceval.enabled()) root.toggleCeval();
    if (root.vm.threatMode) root.toggleThreatMode();
  };

  var commentable = function(node, bonus) {
    if (root.gameOver(node)) return true;
    var ceval = node.ceval;
    return ceval && ((ceval.depth + (bonus || 0)) >= 15 || (ceval.depth >= 13 && ceval.millis > 3000));
  };
  var playable = function(node) {
    var ceval = node.ceval;
    return ceval && (
      ceval.depth >= Math.min(ceval.maxDepth, playableDepth()) ||
      (ceval.depth >= 15 && ceval.millis > 5000)
    );
  };

  var altCastles = {
    e1a1: 'e1c1',
    e1h1: 'e1g1',
    e8a8: 'e8c8',
    e8h8: 'e8g8'
  };

  var makeComment = function(prev, node, path) {
    var verdict, best;
    var over = root.gameOver(node);

    if (over === 'checkmate') verdict = 'good';
    else {
      var nodeEval = (node.threefold || over === 'draw') ? {
        cp: 0
      } : node.ceval;
      var shift = -winningChances.povDiff(root.bottomColor(), nodeEval, prev.ceval);

      best = prev.ceval.pvs[0].moves[0];
      if (best === node.uci || best === altCastles[node.uci]) best = null;

      if (!best) verdict = 'good';
      else if (shift < 0.025) verdict = 'good';
      else if (shift < 0.06) verdict = 'inaccuracy';
      else if (shift < 0.14) verdict = 'mistake';
      else verdict = 'blunder';
    }

    return {
      prev: prev,
      node: node,
      path: path,
      verdict: verdict,
      best: best ? {
        uci: best,
        san: pv2san(root.data.game.variant.key, prev.fen, false, [best])
      } : null
    };
  }

  var isMyTurn = function() {
    return root.turnColor() === root.bottomColor();
  };

  var checkCeval = function() {
    if (!running()) {
      comment(null);
      m.redraw();
      return;
    }
    ensureCevalRunnning();
    var node = root.vm.node;
    if (isMyTurn()) {
      var h = hinting();
      if (h) {
        h.uci = node.ceval.pvs[0].moves[0];
        root.setAutoShapes();
      }
    } else {
      comment(null);
      if (node.san && commentable(node)) {
        var parentNode = root.tree.nodeAtPath(treePath.init(root.vm.path));
        if (commentable(parentNode, +1))
          comment(makeComment(parentNode, node, root.vm.path));
      }
      if (!played() && playable(node)) {
        root.playUci(node.ceval.pvs[0].moves[0]);
        played(true);
      }
    }
  };

  var resume = function() {
    running(true);
    checkCeval();
  };

  var threefoldFen = function(fen) {
    return fen.split(' ').slice(0, 4).join(' ');
  };

  var detectThreefold = function() {
    var n = root.vm.node;
    if (defined(n.threefold)) return;
    var currentFen = threefoldFen(n.fen);
    var nbSimilarPositions = 0;
    for (var i in root.vm.nodeList)
      if (threefoldFen(root.vm.nodeList[i].fen) === currentFen)
        nbSimilarPositions++;
    n.threefold = nbSimilarPositions > 2;
  };

  checkCeval();

  return {
    onCeval: checkCeval,
    onJump: function() {
      played(false);
      hinting(null);
      detectThreefold();
      checkCeval();
    },
    isMyTurn: isMyTurn,
    comment: comment,
    running: running,
    hovering: hovering,
    hinting: hinting,
    resume: resume,
    playableDepth: playableDepth,
    reset: function() {
      comment(null);
      hinting(null);
    },
    preUserJump: function(from, to) {
      if (from !== to) {
        running(false);
        comment(null);
      }
    },
    postUserJump: function(from, to) {
      if (from !== to && isMyTurn()) resume();
    },
    onUserMove: function() {
      running(true);
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
      var best = root.vm.node.ceval ? root.vm.node.ceval.pvs[0].moves[0] : null;
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
