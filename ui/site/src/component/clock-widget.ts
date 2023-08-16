import * as data from 'common/data';

interface Opts {
  pause?: boolean;
  time: number;
}

export default function (el: HTMLElement, opts: Opts) {
  const instance = data.get(el, 'clock') as ClockWidget;
  if (instance) instance.set(opts);
  else data.set(el, 'clock', new ClockWidget(el, opts));
}

class ClockWidget {
  target: number;
  interval: number;
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

const formatMs = (msTime: number) => {
  const date = new Date(Math.max(0, msTime + 500)),
    hours = date.getUTCHours(),
    minutes = date.getUTCMinutes(),
    seconds = date.getUTCSeconds();
  return hours > 0 ? hours + ':' + pad(minutes) + ':' + pad(seconds) : minutes + ':' + pad(seconds);
};

const pad = (x: number) => (x < 10 ? '0' : '') + x;
