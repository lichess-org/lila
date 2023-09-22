import { Run } from './interfaces';
import { Config as CgConfig } from 'chessground/config';
import { opposite, uciToMove } from 'chessground/util';

export const makeCgOpts = (run: Run, canMove: boolean, flipped: boolean): CgConfig => {
  const cur = run.current;
  const pos = cur.position();
  return {
    fen: co.fen.makeFen(pos.toSetup()),
    orientation: flipped ? opposite(run.pov) : run.pov,
    turnColor: pos.turn,
    movable: {
      color: run.pov,
      dests: canMove ? co.compat.chessgroundDests(pos) : undefined,
    },
    check: !!pos.isCheck(),
    lastMove: uciToMove(cur.lastMove()),
    animation: {
      enabled: cur.moveIndex >= 0,
    },
  };
};

export const povMessage = (run: Run) =>
  `youPlayThe${run.pov == 'white' ? 'White' : 'Black'}PiecesInAllPuzzles`;
