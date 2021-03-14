import { Run } from './interfaces';
import { Config as CgConfig } from 'chessground/config';
import { uciToLastMove } from './util';
import { makeFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';

export const makeCgOpts = (run: Run, canMove: boolean): CgConfig => {
  const cur = run.current;
  const pos = cur.position();
  return {
    fen: makeFen(pos.toSetup()),
    orientation: run.pov,
    turnColor: pos.turn,
    movable: {
      color: run.pov,
      dests: canMove ? chessgroundDests(pos) : undefined,
    },
    check: !!pos.isCheck(),
    lastMove: uciToLastMove(cur.lastMove()),
  };
};
