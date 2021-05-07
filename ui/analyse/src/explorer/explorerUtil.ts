import { TablebaseMoveStats } from './interfaces';
import { opposite } from 'chessops/util';

export function colorOf(fen: Fen): Color {
  return fen.split(' ')[1] === 'w' ? 'white' : 'black';
}

export function winnerOf(fen: Fen, move: TablebaseMoveStats): Color | undefined {
  const stm = colorOf(fen);
  if (move.checkmate || move.variant_loss || move.wdl! < 0) return stm;
  if (move.variant_win || move.wdl! > 0) return opposite(stm);
  return undefined;
}
