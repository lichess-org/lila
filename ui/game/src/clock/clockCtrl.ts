import { updateElements } from './clockView';
import { ShowClockTenths } from 'common/prefs';
import { reducedMotion } from 'common/device';

export interface ClockOpts {
  onFlag(): void;
  bothPlayersHavePlayed(): boolean;
  hasGoneBerserk(color: Color): boolean;
  soundColor?: Color;
  nvui: boolean;
}

export interface ClockConfig {
  initial: Seconds;
  increment: Seconds;
  moretime: Seconds;
}

// JSON data from the server
export interface ClockData extends ClockConfig {
  running: boolean;
  white: Seconds;
  black: Seconds;
}
export interface ClockPref {
  clockTenths: ShowClockTenths;
  clockBar: boolean;
}

interface Times {
  white: Millis;
  black: Millis;
  activeColor?: Color;
  lastUpdate: Millis;
}

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

interface SetData {
  white: Seconds;
  black: Seconds;
  ticking: Color | undefined;
  delay?: Centis;
}

export class ClockCtrl {
  emergSound: EmergSound = {
    play: () => site.sound.play('lowTime'),
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

  elements: ByColor<ClockElements> = { white: {}, black: {} };

  private tickCallback?: number;

  constructor(
    data: ClockData,
    pref: ClockPref,
    ticking: Color | undefined,
    readonly opts: ClockOpts,
  ) {
    this.showTenths =
      pref.clockTenths === ShowClockTenths.Never
        ? () => false
        : ShowClockTenths.Below10Secs
          ? time => time < 10000
          : time => time < 3600000;

    this.showBar = pref.clockBar && !this.opts.nvui && !reducedMotion();
    this.barTime = 1000 * (Math.max(data.initial, 2) + 5 * data.increment);
    this.timeRatioDivisor = 1 / this.barTime;

    this.emergMs = 1000 * Math.min(60, Math.max(10, data.initial * 0.125));

    this.setClock({
      white: data.white,
      black: data.black,
      ticking,
    });
  }

  timeRatio = (millis: number): number => Math.min(1, millis * this.timeRatioDivisor);

  setClock = (d: SetData): void => {
    const delayMs = (d.delay || 0) * 10;

    this.times = {
      white: d.white * 1000,
      black: d.black * 1000,
      activeColor: d.ticking,
      lastUpdate: performance.now() + delayMs,
    };

    if (d.ticking) this.scheduleTick(this.times[d.ticking], delayMs);
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
      this.opts.nvui ? 1000 : (time % (this.showTenths(time) ? 100 : 500)) + 1 + extraDelay,
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

  elapsed = (now: number = performance.now()): number => Math.max(0, now - this.times.lastUpdate);

  millisOf = (color: Color): Millis =>
    this.times.activeColor === color ? Math.max(0, this.times[color] - this.elapsed()) : this.times[color];

  isRunning = (): boolean => this.times.activeColor !== undefined;

  speak = (): void => {
    const msgs = ['white', 'black'].map(color => {
      const time = this.millisOf(color as Color);
      const date = new Date(time);
      const msg =
        (time >= 3600000 ? simplePlural(Math.floor(time / 3600000), 'hour') : '') +
        ' ' +
        simplePlural(date.getUTCMinutes(), 'minute') +
        ' ' +
        simplePlural(date.getUTCSeconds(), 'second');
      return `${color} ${msg}`;
    });
    site.sound.say(msgs.join('. '), false, true);
  };
}

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb !== 1 ? 's' : ''}`;
}
