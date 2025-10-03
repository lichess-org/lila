import * as timeouts from './timeouts';
import type { ChessCtrl } from './chess';
import { decomposeUci } from './util';
import type { Shape } from './chessground';

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
      shapes: Shape[];
    }
)[];

interface ScenarioOpts {
  chess: ChessCtrl;
  makeChessDests(): Dests;
  setFen(fen: string, color: Color, dests: Dests, lastMove?: [Key, Key]): void;
  setShapes(shapes: Shape[]): void;
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
    const move = decomposeUci(step.move);
    const res = opts.chess.move(move[0], move[1], move[2]);
    if (!res) return fail();
    it++;
    opts.setFen(opts.chess.fen(), opts.chess.getColor(), opts.makeChessDests(), [move[0], move[1]]);
    if (step.shapes) timeouts.setTimeout(() => opts.setShapes(step.shapes), 500);
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
      if (step.shapes) opts.setShapes(step.shapes);
      timeouts.setTimeout(opponent, 1000);
      return true;
    },
  };
}
