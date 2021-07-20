import { Run } from './interfaces';
import { Config as CgConfig } from 'chessground/config';
import { opposite } from 'chessground/util';
import { uciToLastMove } from './util';
import { makeFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';

export const makeCgOpts = (run: Run, canMove: boolean, flipped: boolean): CgConfig => {
  const cur = run.current;
  const pos = cur.position();
  return {
    fen: makeFen(pos.toSetup()),
    orientation: flipped ? opposite(run.pov) : run.pov,
    turnColor: pos.turn,
    movable: {
      color: run.pov,
      dests: canMove ? chessgroundDests(pos) : undefined,
    },
    check: !!pos.isCheck(),
    lastMove: uciToLastMove(cur.lastMove()),
    animation: {
      enabled: cur.moveIndex >= 0,
    },
  };
};

export const povMessage = (run: Run) => `youPlayThe${run.pov == 'white' ? 'White' : 'Black'}PiecesInAllPuzzles`;
