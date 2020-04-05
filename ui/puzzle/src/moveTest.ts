import { path as pathOps } from 'tree';
import { decomposeUci } from 'draughts';

export default function (vm, puzzle) {
  return function () {

  if (vm.mode === 'view') return;
  if (!pathOps.contains(vm.path, vm.initialPath)) return;

  var playedByColor = vm.node.ply % 2 === 1 ? 'white' : 'black';
  if (playedByColor !== puzzle.color) return;

  const ucis = new Array<string>();
  vm.nodeList.slice(pathOps.size(vm.initialPath) + 1).forEach(function (node: Tree.Node) {
    if (node.mergedNodes && node.mergedNodes.length !== 0) {
      for (var i = 0; i < node.mergedNodes.length; i++) {
          const isUci = node.mergedNodes[i].uci;
          if (isUci) ucis.push(isUci);
      }
  } else if (node.uci)
      ucis.push(node.uci);
  });

  var progress = puzzle.lines;
  for (var i in ucis) {
    let uci = ucis[i];
    if (uci.length > 4) {
      do {
        progress = progress[uci.slice(0, 4)];
        uci = uci.slice(2);
      } while (progress && uci.length >= 4);
    } else progress = progress[uci];
    if (!progress) progress = 'fail';
    if (typeof progress === 'string') break;
  }
  if (typeof progress === 'string') {
    vm.node.puzzle = progress;
    return progress;
  }

  const nextKey = Object.keys(progress)[0]
  if (progress[nextKey] === 'win') {
    vm.node.puzzle = 'win';
    return 'win';
  }

  // from here we have a next move

  const actualColor = (vm.node.displayPly ? vm.node.displayPly : vm.node.ply) % 2 === 1 ? 'white' : 'black';
  if (actualColor === puzzle.color)
    vm.node.puzzle = 'good';

  const opponentUci = decomposeUci(nextKey);
  return {
    orig: opponentUci[0],
    dest: opponentUci[1],
    variant: puzzle.variant.key,
    fen: vm.node.fen,
    path: vm.path
  };
};
}
