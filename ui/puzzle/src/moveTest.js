var treeOps = require('tree').ops;
var pathOps = require('tree').path;
var decomposeUci = require('chess').decomposeUci;

module.exports = function(vm, puzzle) {

  return function() {

    if (vm.mode === 'view') return;
    if (!pathOps.contains(vm.path, vm.initialPath)) return;

    var playedByColor = vm.node.ply % 2 === 1 ? 'white' : 'black';
    if (playedByColor !== puzzle.color) return;

    var ucis = vm.nodeList.slice(pathOps.size(vm.initialPath) + 1).map(function(node) {
      return node.uci;
    });

    var progress = puzzle.lines;
    for (var i in ucis) {
      progress = progress[ucis[i]];
      if (!progress) return 'fail';
      if (progress === 'retry') return 'retry';
      if (progress === 'win') return 'win';
    }

    var nextKey = Object.keys(progress)[0]
    if (progress[nextKey] === 'win') return 'win';

    var opponentUci = decomposeUci(nextKey);

    var move = {
      orig: opponentUci[0],
      dest: opponentUci[1],
      promotion: opponentUci[2],
      fen: vm.node.fen,
      path: vm.path
    };
    return move;
  };
};
