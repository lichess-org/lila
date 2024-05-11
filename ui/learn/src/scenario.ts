import * as util from './util';
import * as ground from './ground';
import * as timeouts from './timeouts';
import { ChessCtrl } from './chess';
import * as cg from 'chessground/types';
import { RunCtrl } from './run/runCtrl';
import { setFen } from './chessground';

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
  runCtrl: RunCtrl;
  chess: ChessCtrl;
  makeChessDests(): cg.Dests;
}

export default function (blueprint: ScenarioLevel | undefined, opts: ScenarioOpts): Scenario {
  const steps = (blueprint || []).map(step => (typeof step !== 'string' ? step : { move: step, shapes: [] }));

  let it = 0;
  let isFailed = false;

  const fail = () => {
    isFailed = true;
    return false;
  };

  const opponent = () => {
    const step = steps[it];
    if (!step) return;
    const move = util.decomposeUci(step.move);
    const res = opts.chess.move(move[0], move[1], move[2]);
    if (!res) return fail();
    it++;
    setFen(opts.runCtrl, opts.chess.fen(), opts.chess.color(), opts.makeChessDests(), move);
    if (step.shapes) timeouts.setTimeout(() => opts.runCtrl.chessground?.setShapes(step.shapes), 500);
    return;
  };

  return {
    isComplete: () => it === steps.length,
    isFailed: () => isFailed,
    opponent,
    player: (move: Uci) => {
      const step = steps[it];
      if (!step) return false;
      if (step.move !== move) return fail();
      it++;
      if (step.shapes) opts.runCtrl.chessground?.setShapes(step.shapes);
      timeouts.setTimeout(opponent, 1000);
      return true;
    },
  };
}
