import { Move } from 'chessops';
import { Board } from './chess';

export const requestBotMove = (board: Board): Promise<Move> =>
  new Promise(resolve => setTimeout(() => resolve(selectMove(board)), selectWait()));

const selectMove = (board: Board): Move => randomMove(board);

const selectWait = () => 500 + Math.floor(Math.random() * 1000);

const randomMove = (board: Board): Move => {
  const dests = board.chess.allDests();
  const moves = Array.from(dests.entries()).flatMap(([from, tos]) =>
    Array.from(tos).map(to => ({ from, to })),
  );
  const move = moves[Math.floor(Math.random() * moves.length)];
  return move;
};
