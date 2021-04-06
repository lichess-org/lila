import { Shogi } from 'shogiops/shogi';
import { INITIAL_FEN, makeFen, parseFen } from 'shogiops/fen';
import { makeSan, parseSan } from 'shogiops/san';
import { scalashogiCharPair, parseLishogiUci, makeLishogiUci, makeChessSquare, makeShogiFen, makeLishogiFen } from 'shogiops/compat';
import { TreeWrapper } from 'tree';
import { Move } from 'shogiops/types';

export function pgnToTree(pgn: San[]): Tree.Node {
  const pos = Shogi.default();
  const root: Tree.Node = {
    ply: 0,
    id: '',
    fen: INITIAL_FEN,
    children: [],
  } as Tree.Node;
  let current = root;
  pgn.forEach((san, i) => {
    const move = parseSan(pos, san)!;
    pos.play(move);
    const nextNode = makeNode(pos, move, i + 1, san);
    current.children.push(nextNode);
    current = nextNode;
  });
  return root;
}

export function fenToTree(sfen: string): Tree.Node {
  return {
    ply: 0,
    id: '',
    fen: sfen,
    children: [],
  } as Tree.Node;
}

export function mergeSolution(root: TreeWrapper, initialPath: Tree.Path, solution: Uci[], pov: Color): void {
  const initialNode = root.nodeAtPath(initialPath);
  const pos = Shogi.fromSetup(parseFen(makeShogiFen(initialNode.fen)).unwrap(), false).unwrap();
  const fromPly = initialNode.ply;
  const nodes = solution.map((uci, i) => {
    console.log("mergeSolution: ", uci)
    const move = parseLishogiUci(uci)!;
    console.log(move);
    const san = makeSan(pos, move);
    console.log(san);
    pos.play(move);
    const node = makeNode(pos, move, fromPly + i + 1, san);
    if ((pov == 'white') == (node.ply % 2 == 1)) (node as any).puzzle = 'good';
    return node;
  });
  root.addNodes(nodes, initialPath);
}

const makeNode = (pos: Shogi, move: Move, ply: number, san: San) => ({
  ply,
  san,
  fen: makeLishogiFen(makeFen(pos.toSetup())),
  id: scalashogiCharPair(move),
  uci: makeLishogiUci(move),
  check: pos.isCheck() ? makeChessSquare(pos.toSetup().board.kingOf(pos.turn)!) : undefined,
  children: [],
});
