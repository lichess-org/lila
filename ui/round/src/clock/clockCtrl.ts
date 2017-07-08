import { updateElements } from './clockView';
import RoundController from '../ctrl';

export type Seconds = number;
export type Centis = number;
export type Millis = number;

interface ClockOpts {
  onFlag(): void;
  soundColor?: Color
}

export interface ClockData {
  running: boolean;
  initial: Seconds;
  increment: Seconds;
  white: Seconds;
  black: Seconds;
  emerg: Seconds;
  showTenths: 0 | 1 | 2;
  showBar: boolean;
  moretime: number;
}

interface Times {
  white: Millis;
  black: Millis;
  lastUpdate: Millis;
}

interface Elements {
  white: ColorElements;
  black: ColorElements;
}

interface ColorElements {
  time: HTMLElement;
  bar: HTMLElement;
}

interface EmergSound {
  play(): void;
  next?: number;
  delay: Millis,
  playable: {
    white: boolean;
    black: boolean;
  };
}

export class ClockController {

  emergSound: EmergSound = {
    play: window.lichess.sound.lowtime,
    delay: 20000,
    playable: {
      white: true,
      black: true
    }
  };

  times: Times;

  timePercentDivisor: number
  emergMs: Millis;

  elements: Elements = {
    white: {},
    black: {}
  } as Elements;

  constructor(public data: ClockData, public opts: ClockOpts) {

    this.timePercentDivisor = .1 / (Math.max(data.initial, 2) + 5 * data.increment);

    this.emergMs = 1000 * Math.min(60, Math.max(10, data.initial * .125));

    this.update(data.white, data.black);
  }

  timePercent = (color: Color): number =>
    Math.max(0, Math.min(100, this.times[color] * this.timePercentDivisor));

  update = (white: Seconds, black: Seconds, delayCentis?: Centis): void => {
    this.times = {
      white: white * 1000,
      black: black * 1000,
      lastUpdate: Date.now() + (delayCentis || 1) * 10
    };
  };

  tick = (ctrl: RoundController, color: Color): void => {
    const now = Date.now();
    if (now > this.times.lastUpdate) {
      this.times[color] -= now - this.times.lastUpdate;
      this.times.lastUpdate = now;
    }
    const millis = this.times[color];

    if (millis <= 0) this.opts.onFlag();
    else updateElements(ctrl, color);

    if (this.opts.soundColor == color) {
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

  millisOf = (color: Color): Millis => Math.max(0, this.times[color]);
}
