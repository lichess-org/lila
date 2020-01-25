import { MsgData, MsgOpts, Redraw } from './interfaces';
import * as xhr from './xhr';

export default class MsgCtrl {

  data: MsgData;
  trans: Trans;

  constructor(opts: MsgOpts, readonly redraw: Redraw) {
    this.data = opts.data;
    this.trans = window.lichess.trans(opts.i18n);
  };

  openThread = (userId: string) => {
    xhr.loadThread(userId).then(data => {
      this.data = data;
      this.redraw();
      data.convo && history.replaceState({contact: userId}, '', `/inbox/${data.convo.thread.contact.name}`);
    });
  }
}
