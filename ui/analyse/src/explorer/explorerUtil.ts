import { TablebaseMoveStats } from './interfaces';

export function colorOf(sfen: Sfen): Color {
  return sfen.split(' ')[1] === 'b' ? 'sente' : 'gote';
}

export function winnerOf(sfen: Sfen, move: TablebaseMoveStats): Color | undefined {
  const stm = sfen.split(' ')[1];
  if ((stm[0] == 'b' && move.wdl! < 0) || (stm[0] == 'w' && move.wdl! > 0)) return 'sente';
  if ((stm[0] == 'w' && move.wdl! < 0) || (stm[0] == 'b' && move.wdl! > 0)) return 'gote';
  return undefined;
}
