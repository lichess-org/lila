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

export function post(userId: string, text: string) {
  return $.ajax({
    url: `/inbox/${userId}`,
    method: 'post',
    data: { text }
  }).then(msg => {
    upgradeMsg(msg);
    return msg;
  });
}

export function upgradeMsg(m: any) {
  m.date = new Date(m.date);
}
export function upgradeThread(t: any) {
  if (t.lastMsg) upgradeMsg(t.lastMsg);
}
export function upgradeConvo(c: any) {
  c.msgs.forEach(upgradeMsg);
  upgradeThread(c.thread);
}
export function upgradeData(d: any) {
  if (d.convo) upgradeConvo(d.convo);
  d.threads.forEach(upgradeThread);
}
