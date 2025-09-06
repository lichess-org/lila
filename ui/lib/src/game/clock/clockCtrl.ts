import { updateElements, formatClockTimeVerbal } from './clockView';
import { ShowClockTenths } from '../../prefs';
import { reducedMotion } from '../../device';

export interface ClockOpts {
  onFlag(): void;
  bothPlayersHavePlayed(): boolean;
  hasGoneBerserk(color: Color): boolean;
  alarmColor?: Color;
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

export interface SetData {
  white: Seconds;
  black: Seconds;
  ticking: Color | undefined;
  delay?: Centis; // network lag to visually compensate
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
  alarmAction?: { seconds: Seconds; fire: () => void };

  elements: ByColor<ClockElements> = { white: {}, black: {} };

  private tickTimeout?: Timeout;

  constructor(
    data: ClockData,
    pref: ClockPref,
    ticking: Color | undefined,
    readonly opts: ClockOpts,
  ) {
    this.showTenths =
      pref.clockTenths === ShowClockTenths.Never
        ? () => false
        : pref.clockTenths === ShowClockTenths.Below10Secs
          ? time => time < 10000
          : time => time < 3600000;

    this.showBar = pref.clockBar && !site.blindMode && !reducedMotion();
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
    if (this.tickTimeout !== undefined) clearTimeout(this.tickTimeout);
    this.tickTimeout = setTimeout(
      this.tick,
      // changing the value of active node confuses the chromevox screen reader
      // so update the clock less often
      site.blindMode ? 1000 : (time % (this.showTenths(time) ? 100 : 500)) + 1 + extraDelay,
    );
  };

  // Should only be invoked by scheduleTick.
  private tick = (): void => {
    this.tickTimeout = undefined;

    const color = this.times.activeColor;
    if (color === undefined) return;

    const now = performance.now();
    const millis = Math.max(0, this.times[color] - this.elapsed(now));

    this.scheduleTick(millis, 0);
    if (millis === 0) this.opts.onFlag();
    else updateElements(this, this.elements[color], millis);

    if (this.opts.alarmColor === color) {
      if (this.alarmAction && millis < this.alarmAction.seconds * 1000) {
        this.alarmAction.fire();
        this.alarmAction = undefined;
      }
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
    const msgs = [
      { key: 'white', i18nName: i18n.site.white },
      { key: 'black', i18nName: i18n.site.black },
    ].map(color => {
      const time = this.millisOf(color.key as Color);
      const msg = formatClockTimeVerbal(time);
      return `${color.i18nName} - ${msg}`;
    });
    site.sound.say(msgs.join('. '), false, true, true);
  };
}
