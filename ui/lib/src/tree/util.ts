import * as co from 'chessops';

import type { TreeNodeBase } from './types';

export function ucisToNodes(
  ucis: Uci[],
  initial: co.Position,
  ply: number = initial.halfmoves,
): TreeNodeBase[] {
  const nodes: TreeNodeBase[] = [];
  const position = initial.clone();
  for (const [i, uci] of ucis.entries()) {
    const move = co.parseUci(uci)!;
    const san = co.san.makeSanAndPlay(position, move);
    nodes.push({
      id: co.compat.scalachessCharPair(move),
      ply: ply + i,
      fen: co.fen.makeFen(position.toSetup()),
      san,
      uci,
      children: [],
    });
    if (i > 0) nodes[i - 1].children?.push(nodes[i]);
  }
  return nodes;
}

export function pgnToNodes(pgn: string, initial?: co.Position, ply?: number): TreeNodeBase[] {
  const nodes: TreeNodeBase[] = [];
  const game = co.pgn.parsePgn(pgn)[0];
  const position = initial ?? co.pgn.startingPosition(game.headers).unwrap();
  ply ??= position.halfmoves;
  for (const [i, pgnNode] of [...game.moves.mainline()].entries()) {
    const move = co.san.parseSan(position, pgnNode.san)!;
    position.play(move);
    nodes.push({
      id: co.compat.scalachessCharPair(move),
      ply: ply + i,
      fen: co.fen.makeFen(position.toSetup()),
      san: pgnNode.san,
      uci: co.makeUci(move),
      children: [],
    });
    if (i > 0) nodes[i - 1].children?.push(nodes[i]);
  }
  return nodes;
}

export function signEval(node: TreeNodeBase): EvalScore {
  const signedEv = Object.assign({}, node.eval);
  if (node.ply % 2 === 1) {
    if (signedEv.cp) signedEv.cp *= -1;
    if (signedEv.mate) signedEv.mate *= -1;
  }
  return signedEv;
}
