import { TablebaseMoveStats } from './interfaces';

export function winner(fen: Fen, move: TablebaseMoveStats): Color | undefined {
  const stm = fen.split(' ')[1];
  if ((stm[0] == 'w' && move.wdl! < 0) || (stm[0] == 'b' && move.wdl! > 0))
    return 'white';
  if ((stm[0] == 'b' && move.wdl! < 0) || (stm[0] == 'w' && move.wdl! > 0))
    return 'black';
}
