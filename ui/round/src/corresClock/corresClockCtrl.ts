import { Seconds, Millis } from '../clock/clockCtrl';
import RoundController from '../ctrl';

export interface CorresClockData {
  daysPerTurn: number;
  increment: Seconds;
  white: Seconds;
  black: Seconds;
  showBar: boolean;
}

// export interface CorresClockController {
//   root: RoundController;
//   data: CorresClockData;
//   timePercent(color: Color): number;
//   update(white: Seconds, black: Seconds): void;
//   tick(color: Color): void;
//   millisOf(color: Color): Millis;
// }

interface Times {
  white: Millis;
  black: Millis;
  lastUpdate: Millis;
}

export class CorresClockController {
  timePercentDivisor: number;
  times: Times;

  constructor(
    readonly root: RoundController,
    readonly data: CorresClockData,
    private readonly onFlag: () => void,
  ) {
    this.timePercentDivisor = 0.1 / data.increment;
    this.update(data.white, data.black);
  }

  timePercent = (color: Color): number =>
    Math.max(0, Math.min(100, this.times[color] * this.timePercentDivisor));

  update = (white: Seconds, black: Seconds): void => {
    this.times = {
      white: white * 1000,
      black: black * 1000,
      lastUpdate: performance.now(),
    };
  };

  tick = (color: Color): void => {
    const now = performance.now();
    this.times[color] -= now - this.times.lastUpdate;
    this.times.lastUpdate = now;
    if (this.times[color] <= 0) this.onFlag();
  };

  millisOf = (color: Color): Millis => Math.max(0, this.times[color]);
}
