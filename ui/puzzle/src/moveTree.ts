import { makeNotationWithPosition } from 'common/notation';
import { initialSfen, makeSfen, parseSfen } from 'shogiops/sfen';
import { MoveOrDrop } from 'shogiops/types';
import { makeUsi, parseUsi } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';
import { Shogi } from 'shogiops/variant/shogi';
import { TreeWrapper } from 'tree';
import { scalashogiCharPair } from './util';

export function usiToTree(usis: Usi[]): Tree.Node {
  const pos = Shogi.default();
  const root: Tree.Node = {
    ply: 0,
    id: '',
    sfen: initialSfen('standard'),
    children: [],
  } as Tree.Node;
  let current = root;
  usis.forEach((usi, i) => {
    const move = parseUsi(usi)!,
      captured = pos.board.has(move.to),
      notationMove = makeNotationWithPosition(pos, move, pos.lastMoveOrDrop);
    pos.play(move);
    const nextNode = makeNode(pos, move, notationMove, captured, i + 1);
    current.children.push(nextNode);
    current = nextNode;
  });
  return root;
}

export function sfenToTree(sfen: string): Tree.Node {
  const startPly = parseInt(sfen.split(' ')[3]) - 1 ?? 0;
  return {
    ply: startPly,
    id: '',
    sfen: sfen,
    children: [],
  } as Tree.Node;
}

export function mergeSolution(root: TreeWrapper, initialPath: Tree.Path, solution: Usi[], pov: Color): void {
  const initialNode = root.nodeAtPath(initialPath),
    pos = parseSfen('standard', initialNode.sfen, false).unwrap(),
    fromPly = initialNode.ply;

  const nodes = solution.map((usi, i) => {
    const move = parseUsi(usi)!,
      captured = pos.board.has(move.to),
      notationMove = makeNotationWithPosition(pos, move, pos.lastMoveOrDrop);
    pos.play(move);
    const node = makeNode(pos, move, notationMove, captured, fromPly + i + 1);
    if ((pov == 'sente') == (node.ply % 2 == 1)) (node as any).puzzle = 'good';
    return node;
  });
  root.addNodes(nodes, initialPath);
}

const makeNode = (pos: Position, md: MoveOrDrop, notation: MoveNotation, capture: boolean, ply: number): Tree.Node => ({
  ply,
  sfen: makeSfen(pos),
  id: scalashogiCharPair(md),
  usi: makeUsi(md),
  notation: notation,
  capture: capture,
  check: pos.isCheck(),
  children: [],
});
