import { Run } from './interfaces';
import { Config as CgConfig } from 'shogiground/config';
import { usiToLastMove } from './util';
import { makeSfen } from 'shogiops/sfen';
import { shogigroundDests, shogigroundDropDests } from 'shogiops/compat';

export const makeCgOpts = (run: Run, canMove: boolean): CgConfig => {
  const cur = run.current;
  const pos = cur.position();
  return {
    sfen: makeSfen(pos.toSetup()),
    orientation: run.pov,
    turnColor: pos.turn,
    movable: {
      color: run.pov,
      dests: canMove ? shogigroundDests(pos) : undefined,
    },
    dropmode: {
      dropDests: canMove ? shogigroundDropDests(pos) : undefined,
    },
    check: !!pos.isCheck(),
    lastMove: cur.moveIndex > 0 ? usiToLastMove(cur.lastMove()) : undefined,
  };
};
