import { MsgData, MsgOpts, Thread, Msg, SearchRes, Redraw } from './interfaces';
import notify from 'common/notification';
import * as network from './network';

export default class MsgCtrl {

  data: MsgData;
  trans: Trans;
  searchRes?: SearchRes;

  constructor(opts: MsgOpts, readonly redraw: Redraw) {
    network.upgradeData(opts.data);
    this.data = opts.data;
    this.trans = window.lichess.trans(opts.i18n);
    network.websocketHandler(this);
    window.addEventListener('focus', this.setRead);
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
    const thread = this.findThread(msg.thread);
    if (thread) {
      thread.lastMsg = msg;
      // pull the thread to the top of the list
      this.data.threads = [thread].concat(this.data.threads.filter(t => t.id != thread.id));
      let redrawn = false;
      if (this.data.convo?.thread.id == thread.id) {
        this.data.convo.thread.lastMsg = msg;
        this.data.convo.msgs.unshift(msg);
        if (msg.user != this.data.me.id) {
          if (document.hasFocus()) redrawn = this.setRead();
          else this.notify(thread, msg);
        }
      }
      if (!redrawn) this.redraw();
    } else network.loadThreads().then(data => {
      this.data.threads = data.threads;
      this.notify(this.findThread(msg.thread), msg);
      this.redraw();
    });
  }

  private findThread = (id: string) => this.data.threads.filter(t => t.id == id)[0];

  private notify = (thread: Thread, msg: Msg) => {
    notify(() => `${thread.contact.name}: ${msg.text}`);
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

  setRead = () => {
    const msg = this.data.convo?.thread.lastMsg;
    if (msg && msg.user != this.data.me.id && !msg.read) {
      msg.read = true;
      network.setRead(msg.user);
      this.redraw();
      return true;
    }
    return false;
  }

  delete = () => {
    const userId = this.data.convo?.thread.contact.id;
    if (userId) network.del(userId).then(data => {
      this.data = data;
      this.redraw();
      history.replaceState({}, '', '/inbox');
    });
  }

  block = () => {
    const userId = this.data.convo?.thread.contact.id;
    if (userId) network.block(userId).then(() => this.openConvo(userId));
  }

  unblock = () => {
    const userId = this.data.convo?.thread.contact.id;
    if (userId) network.unblock(userId).then(() => this.openConvo(userId));
  }

  changeBlockBy = (userId: string) => {
    if (userId == this.data.convo?.thread.contact.id) this.openConvo(userId);
  }
}
