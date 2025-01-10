import type MsgCtrl from './ctrl';
import type { Contact, Convo, Msg, MsgData, SearchResult, User } from './interfaces';

export function loadConvo(userId: string): Promise<MsgData> {
  return window.lishogi.xhr.json('GET', `/inbox/${userId}`).then(upgradeData);
}

export function getMore(userId: string, before: Date): Promise<MsgData> {
  return window.lishogi.xhr
    .json('GET', `/inbox/${userId}`, {
      url: {
        before: before.getTime(),
      },
    })
    .then(upgradeData);
}

export function loadContacts(): Promise<MsgData> {
  return window.lishogi.xhr.json('GET', '/inbox').then(upgradeData);
}

export function search(q: string): Promise<SearchResult> {
  return window.lishogi.xhr
    .json('GET', '/inbox/search', {
      url: {
        q,
      },
    })
    .then(
      res =>
        ({
          ...res,
          contacts: res.contacts.map(upgradeContact),
        }) as SearchResult,
    );
}

export function block(u: string): Promise<any> {
  return window.lishogi.xhr.json('POST', `/rel/block/${u}`);
}

export function unblock(u: string): Promise<any> {
  return window.lishogi.xhr.json('POST', `/rel/unblock/${u}`);
}

export function del(u: string): Promise<MsgData> {
  return window.lishogi.xhr.json('DELETE', `/inbox/${u}`).then(upgradeData);
}

export function report(name: string, text: string): Promise<any> {
  return window.lishogi.xhr.json('POST', '/report/flag', {
    formData: {
      username: name,
      text: text,
      resource: 'msg',
    },
  });
}

export function post(dest: string, text: string): void {
  window.lishogi.pubsub.emit('socket.send', 'msgSend', { dest, text });
}

export function setRead(dest: string): void {
  window.lishogi.pubsub.emit('socket.send', 'msgRead', dest);
}

export function typing(dest: string): void {
  window.lishogi.pubsub.emit('socket.send', 'msgType', dest);
}

export function websocketHandler(ctrl: MsgCtrl) {
  const listen = window.lishogi.pubsub.on;
  listen('socket.in.msgNew', msg => {
    ctrl.receive({
      ...upgradeMsg(msg),
      read: false,
    });
  });
  listen('socket.in.msgType', ctrl.receiveTyping);
  listen('socket.in.blockedBy', ctrl.changeBlockBy);
  listen('socket.in.unblockedBy', ctrl.changeBlockBy);

  let connected = true;
  listen('socket.close', () => {
    connected = false;
    ctrl.redraw();
  });
  listen('socket.open', () => {
    if (!connected) {
      connected = true;
      ctrl.onReconnect();
    }
  });

  return (): boolean => connected;
}

// the upgrade functions convert incoming timestamps into JS dates
export function upgradeData(d: any): MsgData {
  return {
    ...d,
    convo: d.convo && upgradeConvo(d.convo),
    contacts: d.contacts.map(upgradeContact),
  };
}
function upgradeMsg(m: any): Msg {
  return {
    ...m,
    date: new Date(m.date),
  };
}
function upgradeUser(u: any): User {
  return {
    ...u,
    id: u.name.toLowerCase(),
  };
}
function upgradeContact(c: any): Contact {
  return {
    ...c,
    user: upgradeUser(c.user),
    lastMsg: upgradeMsg(c.lastMsg),
  };
}
function upgradeConvo(c: any): Convo {
  return {
    ...c,
    user: upgradeUser(c.user),
    msgs: c.msgs.map(upgradeMsg),
  };
}
