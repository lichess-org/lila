import { Shogi } from 'shogiops/shogi';
import { INITIAL_SFEN, makeSfen, parseSfen } from 'shogiops/sfen';
import { scalashogiCharPair } from 'shogiops/compat';
import { Move } from 'shogiops/types';
import { makeSquare, makeUsi, parseUsi } from 'shogiops/util';
import { TreeWrapper } from 'tree';
import { pretendItsUsi } from 'common';
import { makeNotationWithPosition, Notation } from 'common/notation';

export function usiToTree(usis: Usi[], notation: Notation): Tree.Node {
  const pos = Shogi.default();
  const root: Tree.Node = {
    ply: 0,
    id: '',
    sfen: INITIAL_SFEN,
    children: [],
  } as Tree.Node;
  let current = root;
  let lastMove: Move | undefined = undefined;
  usis.forEach((usi, i) => {
    const move = parseUsi(usi)!;
    const notationMove = makeNotationWithPosition(notation, pos, move, lastMove);
    lastMove = move;
    pos.play(move);
    const nextNode = makeNode(pos, move, notationMove, i + 1);
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
  const initialNode = root.nodeAtPath(initialPath);
  const pos = Shogi.fromSetup(parseSfen(initialNode.sfen).unwrap(), false).unwrap();
  const fromPly = initialNode.ply;

  let lastMove: Move | undefined = undefined;
  const nodes = solution.map((usi, i) => {
    const move = parseUsi(pretendItsUsi(usi))!;
    const notationMove = makeNotationWithPosition(notation, pos, move, lastMove);
    lastMove = move;
    pos.play(move);
    const node = makeNode(pos, move, notationMove, fromPly + i + 1);
    if ((pov == 'sente') == (node.ply % 2 == 1)) (node as any).puzzle = 'good';
    return node;
  });
  root.addNodes(nodes, initialPath);
}

const makeNode = (pos: Shogi, move: Move, notation: MoveNotation, ply: number) => ({
  ply,
  sfen: makeSfen(pos.toSetup()),
  id: scalashogiCharPair(move),
  usi: makeUsi(move),
  notation: notation,
  check: pos.isCheck() ? makeSquare(pos.toSetup().board.kingOf(pos.turn)!) : undefined,
  children: [],
});
