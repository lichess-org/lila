import { Move } from 'chessops';
import { Board } from './chess';

export const scheduleBotMove = (board: Board, move: (m: Move) => void): void => {
  setTimeout(() => move(selectMove(board)), 500);
};

const selectMove = (board: Board): Move => randomMove(board);

const randomMove = (board: Board): Move => {
  const dests = board.chess.allDests();
  const moves = Array.from(dests.entries()).flatMap(([from, tos]) =>
    Array.from(tos).map(to => ({ from, to })),
  );
  const move = moves[Math.floor(Math.random() * moves.length)];
  return move;
};
