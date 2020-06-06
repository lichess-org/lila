import { Hook } from './interfaces';

type Mode = 0 | 1;
type Increment = 0 | 1;

interface FilterConfig {
  variant: string[];
  mode: Mode[];
  speed: number[];
  increment: Increment[];
  rating: [number, number];
}

interface Filtered {
  visible: Hook[];
  hidden: number;
}

export default class Filter {

  config: FilterConfig;

  constructor(readonly storage: LichessStorage, readonly redraw: () => void) {
    this.config = JSON.parse(storage.get() || '') as FilterConfig;
    this.config = this.config || {
      variant: [
        'standard',
        'chess960',
        'kingOfTheHill',
        'threeCheck',
        'antichess',
        'atomic',
        'horde',
        'racingKings',
        'crazyhouse'
      ],
      mode: [0, 1],
      speed: [0, 1, 2, 5, 4],
      increment: [0, 1],
      rating: [600, 2900]
    };
  }

  set = (config: FilterConfig) => {
    console.log(config);
    this.config = config;
  }

  filter = (hooks: Hook[]): Filtered => {
    const f = this.config,
      seen: string[] = [],
      visible: Hook[] = [];
    let variant: string, hidden = 0;
    hooks.forEach(function(hook) {
      variant = hook.variant;
      if (hook.action === 'cancel') visible.push(hook);
      else {
        if (!f.variant.includes(variant) ||
          !f.mode.includes(hook.ra || 0) ||
          !f.speed.includes(hook.s || 1 /* ultrabullet = bullet */) ||
          (f.increment.length && !f.increment.includes(hook.i)) ||
          (f.rating && (!hook.rating || (hook.rating < f.rating[0] || hook.rating > f.rating[1])))) {
          hidden++;
        } else {
          const hash = hook.ra + variant + hook.t + hook.rating;
          if (!seen.includes(hash)) visible.push(hook);
          seen.push(hash);
        }
      }
    });
    return {
      visible: visible,
      hidden: hidden
    };
  }
}
