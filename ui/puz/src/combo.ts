import { Config, TimeMod } from './interfaces';
import { getNow } from './util';

export class Combo {
  current = 0;
  best = 0;

  public constructor(readonly config: Config) {}

  inc = () => {
    this.current++;
    this.best = Math.max(this.best, this.current);
  };

  reset = () => {
    this.current = 0;
  };

  level = () =>
    this.config.combo.levels.reduce((lvl, [threshold, _], index) => (threshold <= this.current ? index : lvl), 0);

  percent = () => {
    const lvl = this.level();
    const levels = this.config.combo.levels;
    const lastLevel = levels[levels.length - 1];
    if (lvl >= levels.length - 1) {
      const range = lastLevel[0] - levels[levels.length - 2][0];
      return (((this.current - lastLevel[0]) / range) * 100) % 100;
    }
    const bounds = [levels[lvl][0], levels[lvl + 1][0]];
    return Math.floor(((this.current - bounds[0]) / (bounds[1] - bounds[0])) * 100);
  };

  bonus = (): TimeMod | undefined => {
    if (this.percent() == 0) {
      const level = this.level();
      if (level > 0)
        return {
          seconds: this.config.combo.levels[level][1],
          at: getNow(),
        };
    }
    return;
  };
}
