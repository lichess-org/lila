import { Redraw } from 'chessground/types';
import { toggle, Toggle } from 'common';

export class PuzFilters {
  fail: Toggle;
  slow: Toggle;
  skip?: Toggle;

  constructor(redraw: Redraw, skip: boolean) {
    this.fail = toggle(false, redraw);
    this.slow = toggle(false, redraw);
    this.skip = skip ? toggle(false, redraw) : undefined;
  }
}
