import { Config } from './interfaces';
import { getNow } from './util';

export class Clock {
  startAt: number | undefined;
  initialMillis: number;

  public constructor(
    readonly config: Config,
    startedMillisAgo: number | undefined = 0,
  ) {
    this.initialMillis = config.clock.initial * 1000 - (startedMillisAgo || 0);
  }

  start = (): void => {
    if (!this.startAt) this.startAt = getNow();
  };

  started = (): boolean => !!this.startAt;

  millis = (): number =>
    this.startAt ? Math.max(0, this.startAt + this.initialMillis - getNow()) : this.initialMillis;

  addSeconds = (seconds: number): void => {
    this.initialMillis += seconds * 1000;
  };

  flag = (): boolean => !this.millis();
}
