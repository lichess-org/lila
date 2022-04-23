import * as data from 'common/data';

interface Opts {
  pause?: boolean;
  time: number;
}

const widget = (name: string, prototype: any) => {
  const constructor: any = (($ as any)[name] = function (options: any, element: HTMLElement) {
    const self: any = this;
    self.element = $(element);
    (element as any)[name] = this;
    self.options = options;
    self._create();
  });
  constructor.prototype = prototype;
  ($.fn as any)[name] = function (method: string) {
    const args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string')
      this.each(function (this: HTMLElement) {
        const instance = data.get(this, name);
        if (instance && $.isFunction(instance[method])) instance[method].apply(instance, args);
      });
    else
      this.each(function (this: HTMLElement) {
        if (!data.get(this, name)) data.set(this, name, new constructor(method, this));
      });
    return this;
  };
};

export default function loadClockWidget() {
  widget('clock', {
    _create: function () {
      this.target = this.options.time * 1000 + Date.now();
      if (!this.options.pause) this.interval = setInterval(this.render.bind(this), 1000);
      this.render();
    },

    set: function (opts: Opts) {
      this.options = opts;
      this.target = this.options.time * 1000 + Date.now();
      this.render();
      clearInterval(this.interval);
      if (!opts.pause) this.interval = setInterval(this.render.bind(this), 1000);
    },

    render: function () {
      if (document.body.contains(this.element[0])) {
        this.element.text(this.formatMs(this.target - Date.now()));
        this.element.toggleClass('clock--run', !this.options.pause);
      } else clearInterval(this.interval);
    },

    pad(x: number) {
      return (x < 10 ? '0' : '') + x;
    },

    formatMs: function (msTime: number) {
      const date = new Date(Math.max(0, msTime + 500)),
        hours = date.getUTCHours(),
        minutes = date.getUTCMinutes(),
        seconds = date.getUTCSeconds();
      return hours > 0 ? hours + ':' + this.pad(minutes) + ':' + this.pad(seconds) : minutes + ':' + this.pad(seconds);
    },
  });
}
