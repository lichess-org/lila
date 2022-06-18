import { Run } from './interfaces';
import { Config as SgConfig } from 'shogiground/config';
import { usiToLastMove } from './util';
import { makeSfen } from 'shogiops/sfen';
import { shogigroundDests, shogigroundDropDests } from 'shogiops/compat';
import { handRoles } from 'shogiops/variantUtil';
import { Dests, DropDests } from 'shogiground/types';

export const makeSgOpts = (run: Run, canMove: boolean): SgConfig => {
  const cur = run.current,
    pos = cur.position(),
    sfen = makeSfen(pos),
    splitSfen = sfen.split(' ');
  return {
    sfen: { board: splitSfen[0], hands: splitSfen[2] },
    activeColor: run.pov,
    orientation: run.pov,
    turnColor: pos.turn,
    movable: {
      dests: canMove ? (shogigroundDests(pos) as Dests) : undefined,
    },
    droppable: {
      dests: canMove ? (shogigroundDropDests(pos) as DropDests) : undefined,
    },
    hands: {
      roles: handRoles('standard'),
    },
    check: !!pos.isCheck(),
    lastDests: cur.moveIndex > 0 ? usiToLastMove(cur.lastMove()) : undefined,
  };
};
