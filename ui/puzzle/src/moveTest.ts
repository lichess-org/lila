import { altCastles } from 'chess';
import { parseUci } from 'chessops/util';
import { path as pathOps } from 'tree';
import { Vm, Puzzle, MoveTest } from './interfaces';

type MoveTestReturn = undefined | 'fail' | 'win' | MoveTest;

type AltCastle = keyof typeof altCastles;

function isAltCastle(str: string): str is AltCastle {
  return str in altCastles;
}

export default function moveTest(vm: Vm, puzzle: Puzzle): MoveTestReturn {
  if (vm.mode === 'view') return;
  if (!pathOps.contains(vm.path, vm.initialPath)) return;

  const playedByColor = vm.node.ply % 2 === 1 ? 'white' : 'black';
  if (playedByColor !== vm.pov) return;

  const nodes = vm.nodeList.slice(pathOps.size(vm.initialPath) + 1).map(node => ({
    uci: node.uci,
    castle: node.san!.startsWith('O-O'),
    checkmate: node.san!.endsWith('#'),
  }));

  for (const i in nodes) {
    if (nodes[i].checkmate) return (vm.node.puzzle = 'win');
    const uci = nodes[i].uci!,
      solUci = puzzle.solution[i];
    if (uci != solUci && (!nodes[i].castle || !isAltCastle(uci) || altCastles[uci] != solUci))
      return (vm.node.puzzle = 'fail');
  }

  const nextUci = puzzle.solution[nodes.length];
  if (!nextUci) return (vm.node.puzzle = 'win');

  // from here we have a next move
  vm.node.puzzle = 'good';

  return {
    move: parseUci(nextUci)!,
    fen: vm.node.fen,
    path: vm.path,
  };
}
