import MsgCtrl from './ctrl';
import { MsgData, Contact, User, Msg, Convo, SearchResult } from './interfaces';

const headers: HeadersInit = {
  'Accept': 'application/vnd.lichess.v5+json'
};
const cache: RequestCache = 'no-cache';
const credentials = 'same-origin';

function xhr(url: string, init: RequestInit = {}): Promise<any> {
  return fetch(url, {
    headers,
    cache,
    credentials,
    ...init
  }).then(httpResponse);
}

export function loadConvo(userId: string): Promise<MsgData> {
  return xhr(`/inbox/${userId}`).then(upgradeData);
}

export function loadContacts(): Promise<MsgData> {
  return xhr(`/inbox`).then(upgradeData);
}

export function search(q: string): Promise<SearchResult> {
  return xhr(`/inbox/search?q=${q}`)
    .then(res => ({
      ...res,
      contacts: res.contacts.map(upgradeContact)
    } as SearchResult));
}

export function block(u: string) {
  return xhr(`/rel/block/${u}`, { method: 'post' });
}

export function unblock(u: string) {
  return xhr(`/rel/unblock/${u}`, { method: 'post' });
}

export function del(u: string): Promise<MsgData> {
  return xhr(`/inbox/${u}`, { method: 'delete' })
    .then(upgradeData);
}

export function report(name: string, text: string): Promise<any> {
  const formData = new FormData()
  formData.append('username', name);
  formData.append('text', text);
  formData.append('resource', 'msg');
  return xhr('/report/flag', {
    method: 'post',
    body: formData
  });
}

export function post(dest: string, text: string) {
  window.lichess.pubsub.emit('socket.send', 'msgSend', { dest, text });
}

export function setRead(dest: string) {
  window.lichess.pubsub.emit('socket.send', 'msgRead', dest);
}

export function websocketHandler(ctrl: MsgCtrl) {
  const listen = window.lichess.pubsub.on;
  listen('socket.in.msgNew', msg => {
    ctrl.receive({
      ...upgradeMsg(msg),
      read: false
    });
  });
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

  return () => connected;
}

function httpResponse(response: Response) {
  if (response.ok) return response.json();
  alert(response.statusText);
  throw response.statusText;
}

// the upgrade functions convert incoming timestamps into JS dates
export function upgradeData(d: any): MsgData {
  return {
    ...d,
    convo: d.convo && upgradeConvo(d.convo),
    contacts: d.contacts.map(upgradeContact)
  };
}
function upgradeMsg(m: any): Msg {
  return {
    ...m,
    date: new Date(m.date)
  };
}
function upgradeUser(u: any): User {
  return {
    ...u,
    id: u.name.toLowerCase()
  };
}
function upgradeContact(c: any): Contact {
  return {
    ...c,
    user: upgradeUser(c.user),
    lastMsg: upgradeMsg(c.lastMsg)
  };
}
function upgradeConvo(c: any): Convo {
  return {
    ...c,
    user: upgradeUser(c.user),
    msgs: c.msgs.map(upgradeMsg)
  };
}
