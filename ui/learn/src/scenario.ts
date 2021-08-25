import * as util from './util';
import * as ground from './ground';
import * as timeouts from './timeouts';
import { ChessCtrl } from './chess';

export interface Scenario {
  isComplete(): boolean;
  isFailed(): boolean;
  opponent(): void;
  player(move: Uci): boolean;
}

export type ScenarioLevel = (
  | Uci
  | {
      move: Uci;
      shapes: ground.Shape[];
    }
)[];

interface ScenarioOpts {
  chess: ChessCtrl;
  makeChessDests(): ground.Dests;
}

export default function (blueprint: ScenarioLevel | undefined, opts: ScenarioOpts): Scenario {
  const steps = (blueprint || []).map(function (step) {
    if (typeof step !== 'string') return step;
    return {
      move: step,
      shapes: [],
    };
  });

  let it = 0;
  let isFailed = false;

  const fail = function () {
    isFailed = true;
    return false;
  };

  const opponent = function () {
    const step = steps[it];
    if (!step) return;
    const move = util.decomposeUci(step.move);
    const res = opts.chess.move(move[0], move[1], move[2]);
    if (!res) return fail();
    it++;
    ground.fen(opts.chess.fen(), opts.chess.color(), opts.makeChessDests(), move);
    if (step.shapes)
      timeouts.setTimeout(function () {
        ground.setShapes(step.shapes);
      }, 500);
    return;
  };

  return {
    isComplete: function () {
      return it === steps.length;
    },
    isFailed: function () {
      return isFailed;
    },
    opponent: opponent,
    player: function (move: Uci) {
      const step = steps[it];
      if (!step) return false;
      if (step.move !== move) return fail();
      it++;
      if (step.shapes) ground.setShapes(step.shapes);
      timeouts.setTimeout(opponent, 1000);
      return true;
    },
  };
}
