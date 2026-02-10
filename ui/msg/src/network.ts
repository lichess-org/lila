import type MsgCtrl from './ctrl';
import type { MsgData, Contact, User, Msg, Convo, SearchResult } from './interfaces';
import { json, form } from 'lib/xhr';
import { pubsub } from 'lib/pubsub';

export async function loadConvo(userId: string): Promise<MsgData> {
  const d = await json(`/inbox/${userId}`);
  return upgradeData(d);
}

export async function getMore(userId: string, before: Date): Promise<MsgData> {
  const d = await json(`/inbox/${userId}?before=${before.getTime()}`);
  return upgradeData(d);
}

export async function loadContacts(): Promise<MsgData> {
  const d = await json(`/inbox`);
  return upgradeData(d);
}

export async function loadMoreContacts(before: Date): Promise<Contact[]> {
  const d = await json(`/inbox?before=${before.getTime()}`);
  return d.contacts.map(upgradeContact);
}

export async function search(q: string): Promise<SearchResult> {
  const res = await json(`/inbox/search?q=${q}`);
  return {
    ...res,
    contacts: res.contacts.map(upgradeContact),
  } as SearchResult;
}

export function block(u: string) {
  return json(`/api/rel/block/${u}?mini=1`, { method: 'post' });
}

export function unblock(u: string) {
  return json(`/api/rel/unblock/${u}?mini=1`, { method: 'post' });
}

export async function del(u: string): Promise<MsgData> {
  const d = await json(`/inbox/${u}`, { method: 'delete' });
  return upgradeData(d);
}

export function report(name: string, text: string): Promise<any> {
  return json('/report/flag', {
    method: 'post',
    body: form({
      username: name,
      text: text,
      resource: 'msg',
    }),
  });
}

export function post(dest: string, text: string) {
  pubsub.emit('socket.send', 'msgSend', { dest, text });
}

export function setRead(dest: string) {
  pubsub.emit('socket.send', 'msgRead', dest);
}

export function typing(dest: string) {
  pubsub.emit('socket.send', 'msgType', dest);
}

export function websocketHandler(ctrl: MsgCtrl) {
  pubsub.on('socket.in.msgNew', msg => {
    ctrl.receive({
      ...upgradeMsg(msg),
      read: false,
    });
  });
  pubsub.on('socket.in.msgType', ctrl.receiveTyping);
  pubsub.on('socket.in.blockedBy', ctrl.changeBlockBy);
  pubsub.on('socket.in.unblockedBy', ctrl.changeBlockBy);

  let connected = true;
  pubsub.on('socket.close', () => {
    connected = false;
    ctrl.redraw();
  });
  pubsub.on('socket.open', () => {
    if (!connected) {
      connected = true;
      ctrl.onReconnect();
    }
  });

  return () => connected;
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
