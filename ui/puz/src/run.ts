import { Run, TimeMod } from './interfaces';
import { Config as CgConfig } from 'chessground/config';
import { getNow, uciToLastMove } from './util';
import { opposite } from 'chessops';
import { makeFen, parseFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';
import config from './config';

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

export const onGoodMove = (run: Run): TimeMod | undefined => {
  run.combo.inc();
  run.modifier.moveAt = getNow();
  const bonus = run.combo.bonus();
  if (bonus) {
    run.modifier.bonus = bonus;
    run.clock.addSeconds(bonus.seconds);
    return bonus;
  }
  return undefined;
};

export const onBadMove = (run: Run): void => {
  run.errors++;
  run.combo.reset();
  run.clock.addSeconds(-config.clock.malus);
  run.modifier.malus = {
    seconds: config.clock.malus,
    at: getNow(),
  };
};

export const countWins = (run: Run): number => run.history.reduce((c, r) => c + (r.win ? 1 : 0), 0);
