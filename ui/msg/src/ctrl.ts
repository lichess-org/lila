import type {
  MsgData,
  Contact,
  Convo,
  Msg,
  LastMsg,
  Search,
  SearchResult,
  Typing,
  Pane,
  Redraw,
} from './interfaces';
import { throttle } from 'lib/async';
import * as network from './network';
import { scroller } from './view/scroller';
import { storage, type LichessStorage } from 'lib/storage';
import { pubsub } from 'lib/pubsub';

export default class MsgCtrl {
  data: MsgData;
  search: Search = {
    input: '',
  };
  pane: Pane;
  loading = false;
  connected = () => true;
  msgsPerPage = 100;
  canGetMoreSince?: Date;
  typing?: Typing;
  textStore?: LichessStorage;

  constructor(
    data: MsgData,
    readonly redraw: Redraw,
  ) {
    this.data = data;
    this.pane = data.convo ? 'convo' : 'side';
    this.connected = network.websocketHandler(this);
    if (this.data.convo) this.onLoadConvo(this.data.convo);
    window.addEventListener('focus', this.setRead);
  }

  openConvo = (userId: string) => {
    if (this.data.convo?.user.id !== userId) {
      this.data.convo = undefined;
      this.loading = true;
    }
    network.loadConvo(userId).then(data => {
      this.data = data;
      this.search.result = undefined;
      this.loading = false;
      if (data.convo) {
        history.replaceState({ contact: userId }, '', `/inbox/${data.convo.user.name}`);
        this.onLoadConvo(data.convo);
        this.redraw();
      } else this.showSide();
    });
    this.pane = 'convo';
    this.redraw();
  };

  showSide = () => {
    this.pane = 'side';
    this.redraw();
  };

  getMore = () => {
    if (this.data.convo && this.canGetMoreSince)
      network.getMore(this.data.convo.user.id, this.canGetMoreSince).then(data => {
        if (
          !this.data.convo ||
          !data.convo ||
          data.convo.user.id !== this.data.convo.user.id ||
          !data.convo.msgs[0]
        )
          return;
        if (data.convo.msgs[0].date >= this.data.convo.msgs[this.data.convo.msgs.length - 1].date) return;
        this.data.convo.msgs = this.data.convo.msgs.concat(data.convo.msgs);
        this.onLoadMsgs(data.convo.msgs);
        this.redraw();
      });
    this.canGetMoreSince = undefined;
    this.redraw();
  };

  private onLoadConvo = (convo: Convo) => {
    this.textStore = storage.make(`msg:area:${convo.user.id}`);
    this.onLoadMsgs(convo.msgs);
    if (this.typing) {
      clearTimeout(this.typing.timeout);
      this.typing = undefined;
    }
    setTimeout(this.setRead, 500);
  };
  private onLoadMsgs = (msgs: Msg[]) => {
    const oldFirstMsg = msgs[this.msgsPerPage - 1];
    this.canGetMoreSince = oldFirstMsg?.date;
  };

  post = (text: string) => {
    if (this.data.convo) {
      network.post(this.data.convo.user.id, text);
      const msg: LastMsg = {
        text,
        user: this.data.me.id,
        date: new Date(),
        read: true,
      };
      this.data.convo.msgs.unshift(msg);
      const contact = this.currentContact();
      if (contact) this.addMsg(msg, contact);
      else
        setTimeout(
          () =>
            network.loadContacts().then(data => {
              this.data.contacts = data.contacts;
              this.redraw();
            }),
          1000,
        );
      scroller.enable(true);
      this.redraw();
    }
  };

  receive = (msg: LastMsg) => {
    const contact = this.findContact(msg.user);
    this.addMsg(msg, contact);
    if (contact) {
      let redrawn = false;
      if (msg.user === this.data.convo?.user.id) {
        this.data.convo.msgs.unshift(msg);
        if (document.hasFocus()) redrawn = this.setRead();
        this.receiveTyping(msg.user, true);
      }
      if (!redrawn) this.redraw();
    } else
      network.loadContacts().then(data => {
        this.data.contacts = data.contacts;
        this.redraw();
      });
  };

  private addMsg = (msg: LastMsg, contact?: Contact) => {
    if (contact) {
      contact.lastMsg = msg;
      this.data.contacts = [contact].concat(this.data.contacts.filter(c => c.user.id !== contact.user.id));
    }
  };

  private findContact = (userId: string): Contact | undefined =>
    this.data.contacts.find(c => c.user.id === userId);

  private currentContact = (): Contact | undefined =>
    this.data.convo && this.findContact(this.data.convo.user.id);

  searchInput = (q: string) => {
    this.search.input = q;
    if (q.length > 2)
      network.search(q).then((res: SearchResult) => {
        this.search.result = this.search.input[1] ? res : undefined;
        this.redraw();
      });
    else {
      this.search.result = undefined;
      this.redraw();
    }
  };

  setRead = () => {
    const msg = this.currentContact()?.lastMsg;
    if (msg && msg.user !== this.data.me.id) {
      pubsub.emit('notify-app.set-read', msg.user);
      if (msg.read) return false;
      msg.read = true;
      network.setRead(msg.user);
      this.redraw();
      return true;
    }
    return false;
  };

  delete = () => {
    const userId = this.data.convo?.user.id;
    if (userId)
      network.del(userId).then(data => {
        this.data = data;
        this.redraw();
        history.replaceState({}, '', '/inbox');
      });
  };

  block = () => {
    const userId = this.data.convo?.user.id;
    if (userId) network.block(userId).then(() => this.openConvo(userId));
  };

  unblock = () => {
    const userId = this.data.convo?.user.id;
    if (userId) network.unblock(userId).then(() => this.openConvo(userId));
  };

  changeBlockBy = (userId: string) => {
    if (userId === this.data.convo?.user.id) this.openConvo(userId);
  };

  sendTyping = throttle(3000, (user: string) => {
    if (this.textStore?.get()) network.typing(user);
  });

  receiveTyping = (userId: string, cancel?: any) => {
    if (this.typing) {
      clearTimeout(this.typing.timeout);
      this.typing = undefined;
    }
    if (cancel !== true && this.data.convo?.user.id === userId) {
      this.typing = {
        user: userId,
        timeout: setTimeout(() => {
          if (this.data.convo?.user.id === userId) this.typing = undefined;
          this.redraw();
        }, 3000),
      };
    }
    this.redraw();
  };

  onReconnect = () => {
    this.data.convo && this.openConvo(this.data.convo.user.id);
    this.redraw();
  };
}
