import { Chess } from 'chessops/chess';
import { makeSan, parseSan } from 'chessops/san';
import { INITIAL_FEN, makeFen, parseFen } from 'chessops/fen';
import { makeUci, parseUci } from 'chessops/util';
import { scalachessCharPair } from 'chessops/compat';

export function pgnToTree(pgn: San[]): Tree.Node {
  const pos = Chess.default();
  const root: Tree.Node = {
    ply: 0,
    id: '',
    fen: INITIAL_FEN,
    children: []
  } as Tree.Node;
  let current = root;
  pgn.forEach((san, i) => {
    const move = parseSan(pos, san)!;
    pos.play(move);
    const nextNode = {
      ply: i + 1,
      fen: makeFen(pos.toSetup()),
      id: scalachessCharPair(move),
      uci: makeUci(move),
      san: san,
      children: []
    };
    current.children.push(nextNode);
    current = nextNode;
  });
  console.log(root);
  return root;
}

export function mergeSolution(node: Tree.Node, solution: Uci[], color: Color): void {
  const pos = Chess.fromSetup(parseFen(node.fen).unwrap()).unwrap();
  for (const uci of solution) {
    const move = parseUci(uci)!;
    const san = makeSan(pos, move)!;
    pos.play(move);
    const nextNode = {
      ply: pos.fullmoves,
      fen: makeFen(pos.toSetup()),
      id: scalachessCharPair(move),
      uci: makeUci(move),
      san: san,
      children: []
    };
    if (color == 'white' == (pos.fullmoves % 2 == 1)) node.puzzle = 'good';
    node.children.push(nextNode);
    node = nextNode;
  };
}
