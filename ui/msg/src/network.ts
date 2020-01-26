import MsgCtrl from './ctrl';

const headers = {
  'Accept': 'application/vnd.lichess.v4+json'
};

export function loadConvo(userId: string) {
  return $.ajax({
    url: `/inbox/${userId}`,
    headers,
    cache: false
  }).then(d => {
    upgradeData(d);
    return d;
  });
}

export function loadContacts() {
  return $.ajax({
    url: `/inbox`,
    headers,
    cache: false
  }).then(d => {
    upgradeData(d);
    return d;
  });
}

export function search(q: string) {
  return $.ajax({
    url: '/inbox/search',
    data: { q }
  }).then(res => {
    res.contacts.forEach(upgradeContact);
    return res;
  });
}

export function block(u: string) {
  return $.ajax({
    url: `/rel/block/${u}`,
    method: 'post',
    headers
  });
}

export function unblock(u: string) {
  return $.ajax({
    url: `/rel/unblock/${u}`,
    method: 'post',
    headers
  });
}

export function del(u: string) {
  return $.ajax({
    url: `/inbox/${u}`,
    method: 'delete',
    headers
  }).then(res => {
    upgradeData(res);
    return res;
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
    upgradeMsg(msg);
    msg.read = false;
    ctrl.receiveMsg(msg);
  });
  listen('socket.in.blockedBy', ctrl.changeBlockBy);
  listen('socket.in.unblockedBy', ctrl.changeBlockBy);
}

// the upgrade functions convert incoming timestamps into JS dates
export function upgradeData(d: any) {
  if (d.convo) upgradeConvo(d.convo);
  d.contacts.forEach(upgradeContact);
}
function upgradeMsg(m: any) {
  m.date = new Date(m.date);
}
function upgradeContact(t: any) {
  if (t.lastMsg) upgradeMsg(t.lastMsg);
}
function upgradeConvo(c: any) {
  c.msgs.forEach(upgradeMsg);
}
