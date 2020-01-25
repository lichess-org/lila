import { MsgData, MsgOpts, Msg, Redraw } from './interfaces';
import * as xhr from './xhr';

export default class MsgCtrl {

  data: MsgData;
  trans: Trans;

  constructor(opts: MsgOpts, readonly redraw: Redraw) {
    xhr.upgradeData(opts.data);
    this.data = opts.data;
    this.trans = window.lichess.trans(opts.i18n);
  };

  openConvo = (userId: string) => {
    xhr.loadConvo(userId).then(data => {
      this.data = data;
      this.redraw();
      data.convo && history.replaceState({contact: userId}, '', `/inbox/${data.convo.thread.contact.name}`);
    });
  }

  post = (text: string) => {
    this.data.convo && xhr.post(this.data.convo.thread.contact.id, text).then(this.addMsg);
  }

  addMsg = (msg: Msg) => {
    if (this.data.convo && this.data.convo.thread.id == msg.thread) {
      this.data.convo.msgs.unshift(msg);
      this.redraw();
    }
  }
}
