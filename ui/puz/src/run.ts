import { Run } from './interfaces';
import { Config as CgConfig } from 'chessground/config';
import { uciToLastMove } from './util';
import { opposite } from 'chessops';
import { makeFen, parseFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';

export const makeCgOpts = (run: Run): CgConfig => {
  const cur = run.current;
  const pos = cur.position();
  const pov = opposite(parseFen(cur.puzzle.fen).unwrap().turn);
  const canMove = !run.endAt;
  return {
    fen: makeFen(pos.toSetup()),
    orientation: pov,
    turnColor: pos.turn,
    movable: canMove
      ? {
          color: pov,
          dests: chessgroundDests(pos),
        }
      : undefined,
    check: !!pos.isCheck(),
    lastMove: uciToLastMove(cur.lastMove()),
  };
};
