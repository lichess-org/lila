import * as data from './data';

export const formatMs = (msTime: number): string => {
  const date = new Date(Math.max(0, msTime + 500)),
    hours = date.getUTCHours(),
    minutes = date.getUTCMinutes(),
    seconds = date.getUTCSeconds();
  return hours > 0 ? hours + ':' + pad(minutes) + ':' + pad(seconds) : minutes + ':' + pad(seconds);
};

export const otbClockIsRunning = (fen: string): boolean => !fen.includes('PPPPPPPP/RNBQKBNR');

export const lichessClockIsRunning = (fen: string, color: Color): boolean =>
  color == 'white' ? !fen.includes('PPPPPPPP/RNBQKBNR') : !fen.startsWith('rnbqkbnr/pppppppp');

export function setClockWidget(el: HTMLElement, opts: Opts): void {
  const instance = data.get(el, 'clock') as ClockWidget;
  if (instance) instance.set(opts);
  else data.set(el, 'clock', new ClockWidget(el, opts));
}

const pad = (x: number) => (x < 10 ? '0' : '') + x;

interface Opts {
  pause?: boolean;
  time: number;
}

class ClockWidget {
  target: number;
  interval: Timeout;
  constructor(
    readonly el: HTMLElement,
    private opts: Opts,
  ) {
    this.target = opts.time * 1000 + Date.now();
    if (!opts.pause) this.interval = setInterval(this.render, 1000);
    this.render();
  }
  set = (opts: Opts) => {
    this.opts = opts;
    this.target = opts.time * 1000 + Date.now();
    this.render();
    clearInterval(this.interval);
    if (!opts.pause) this.interval = setInterval(this.render, 1000);
  };
  private render = () => {
    if (document.body.contains(this.el)) {
      this.el.textContent = formatMs(this.target - Date.now());
      this.el.classList.toggle('clock--run', !this.opts.pause);
    } else clearInterval(this.interval);
  };
}
