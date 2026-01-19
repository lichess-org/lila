import { Chess, normalizeMove, Position } from 'chessops/chess';
import { INITIAL_FEN, makeFen, parseFen } from 'chessops/fen';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { makeUci, parseUci } from 'chessops/util';
import { type TreeWrapper, path as pathOps } from 'lib/tree/tree';
import { isNormal, type Move, type NormalMove } from 'chessops/types';
import type PuzzleCtrl from './ctrl';
import type { TreeNode, TreePath } from '../../lib/src/tree/types';
import { completeNode } from 'lib/tree/node';
import { Result } from '@badrap/result';

export function pgnToTree(pgn: San[]): TreeNode {
  const pos = Chess.default();
  const root: TreeNode = completeNode('standard')({
    ply: 0,
    fen: INITIAL_FEN,
  });
  let current = root;
  pgn.forEach((san, i) => {
    const move = parseSan(pos, san)!;
    pos.play(move);
    const nextNode = makeNode(pos.clone(), move, i + 1, san);
    current.children.push(nextNode);
    current = nextNode;
  });
  return root;
}

export function mergeSolution(root: TreeWrapper, initialPath: TreePath, solution: Uci[], pov: Color): void {
  const initialNode = root.nodeAtPath(initialPath);
  const pos = Chess.fromSetup(parseFen(initialNode.fen).unwrap()).unwrap();
  const fromPly = initialNode.ply;
  const nodes = solution.map((uci, i) => {
    const move = normalizeMove(pos, parseUci(uci)!);
    const san = makeSanAndPlay(pos, move);
    const node = makeNode(pos.clone(), move, fromPly + i + 1, san);
    if ((pov === 'white') === (node.ply % 2 === 1)) node.puzzle = 'good';
    return node;
  });
  root.addNodes(nodes, initialPath);
}

const makeNode = (pos: Position, move: Move, ply: number, san: San): TreeNode =>
  completeNode('standard')({
    ply,
    san,
    fen: makeFen(pos.toSetup()),
    uci: makeUci(move),
    pos: () => Result.ok(pos),
    children: [],
  });

export function nextCorrectMove(ctrl: PuzzleCtrl): NormalMove | undefined {
  if (ctrl.mode === 'view') return;
  if (!pathOps.contains(ctrl.path, ctrl.initialPath)) return;

  const playedByColor = ctrl.node.ply % 2 === 1 ? 'white' : 'black';
  if (playedByColor === ctrl.pov) return;

  const nodes = ctrl.nodeList.slice(pathOps.size(ctrl.initialPath) + 1);
  const nextUci = ctrl.data.puzzle.solution[nodes.length];
  const move = nextUci && parseUci(nextUci);

  return move && isNormal(move) ? move : undefined;
}
