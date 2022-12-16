import { Millis, Seconds } from '../clock/clockCtrl';
import RoundController from '../ctrl';

export interface CorresClockData {
  daysPerTurn: number;
  increment: Seconds;
  sente: Seconds;
  gote: Seconds;
}

export interface CorresClockController {
  root: RoundController;
  data: CorresClockData;
  update(sente: Seconds, gote: Seconds): void;
  tick(color: Color): void;
  millisOf(color: Color): Millis;
}

interface Times {
  sente: Millis;
  gote: Millis;
  lastUpdate: Millis;
}

export function ctrl(root: RoundController, data: CorresClockData, onFlag: () => void): CorresClockController {
  let times: Times;

  function update(sente: Seconds, gote: Seconds): void {
    times = {
      sente: sente * 1000,
      gote: gote * 1000,
      lastUpdate: performance.now(),
    };
  }
  update(data.sente, data.gote);

  function tick(color: Color): void {
    const now = performance.now();
    times[color] -= now - times.lastUpdate;
    times.lastUpdate = now;
    if (times[color] <= 0) onFlag();
  }

  function millisOf(color: Color): Millis {
    return Math.max(0, times[color]);
  }

  return {
    root,
    data,
    millisOf,
    update,
    tick,
  };
}
