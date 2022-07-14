import { ReplayData, ReplayOpts } from './interfaces';

export default class ReplayCtrl {
  data: ReplayData;
  trans: Trans;

  constructor(opts: ReplayOpts, readonly redraw: () => void) {
    this.data = {
      pgn: opts.pgn.split(' '),
    };
    this.trans = lichess.trans(opts.i18n);
  }
}
