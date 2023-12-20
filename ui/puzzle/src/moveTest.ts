import { altCastles } from 'chess';
import { parseUci } from 'chessops/util';
import { path as pathOps } from 'tree';
import { MoveTest } from './interfaces';
import PuzzleCtrl from './ctrl';

type MoveTestReturn = undefined | 'fail' | 'win' | MoveTest;

type AltCastle = keyof typeof altCastles;

function isAltCastle(str: string): str is AltCastle {
  return str in altCastles;
}

export default function moveTest(ctrl: PuzzleCtrl): MoveTestReturn {
  if (ctrl.mode === 'view') return;
  if (!pathOps.contains(ctrl.path, ctrl.initialPath)) return;

  const playedByColor = ctrl.node.ply % 2 === 1 ? 'white' : 'black';
  if (playedByColor !== ctrl.pov) return;

  const nodes = ctrl.nodeList.slice(pathOps.size(ctrl.initialPath) + 1).map(node => ({
    uci: node.uci,
    castle: node.san!.startsWith('O-O'),
    checkmate: node.san!.endsWith('#'),
  }));

  for (const i in nodes) {
    if (nodes[i].checkmate) return (ctrl.node.puzzle = 'win');
    const uci = nodes[i].uci!,
      solUci = ctrl.data.puzzle.solution[i];
    if (uci != solUci && (!nodes[i].castle || !isAltCastle(uci) || altCastles[uci] != solUci))
      return (ctrl.node.puzzle = 'fail');
  }

  const nextUci = ctrl.data.puzzle.solution[nodes.length];
  if (!nextUci) return (ctrl.node.puzzle = 'win');

  // from here we have a next move
  ctrl.node.puzzle = 'good';

  return {
    move: parseUci(nextUci)!,
    fen: ctrl.node.fen,
    path: ctrl.path,
  };
}
