import widget from './widget';

interface Opts {
  pause?: boolean;
  time: number;
}

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
