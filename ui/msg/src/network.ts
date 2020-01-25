import { Msg } from './interfaces';

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

export function loadThreads() {
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
    res.threads.forEach(upgradeThread);
    return res;
  });
}

export function post(dest: string, text: string) {
  window.lichess.pubsub.emit('socket.send', 'msgSend', { dest, text });
}

export function setRead(dest: string) {
  window.lichess.pubsub.emit('socket.send', 'msgRead', dest);
}

export function onMsgNew(f: (msg: Msg) => void) {
  window.lichess.pubsub.on('socket.in.msgNew', msg => {
    upgradeMsg(msg);
    f(msg);
  });
}

// the upgrade functions convert incoming timestamps into JS dates
export function upgradeData(d: any) {
  if (d.convo) upgradeConvo(d.convo);
  d.threads.forEach(upgradeThread);
}
function upgradeMsg(m: any) {
  m.date = new Date(m.date);
}
function upgradeThread(t: any) {
  if (t.lastMsg) upgradeMsg(t.lastMsg);
}
function upgradeConvo(c: any) {
  c.msgs.forEach(upgradeMsg);
  upgradeThread(c.thread);
}
