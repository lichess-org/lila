import { VoiceMove } from './interfaces';
import { Role } from 'chessground/types';
import { Board, squareDist } from 'chess';

export const src = (uci: Uci) => uci.slice(0, 2) as Key;
export const dest = (uci: Uci) => uci.slice(2, 4) as Key;
export const promo = (uci: Uci) =>
  ({
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen',
    K: 'king',
  }[uci.slice(4, 5).toUpperCase()] as Role);

export function movesTo(s: number, role: string, board: Board): number[] {
  const deltas = (d: number[], s = 0) => d.flatMap(x => [s - x, s + x]);

  if (role === 'K') return deltas([1, 7, 8, 9], s).filter(o => o >= 0 && o < 64 && squareDist(s, o) === 1);
  else if (role === 'N') return deltas([6, 10, 15, 17], s).filter(o => o >= 0 && o < 64 && squareDist(s, o) <= 2);
  const dests: number[] = [];
  for (const delta of deltas(role === 'Q' ? [1, 7, 8, 9] : role === 'R' ? [1, 8] : role === 'B' ? [7, 9] : [])) {
    for (
      let square = s + delta;
      square >= 0 && square < 64 && squareDist(square, square - delta) === 1;
      square += delta
    ) {
      dests.push(square);
      if (board.pieces[square]) break;
    }
  }
  return dests;
}

export function nonMoveCommand(command: string, ctrl: VoiceMove): boolean {
  if (!commands.includes(command)) {
    return command.length > 0 && commands.some(c => c.startsWith(command.toLowerCase()));
  }
  commandFunctions[command](ctrl);
  return true;
}

const commandFunctions: Record<string, (ctrl?: VoiceMove) => void> = {
  draw: (ctrl: VoiceMove) => ctrl.root.offerDraw?.(true, true),
  resign: (ctrl: VoiceMove) => ctrl.root.resign?.(true, true),
  next: (ctrl: VoiceMove) => ctrl.root.next?.(),
  takeback: (ctrl: VoiceMove) => ctrl.root.takebackYes?.(),
  upv: (ctrl: VoiceMove) => ctrl.root.vote?.(true),
  downv: (ctrl: VoiceMove) => ctrl.root.vote?.(false),
  help: (ctrl: VoiceMove) => ctrl.modalOpen(true),
};
const commands = Object.keys(commandFunctions);
