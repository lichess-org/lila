import MsgCtrl from './ctrl';
import { MsgData, Contact, User, Msg, Convo, SearchRes } from './interfaces';

const headers: HeadersInit = {
  'Accept': 'application/vnd.lichess.v5+json'
};
const cache: RequestCache = 'no-cache';

export function loadConvo(userId: string): Promise<MsgData> {
  return fetch(`/inbox/${userId}`, { headers, cache })
    .then(r => r.json())
    .then(upgradeData);
}

export function loadContacts(): Promise<MsgData> {
  return fetch(`/inbox`, { headers, cache })
    .then(r => r.json())
    .then(upgradeData);
}

export function search(q: string): Promise<SearchRes> {
  return fetch(`/inbox/search?q=${q}`)
    .then(r => r.json())
    .then(res => ({
      ...res,
      contacts: res.contacts.map(upgradeContact)
    } as SearchRes));
}

export function block(u: string) {
  return fetch(`/rel/block/${u}`, {
    method: 'post',
    headers
  });
}

export function unblock(u: string) {
  return fetch(`/rel/unblock/${u}`, {
    method: 'post',
    headers
  });
}

export function del(u: string): Promise<MsgData> {
  return fetch(`/inbox/${u}`, {
    method: 'delete',
    headers
  })
    .then(r => r.json())
    .then(upgradeData);
}

export function report(name: string, text: string): Promise<any> {
  const formData = new FormData()
  formData.append('username', name);
  formData.append('text', text);
  formData.append('resource', 'msg');
  return fetch('/report/flag', {
    method: 'post',
    headers,
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
  listen('socket.close', () => { connected = false });
  listen('socket.open', () => {
    if (!connected) {
      connected = true;
      ctrl.onReconnect();
    }
  });
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
