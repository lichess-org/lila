import { MsgData, MsgOpts, Msg, SearchRes, Redraw } from './interfaces';
import * as xhr from './xhr';

export default class MsgCtrl {

  data: MsgData;
  trans: Trans;
  searchRes?: SearchRes;

  constructor(opts: MsgOpts, readonly redraw: Redraw) {
    xhr.upgradeData(opts.data);
    this.data = opts.data;
    this.trans = window.lichess.trans(opts.i18n);
  };

  openConvo = (userId: string) => {
    xhr.loadConvo(userId).then(data => {
      this.data = data;
      this.searchRes = undefined;
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
      this.data.threads.filter(t => t.id == msg.thread).forEach(t => {
        t.lastMsg = msg;
      });
      this.data.threads.sort((a, b) => {
        return a.lastMsg ? (
          b.lastMsg ? (a.lastMsg.date < b.lastMsg.date ? 1 : -1) : 1
        ) : -1
      });
      this.redraw();
    }
  }

  search = (q: string) => {
    if (q.length > 1) xhr.search(q).then((res: SearchRes) => {
      this.searchRes = res;
      this.redraw();
    });
    else {
      this.searchRes = undefined;
      this.redraw();
    }
  }
}
