import { MsgData, MsgOpts, Msg, SearchRes, Redraw } from './interfaces';
import * as network from './network';

export default class MsgCtrl {

  data: MsgData;
  trans: Trans;
  searchRes?: SearchRes;

  constructor(opts: MsgOpts, readonly redraw: Redraw) {
    network.upgradeData(opts.data);
    this.data = opts.data;
    this.trans = window.lichess.trans(opts.i18n);
    network.onMsgNew(this.addMsg);
  };

  openConvo = (userId: string) => {
    network.loadConvo(userId).then(data => {
      this.data = data;
      this.searchRes = undefined;
      this.redraw();
      data.convo && history.replaceState({contact: userId}, '', `/inbox/${data.convo.thread.contact.name}`);
    });
  }

  post = (text: string) => this.data.convo && network.post(this.data.convo.thread.contact.id, text);

  addMsg = (msg: Msg) => {
    const thread = this.data.threads.filter(t => t.id == msg.thread)[0];
    if (thread) {
      thread.lastMsg = msg;
      // pull the thread to the top of the list
      this.data.threads = [thread].concat(this.data.threads.filter(t => t.id != thread.id));
      if (this.data.convo && this.data.convo.thread.id == thread.id) {
        this.data.convo.msgs.unshift(msg);
        if (msg.user != this.data.me.id) {
          thread.lastMsg.read = true;
          network.setRead(msg.user);
        }
      }
      this.redraw();
    } else network.loadThreads().then(data => {
      this.data.threads = data.threads;
      this.redraw();
    });
  }

  search = (q: string) => {
    if (q.length > 1) network.search(q).then((res: SearchRes) => {
      this.searchRes = res;
      this.redraw();
    });
    else {
      this.searchRes = undefined;
      this.redraw();
    }
  }

  block = () => {
    const userId = this.data.convo?.thread.contact.id;
    if (userId) network.block(userId).then(() => this.openConvo(userId));
  }

  unblock = () => {
    const userId = this.data.convo?.thread.contact.id;
    if (userId) network.unblock(userId).then(() => this.openConvo(userId));
  }
}
