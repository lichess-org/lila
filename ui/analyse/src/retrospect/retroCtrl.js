var nodeFinder = require('../nodeFinder');
var makeAutoShapes = require('../autoShape').makeAutoShapesFromUci;
var winningChances = require('ceval').winningChances;
var treePath = require('tree').path;
var empty = require('common').empty;
var m = require('mithril');

module.exports = function(root) {

  var game = root.data.game;
  var color = root.bottomColor();
  var candidateNodes = [];
  var explorerCancelPlies = [];
  var solvedPlies = [];
  var current = m.prop();
  var feedback = m.prop('find'); // find | eval | win | fail | view

  var isPlySolved = function(ply) {
    return $.fp.contains(solvedPlies, ply)
  };

  var findNextNode = function() {
    var colorModulo = root.bottomColor() === 'white' ? 1 : 0;
    candidateNodes = nodeFinder.evalSwings(root.vm.mainline).filter(function(n) {
      return n.ply % 2 === colorModulo && !$.fp.contains(explorerCancelPlies, n.ply);
    });
    return candidateNodes.filter(function(n) {
      return !isPlySolved(n.ply);
    })[0];
  };

  var jumpToNext = function() {
    feedback('find');
    var node = findNextNode();
    if (!node) {
      current(null);
      return;
    }
    var fault = {
      node: node,
      path: root.mainlinePathToPly(node.ply)
    };
    var prevPath = treePath.init(fault.path);
    var prev = {
      node: root.tree.nodeAtPath(prevPath),
      path: prevPath
    };
    var solutionNode = prev.node.children.filter(function(n) {
      return n.comp;
    })[0];
    current({
      fault: fault,
      prev: prev,
      solution: {
        node: solutionNode,
        path: prevPath + solutionNode.id
      },
      openingUcis: []
    });
    // fetch opening explorer moves
    if (!game.division.middle || fault.node.ply < game.division.middle) {
      root.explorer.fetchOpening(prev.node.fen).then(function(res) {
        var cur = current();
        var ucis = [];
        res.moves.forEach(function(m) {
          if (m.white + m.draws + m.black > 1) ucis.push(m.uci);
        });
        if (ucis.filter(function(uci) {
          return fault.node.uci === uci;
        })[0]) {
          explorerCancelPlies.push(fault.node.ply);
          setTimeout(jumpToNext, 100);
        } else {
          cur.openingUcis = ucis;
          current(cur);
        }
      });
    }
    root.userJump(prev.path);
    m.redraw();
  };

  var trustedEval = function(node) {
    if (node.eval) return node.eval;
    if (node.ceval && node.ceval.depth >= 16) return node.ceval;
  };

  var onJump = function() {
    var node = root.vm.node;
    var fb = feedback();
    var cur = current();
    if (!cur) return;
    if (fb === 'eval' && cur.fault.node.ply !== node.ply) {
      feedback('find');
      root.setAutoShapes();
      return;
    }
    if (isSolving() && cur.fault.node.ply === node.ply) {
      if (cur.openingUcis.filter(function(uci) {
        return node.uci === uci;
      })[0]) onWin(); // found in opening explorer
      else if (node.comp) onWin(); // the computer solution line
      else if (node.eval) onFail(); // the move that was played in the game
      else {
        feedback('eval');
        if (!root.ceval.enabled()) root.toggleCeval();
        checkCeval();
      }
    }
    root.setAutoShapes();
  };

  var checkCeval = function() {
    var node = root.vm.node,
      cur = current();
    if (!cur || feedback() !== 'eval' || cur.fault.node.ply !== node.ply) return;
    if (node.ceval && node.ceval.depth >= 16) {
      var diff = winningChances.povDiff(color, node.ceval, cur.prev.node.eval);
      if (diff > -0.03) onWin();
      else onFail();
    }
  };

  var onWin = function() {
    solveCurrent();
    feedback('win');
    m.redraw();
  };

  var onFail = function() {
    feedback('fail');
    var bad = {
      node: root.vm.node,
      path: root.vm.path
    };
    root.userJump(current().prev.path);
    if (!root.tree.pathIsMainline(bad.path) && empty(bad.node.children))
      root.tree.deleteNodeAt(bad.path);
    m.redraw();
  };

  var viewSolution = function() {
    feedback('view');
    root.userJump(current().solution.path);
    solveCurrent();
  };

  var skip = function() {
    solveCurrent();
    jumpToNext();
  };

  var solveCurrent = function() {
    solvedPlies.push(current().fault.node.ply);
  };

  var hideComputerLine = function(node, parentPath) {
    return (node.ply % 2 === 0) !== (color === 'white') &&
      !isPlySolved(node.ply);
  };
  var showBadNode = function() {
    var cur = current();
    if (cur && isSolving() && cur.prev.path === root.vm.path) return cur.fault.node;
  };

  var isSolving = function() {
    var fb = feedback();
    return fb === 'find' || fb === 'fail';
  };

  jumpToNext();

  var onMergeAnalysisData = function() {
    if (isSolving() && !current()) jumpToNext();
  };

  return {
    current: current,
    color: color,
    isPlySolved: isPlySolved,
    onJump: onJump,
    jumpToNext: jumpToNext,
    skip: skip,
    viewSolution: viewSolution,
    hideComputerLine: hideComputerLine,
    showBadNode: showBadNode,
    onCeval: checkCeval,
    onMergeAnalysisData: onMergeAnalysisData,
    feedback: feedback,
    isSolving: isSolving,
    completion: function() {
      return [solvedPlies.length, candidateNodes.length];
    },
    reset: function() {
      solvedPlies = [];
      jumpToNext();
    },
    close: root.toggleRetro,
    trans: root.trans
  };
};
