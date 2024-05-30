import * as game from 'game';
import { Redraw, RoundData } from '../interfaces';
import { updateElements } from './clockView';

export type Seconds = number;
export type Centis = number;
export type Millis = number;

interface ClockOpts {
  onFlag(): void;
  redraw: Redraw;
  soundColor?: Color;
  nvui: boolean;
}

export type TenthsPref = 0 | 1 | 2;

export interface ClockData {
  running: boolean;
  initial: Seconds;
  increment: Seconds;
  byoyomi: Seconds;
  periods: number;
  sente: Seconds;
  gote: Seconds;
  emerg: Seconds;
  showTenths: TenthsPref;
  clockCountdown: Seconds;
  moretime: number;
  sPeriods: number;
  gPeriods: number;
}

interface Times {
  sente: Millis;
  gote: Millis;
  activeColor?: Color;
  lastUpdate: Millis;
}

type ColorMap<T> = { [C in Color]: T };

export interface ClockElements {
  time?: HTMLElement;
  clock?: HTMLElement;
}

interface EmergSound {
  lowtime(): void;
  nextPeriod(): void;
  tick(): void;
  byoTicks?: number;
  next?: number;
  delay: Millis;
  playable: {
    sente: boolean;
    gote: boolean;
  };
}

export class ClockController {
  emergSound: EmergSound = {
    lowtime: window.lishogi.sound.lowtime,
    nextPeriod: window.lishogi.sound.period,
    tick: window.lishogi.sound.tick,
    delay: 20000,
    playable: {
      sente: true,
      gote: true,
    },
  };

  showTenths: (millis: Millis, color: Color) => boolean;
  times: Times;

  emergMs: Millis;
  byoEmergeS: Seconds;

  elements = {
    sente: {},
    gote: {},
  } as ColorMap<ClockElements>;

  byoyomi: number;
  initial: number;

  totalPeriods: number;
  curPeriods = {} as ColorMap<number>;
  goneBerserk = {} as ColorMap<boolean>;

  private tickCallback?: number;

  constructor(
    d: RoundData,
    readonly opts: ClockOpts
  ) {
    const cdata = d.clock!;

    if (cdata.showTenths === 0) this.showTenths = () => false;
    else {
      const cutoff = cdata.showTenths === 1 ? 10000 : 3600000;
      this.showTenths = (time, color) =>
        time < cutoff && (this.byoyomi === 0 || time <= 1000 || this.isUsingByo(color) || cdata.showTenths === 2);
    }

    this.byoyomi = cdata.byoyomi;
    this.initial = cdata.initial;

    this.totalPeriods = cdata.periods;
    this.curPeriods['sente'] = cdata.sPeriods ?? 0;
    this.curPeriods['gote'] = cdata.gPeriods ?? 0;

    this.goneBerserk[d.player.color] = !!d.player.berserk;
    this.goneBerserk[d.opponent.color] = !!d.opponent.berserk;

    this.emergMs = 1000 * Math.min(60, Math.max(10, cdata.initial * 0.125));
    this.byoEmergeS = cdata.clockCountdown ?? 3;

    this.setClock(d, cdata.sente, cdata.gote, cdata.sPeriods, cdata.gPeriods);
  }

  isUsingByo = (color: Color): boolean => this.byoyomi > 0 && (this.curPeriods[color] > 0 || this.initial === 0);

  setClock = (d: RoundData, sente: Seconds, gote: Seconds, sPer: number, gPer: number, delay: Centis = 0) => {
    const isClockRunning = game.playable(d) && (game.playedPlies(d) > 1 || d.clock!.running),
      delayMs = delay * 10;

    this.times = {
      sente: sente * 1000,
      gote: gote * 1000,
      activeColor: isClockRunning ? d.game.player : undefined,
      lastUpdate: performance.now() + delayMs,
    };
    this.curPeriods['sente'] = sPer;
    this.curPeriods['gote'] = gPer;

    if (isClockRunning) this.scheduleTick(this.times[d.game.player], d.game.player, delayMs);
  };

  addTime = (color: Color, time: Centis): void => {
    this.times[color] += time * 10;
  };

  setBerserk = (color: Color): void => {
    this.goneBerserk[color] = true;
  };

  nextPeriod = (color: Color): void => {
    this.curPeriods[color] += 1;
    this.times[color] += this.byoyomi * 1000;
    if (this.opts.soundColor === color) this.emergSound.nextPeriod();
    this.emergSound.byoTicks = undefined;
  };

  stopClock = (): Millis | void => {
    const color = this.times.activeColor;
    if (color) {
      const curElapse = this.elapsed();
      this.times[color] = Math.max(0, this.times[color] - curElapse);
      this.times.activeColor = undefined;
      this.emergSound.byoTicks = undefined;
      return curElapse;
    }
  };

  hardStopClock = (): void => (this.times.activeColor = undefined);

  private scheduleTick = (time: Millis, color: Color, extraDelay: Millis) => {
    if (this.tickCallback !== undefined) clearTimeout(this.tickCallback);
    this.tickCallback = setTimeout(
      this.tick,
      // changing the value of active node confuses the chromevox screen reader
      // so update the clock less often
      this.opts.nvui ? 1000 : (time % (this.showTenths(time, color) ? 100 : 500)) + 1 + extraDelay
    );
  };

  // Should only be invoked by scheduleTick.
  private tick = (): void => {
    this.tickCallback = undefined;

    const color = this.times.activeColor;
    if (color === undefined) return;

    const now = performance.now(),
      millis = Math.max(0, this.times[color] - this.elapsed(now)),
      curPeriod = this.curPeriods[color];

    this.scheduleTick(millis, color, 0);
    if (millis === 0 && !this.goneBerserk[color] && this.byoyomi > 0 && curPeriod < this.totalPeriods) {
      this.nextPeriod(color);
      this.opts.redraw();
    } else if (millis === 0) this.opts.onFlag();
    else updateElements(this, this.elements[color], millis, color);

    if (this.opts.soundColor === color) {
      if (this.emergSound.playable[color]) {
        if (millis < this.emergMs && !(now < this.emergSound.next!) && curPeriod === 0) {
          this.emergSound.lowtime();
          this.emergSound.next = now + this.emergSound.delay;
          this.emergSound.playable[color] = false;
        }
      } else if (millis > 1.5 * this.emergMs) {
        this.emergSound.playable[color] = true;
      }
      if (
        this.byoyomi >= 5 &&
        millis > 0 &&
        ((this.emergSound.byoTicks === undefined && millis < this.byoEmergeS * 1000) ||
          (this.emergSound.byoTicks && Math.floor(millis / 1000) < this.emergSound.byoTicks)) &&
        this.isUsingByo(color)
      ) {
        this.emergSound.byoTicks = Math.floor(millis / 1000);
        this.emergSound.tick();
      }
    }
  };

  elapsed = (now = performance.now()) => Math.max(0, now - this.times.lastUpdate);

  millisOf = (color: Color): Millis =>
    this.times.activeColor === color ? Math.max(0, this.times[color] - this.elapsed()) : this.times[color];

  isRunning = () => this.times.activeColor !== undefined;
}
