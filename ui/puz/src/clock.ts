import { Config } from './interfaces';
import { getNow } from './util';

export class Clock {
  startAt: number | undefined;
  initialMillis: number;

  public constructor(readonly config: Config, startedMillisAgo: number = 0) {
    this.initialMillis = config.clock.initial * 1000 - (startedMillisAgo || 0);
  }

  start = () => {
    if (!this.startAt) this.startAt = getNow();
  };

  started = () => !!this.startAt;

  millis = (): number =>
    this.startAt ? Math.max(0, this.startAt + this.initialMillis - getNow()) : this.initialMillis;

  addSeconds = (seconds: number) => {
    this.initialMillis += seconds * 1000;
  };

  flag = () => !this.millis();
}
