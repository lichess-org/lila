import { Shogi } from 'shogiops/shogi';
import { INITIAL_FEN, makeFen, parseFen } from 'shogiops/fen';
import { scalashogiCharPair } from 'shogiops/compat';
import { Move } from 'shogiops/types';
import { makeSquare, makeUsi, parseUsi } from 'shogiops/util';
import { TreeWrapper } from 'tree';
import { pretendItsUsi } from 'common';

export function pgnToTree(usis: Usi[]): Tree.Node {
  const pos = Shogi.default();
  const root: Tree.Node = {
    ply: 0,
    id: '',
    fen: INITIAL_FEN,
    children: [],
  } as Tree.Node;
  let current = root;
  usis.forEach((usi, i) => {
    const move = parseUsi(usi)!;
    pos.play(move);
    const nextNode = makeNode(pos, move, i + 1);
    current.children.push(nextNode);
    current = nextNode;
  });
  return root;
}

export function fenToTree(sfen: string): Tree.Node {
  const startPly = parseInt(sfen.split(' ')[3]) - 1 ?? 0;
  return {
    ply: startPly,
    id: '',
    fen: sfen,
    children: [],
  } as Tree.Node;
}

export function mergeSolution(root: TreeWrapper, initialPath: Tree.Path, solution: Usi[], pov: Color): void {
  const initialNode = root.nodeAtPath(initialPath);
  const pos = Shogi.fromSetup(parseFen(initialNode.fen).unwrap(), false).unwrap();
  const fromPly = initialNode.ply;
  const nodes = solution.map((usi, i) => {
    const move = parseUsi(pretendItsUsi(usi))!;
    pos.play(move);
    const node = makeNode(pos, move, fromPly + i + 1);
    if ((pov == 'sente') == (node.ply % 2 == 1)) (node as any).puzzle = 'good';
    return node;
  });
  root.addNodes(nodes, initialPath);
}

const makeNode = (pos: Shogi, move: Move, ply: number) => ({
  ply,
  san: '',
  fen: makeFen(pos.toSetup()),
  id: scalashogiCharPair(move),
  usi: makeUsi(move),
  check: pos.isCheck() ? makeSquare(pos.toSetup().board.kingOf(pos.turn)!) : undefined,
  children: [],
});
