import { StormOpts, StormData, StormPuzzle } from './interfaces';

export default class StormCtrl {

  data: StormData;
  trans: Trans;

  constructor(readonly opts: StormOpts, readonly redraw: () => void) {
    this.data = opts.data;
    this.trans = lichess.trans(opts.i18n);
  }
}
