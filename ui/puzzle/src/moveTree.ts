import { Position, Shogi } from 'shogiops/shogi';
import { initialSfen, makeSfen, parseSfen } from 'shogiops/sfen';
import { Move } from 'shogiops/types';
import { makeSquare, makeUsi, parseUsi } from 'shogiops/util';
import { TreeWrapper } from 'tree';
import { makeNotationWithPosition, Notation } from 'common/notation';
import { scalashogiCharPair } from './util';

export function usiToTree(usis: Usi[], notation: Notation): Tree.Node {
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
      notationMove = makeNotationWithPosition(notation, pos, move, pos.lastMove);
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

export function mergeSolution(
  root: TreeWrapper,
  initialPath: Tree.Path,
  solution: Usi[],
  pov: Color,
  notation: Notation
): void {
  const initialNode = root.nodeAtPath(initialPath),
    pos = parseSfen('standard', initialNode.sfen, false).unwrap(),
    fromPly = initialNode.ply;

  const nodes = solution.map((usi, i) => {
    const move = parseUsi(usi)!,
      captured = pos.board.has(move.to),
      notationMove = makeNotationWithPosition(notation, pos, move, pos.lastMove);
    pos.play(move);
    const node = makeNode(pos, move, notationMove, captured, fromPly + i + 1);
    if ((pov == 'sente') == (node.ply % 2 == 1)) (node as any).puzzle = 'good';
    return node;
  });
  root.addNodes(nodes, initialPath);
}

const makeNode = (pos: Position, move: Move, notation: MoveNotation, capture: boolean, ply: number): Tree.Node => ({
  ply,
  sfen: makeSfen(pos),
  id: scalashogiCharPair(move),
  usi: makeUsi(move),
  notation: notation,
  capture: capture,
  check: pos.isCheck() ? (makeSquare(pos.board.kingOf(pos.turn)!) as Key) : undefined,
  children: [],
});
