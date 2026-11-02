import { opposite, uciToMove } from '@lichess-org/chessground/util';
import { chessgroundDests } from 'chessops/compat';
import { makeFen } from 'chessops/fen';

import { capitalize } from '@/game';

import type { Run } from './interfaces';

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
    check: pos.isCheck(),
    lastMove: uciToMove(cur.lastMove()),
    animation: {
      enabled: cur.moveIndex >= 0,
    },
  };
};

export const povMessage = ({ pov }: Run): string =>
  i18n.storm[`youPlayThe${capitalize(pov)}PiecesInAllPuzzles`];
