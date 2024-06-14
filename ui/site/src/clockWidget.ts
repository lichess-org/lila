import * as data from 'common/data';
import { formatMs } from 'common/clock';

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
