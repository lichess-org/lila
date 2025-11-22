import { COLORS, SquareSet, type Board } from 'chessops';
import { parseFen } from 'chessops/fen';

// scalachess/.../Divider.scala

export function dividePhases(nodes: Tree.Node[]): { middle?: number; end?: number } {
  const boards: Board[] = nodes.map(node => parseFen(node.fen).unwrap().board);
  const middle = boards.findIndex(
    board =>
      150 < complexity(board) ||
      board.occupied.diff(board.pawn.union(board.king)).size() <= 10 ||
      COLORS.find(c => board.occupied.intersect(SquareSet.backrank(c)).size() < 4),
  );
  const end = boards.findIndex(board => board.occupied.diff(board.pawn.union(board.king)).size() <= 6);
  return { middle: middle === -1 ? undefined : middle, end: end === -1 ? undefined : end };
}

function complexity(board: Board) {
  let sum = 0;
  for (let rank = 0; rank < 7; rank++) {
    const rankMask = SquareSet.fromRank(rank).union(SquareSet.fromRank(rank + 1));
    for (let file = 0; file < 7; file++) {
      const mask = rankMask.intersect(SquareSet.fromFile(file).union(SquareSet.fromFile(file + 1)));
      sum += subgridScore(rank + 1, board.white.intersect(mask).size(), board.black.intersect(mask).size());
    }
  }
  return sum;
}

function subgridScore(rank: number, white: number, black: number): number {
  if (white < black) return subgridScore(8 - rank, black, white);
  if (black === 0) {
    if (white === 1) return 9 - rank; // this strange single piece case mirrors server behavior
    if (white === 2) return rank < 3 ? 0 : rank;
    if (white === 0 || rank === 1) return 0;
    return 1 + rank;
  }
  if (white === 1 && black === 1) return 5 + Math.abs(4 - rank);
  if (white === 2 && black === 1) return 3 + rank;
  if (white === 2 && black === 2) return 7;
  if (white === 3 && black === 1) return 4 + rank;
  return 0;
}
