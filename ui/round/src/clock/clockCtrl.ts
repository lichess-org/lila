import { updateElements } from './clockView';
import { RoundData } from '../interfaces';
import * as game from 'game';

export type Seconds = number;
export type Centis = number;
export type Millis = number;

interface ClockOpts {
  onFlag(): void;
  soundColor?: Color;
  nvui: boolean;
}

export type TenthsPref = 0 | 1 | 2;

export interface ClockData {
  running: boolean;
  initial: Seconds;
  increment: Seconds;
  white: Seconds;
  black: Seconds;
  emerg: Seconds;
  showTenths: TenthsPref;
  showBar: boolean;
  moretime: number;
}

interface Times {
  white: Millis;
  black: Millis;
  activeColor?: Color;
  lastUpdate: Millis;
}

type ColorMap<T> = { [C in Color]: T };

export interface ClockElements {
  time?: HTMLElement;
  clock?: HTMLElement;
  bar?: HTMLElement;
  barAnim?: Animation;
}

interface EmergSound {
  play(): void;
  next?: number;
  delay: Millis;
  playable: {
    white: boolean;
    black: boolean;
  };
}

export class ClockController {
  emergSound: EmergSound = {
    play: () => lichess.sound.play('lowTime'),
    delay: 20000,
    playable: {
      white: true,
      black: true,
    },
  };

  showTenths: (millis: Millis) => boolean;
  showBar: boolean;
  times: Times;

  barTime: number;
  timeRatioDivisor: number;
  emergMs: Millis;

  elements = {
    white: {},
    black: {},
  } as ColorMap<ClockElements>;

  private tickCallback?: number;

  constructor(d: RoundData, readonly opts: ClockOpts) {
    const cdata = d.clock!;

    if (cdata.showTenths === 0) this.showTenths = () => false;
    else {
      const cutoff = cdata.showTenths === 1 ? 10000 : 3600000;
      this.showTenths = time => time < cutoff;
    }

    this.showBar = cdata.showBar && !this.opts.nvui;
    this.barTime = 1000 * (Math.max(cdata.initial, 2) + 5 * cdata.increment);
    this.timeRatioDivisor = 1 / this.barTime;

    this.emergMs = 1000 * Math.min(60, Math.max(10, cdata.initial * 0.125));

    this.setClock(d, cdata.white, cdata.black);
  }

  timeRatio = (millis: number): number => Math.min(1, millis * this.timeRatioDivisor);

  setClock = (d: RoundData, white: Seconds, black: Seconds, delay: Centis = 0) => {
    const isClockRunning = game.playable(d) && (game.playedTurns(d) > 1 || d.clock!.running),
      delayMs = delay * 10;

    this.times = {
      white: white * 1000,
      black: black * 1000,
      activeColor: isClockRunning ? d.game.player : undefined,
      lastUpdate: performance.now() + delayMs,
    };

    if (isClockRunning) this.scheduleTick(this.times[d.game.player], delayMs);
  };

  addTime = (color: Color, time: Centis): void => {
    this.times[color] += time * 10;
  };

  stopClock = (): Millis | void => {
    const color = this.times.activeColor;
    if (color) {
      const curElapse = this.elapsed();
      this.times[color] = Math.max(0, this.times[color] - curElapse);
      this.times.activeColor = undefined;
      return curElapse;
    }
  };

  hardStopClock = (): void => (this.times.activeColor = undefined);

  private scheduleTick = (time: Millis, extraDelay: Millis) => {
    if (this.tickCallback !== undefined) clearTimeout(this.tickCallback);
    this.tickCallback = setTimeout(
      this.tick,
      // changing the value of active node confuses the chromevox screen reader
      // so update the clock less often
      this.opts.nvui ? 1000 : (time % (this.showTenths(time) ? 100 : 500)) + 1 + extraDelay
    );
  };

  // Should only be invoked by scheduleTick.
  private tick = (): void => {
    this.tickCallback = undefined;

    const color = this.times.activeColor;
    if (color === undefined) return;

    const now = performance.now();
    const millis = Math.max(0, this.times[color] - this.elapsed(now));

    this.scheduleTick(millis, 0);
    if (millis === 0) this.opts.onFlag();
    else updateElements(this, this.elements[color], millis);

    if (this.opts.soundColor === color) {
      if (this.emergSound.playable[color]) {
        if (millis < this.emergMs && !(now < this.emergSound.next!)) {
          this.emergSound.play();
          this.emergSound.next = now + this.emergSound.delay;
          this.emergSound.playable[color] = false;
        }
      } else if (millis > 1.5 * this.emergMs) {
        this.emergSound.playable[color] = true;
      }
    }
  };

  elapsed = (now = performance.now()) => Math.max(0, now - this.times.lastUpdate);

  millisOf = (color: Color): Millis =>
    this.times.activeColor === color ? Math.max(0, this.times[color] - this.elapsed()) : this.times[color];

  isRunning = () => this.times.activeColor !== undefined;
}
