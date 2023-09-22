import { TreeWrapper } from 'tree';

export function pgnToTree(pgn: San[]): Tree.Node {
  const pos = co.Chess.default();
  const root: Tree.Node = {
    ply: 0,
    id: '',
    fen: co.fen.INITIAL_FEN,
    children: [],
  } as Tree.Node;
  let current = root;
  pgn.forEach((san, i) => {
    const move = co.san.parseSan(pos, san)!;
    pos.play(move);
    const nextNode = makeNode(pos, move, i + 1, san);
    current.children.push(nextNode);
    current = nextNode;
  });
  return root;
}

export function mergeSolution(root: TreeWrapper, initialPath: Tree.Path, solution: Uci[], pov: Color): void {
  const initialNode = root.nodeAtPath(initialPath);
  const pos = co.Chess.fromSetup(co.fen.parseFen(initialNode.fen).unwrap()).unwrap();
  const fromPly = initialNode.ply;
  const nodes = solution.map((uci, i) => {
    const move = co.variant.normalizeMove(pos, co.parseUci(uci)!);
    const san = co.san.makeSan(pos, move);
    pos.play(move);
    const node = makeNode(pos, move, fromPly + i + 1, san);
    if ((pov == 'white') == (node.ply % 2 == 1)) node.puzzle = 'good';
    return node;
  });
  root.addNodes(nodes, initialPath);
}

const makeNode = (pos: co.Chess, move: co.Move, ply: number, san: San): Tree.Node => ({
  ply,
  san,
  fen: co.fen.makeFen(pos.toSetup()),
  id: co.compat.scalachessCharPair(move),
  uci: co.makeUci(move),
  check: pos.isCheck() ? co.makeSquare(pos.toSetup().board.kingOf(pos.turn)!) : undefined,
  children: [],
});
