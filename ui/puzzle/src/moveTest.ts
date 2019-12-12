import { path as pathOps } from 'tree';
import { decomposeUci, sanToRole } from 'chess';

const altCastles = {
  e1a1: 'e1c1',
  e1h1: 'e1g1',
  e8a8: 'e8c8',
  e8h8: 'e8g8'
};

export default function(vm, puzzle) {

  return function() {

    if (vm.mode === 'view') return;
    if (!pathOps.contains(vm.path, vm.initialPath)) return;

    var playedByColor = vm.node.ply % 2 === 1 ? 'white' : 'black';
    if (playedByColor !== puzzle.color) return;

    var nodes = vm.nodeList.slice(pathOps.size(vm.initialPath) + 1).map(function(node) {
      return {
        uci: node.uci,
        castle: node.san.startsWith('O-O')
      };
    });

    var progress = puzzle.lines;
    for (var i in nodes) {
      if (progress[nodes[i].uci]) progress = progress[nodes[i].uci];
      else if (nodes[i].castle) progress = progress[altCastles[nodes[i].uci]] || 'fail';
      else progress = 'fail';
      if (typeof progress === 'string') break;
    }
    if (typeof progress === 'string') {
      vm.node.puzzle = progress;
      return progress;
    }

    var nextKey = Object.keys(progress)[0]
      if (progress[nextKey] === 'win') {
        vm.node.puzzle = 'win';
        return 'win';
      }

      // from here we have a next move

      vm.node.puzzle = 'good';

    var opponentUci = decomposeUci(nextKey);
    var promotion = opponentUci[2] ? sanToRole[opponentUci[2].toUpperCase()] : null;

    var move: any = {
      orig: opponentUci[0],
      dest: opponentUci[1],
      fen: vm.node.fen,
      path: vm.path
    };
    if (promotion) move.promotion = promotion;

    return move;
  };
}
