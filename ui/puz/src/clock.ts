import { getNow } from './util';
import config from './config';

export class Clock {
  startAt: number | undefined;
  initialMillis = config.clock.initial * 1000;

  start = () => {
    this.startAt = getNow();
  };

  millis = (): number =>
    this.startAt ? Math.max(0, this.startAt + this.initialMillis - getNow()) : this.initialMillis;

  addSeconds = (seconds: number) => {
    this.initialMillis += seconds * 1000;
  };

  flag = () => !this.millis();
}
