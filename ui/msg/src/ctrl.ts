import { MsgData, MsgOpts } from './interfaces';

export default class MsgCtrl {

  data: MsgData;
  trans: Trans;

  constructor(opts: MsgOpts, readonly redraw: () => void) {
    this.data = opts.data;
    this.trans = window.lichess.trans(opts.i18n);
  };
}
