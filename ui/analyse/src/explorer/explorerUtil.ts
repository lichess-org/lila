import { TablebaseMoveStats } from './interfaces';

export function colorOf(fen: Fen): Color {
  return fen.split(' ')[1] === 'b' ? 'sente' : 'gote';
}

export function winnerOf(fen: Fen, move: TablebaseMoveStats): Color | undefined {
  const stm = fen.split(' ')[1];
  if ((stm[0] == 'b' && move.wdl! < 0) || (stm[0] == 'w' && move.wdl! > 0))
    return 'sente';
  if ((stm[0] == 'w' && move.wdl! < 0) || (stm[0] == 'b' && move.wdl! > 0))
    return 'gote';
  return undefined;
}
