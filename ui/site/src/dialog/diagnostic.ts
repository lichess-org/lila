import { isTouchDevice } from 'common/device';
import * as licon from 'common/licon';

export default async function initModule() {
  const ops = processQueryParams();
  const logs = await lichess.log.get();
  const text =
    `Browser: ${navigator.userAgent}\n` +
    `Cores: ${navigator.hardwareConcurrency}, ` +
    `Touch: ${isTouchDevice()} ${navigator.maxTouchPoints}, ` +
    `Screen: ${window.screen.width}x${window.screen.height}, ` +
    `Lang: ${navigator.language}, ` +
    `Engine: ${lichess.storage.get('ceval.engine')}, ` +
    `Threads: ${lichess.storage.get('ceval.threads')}` +
    (logs ? `\n\n${logs}` : '');
  const escaped = lichess.escapeHtml(text);
  const flash = ops > 0 ? `<p class="good">Changes applied</p>` : '';
  const submit = document.body.dataset.user
    ? `<form method="post" action="/diagnostic">
      <input type="hidden" name="text" value="${escaped}"/>
      <button type="submit" class="button">send to lichess</button></form>`
    : '';
  const clear = logs ? `<button class="button button-empty button-red">clear logs</button>` : '';
  const copy = `<button class="button copy" data-icon="${licon.Clipboard}"> copy</button>`;
  const dlg = await lichess.dialog.dom({
    class: 'diagnostic',
    css: [{ themed: 'diagnosticDialog' }],
    htmlText: `
      <h2>Diagnostics</h2>${flash}
      <pre tabindex="0" class="err">${escaped}</pre>
      <span class="actions"> ${clear} <div class="spacer"></div> ${copy} ${submit} </span>`,
  });
  const select = () =>
    setTimeout(() => {
      const range = document.createRange();
      range.selectNodeContents(dlg.view.querySelector('.err')!);
      window.getSelection()?.removeAllRanges();
      window.getSelection()?.addRange(range);
    }, 0);
  $('.err', dlg.view).on('focus', select);
  $('.clear', dlg.view).on('click', () => lichess.log.clear().then(dlg.close));
  $('.copy', dlg.view).on('click', () =>
    navigator.clipboard.writeText(text).then(() => {
      const copied = $(`<div data-icon="${licon.Checkmark}" class="good"> COPIED</div>`);
      $('.copy', dlg.view).before(copied);
      setTimeout(() => copied.remove(), 2000);
    }),
  );
  dlg.showModal();
}

const storageProxy: { [key: string]: { storageKey: string; validate: (val?: string) => boolean } } = {
  wsPing: {
    storageKey: 'socket.ping.interval',
    validate: (val?: string) => parseInt(val ?? '') > 249,
  },
  wsHost: {
    storageKey: 'socket.host',
    validate: (val?: string) => val?.endsWith('.lichess.org') ?? false,
  },
  allowLsfw: {
    storageKey: 'ceval.lsfw.forceEnable',
    validate: (val?: string) => val === 'true' || val === 'false',
  },
  logWindow: {
    storageKey: 'log.window',
    validate: (val?: string) => parseInt(val ?? '') >= 0,
  },
};

const ops: { [op: string]: (val?: string) => boolean } = {
  set: (data: string) => {
    try {
      const kv = atob(data).split('=');
      const proxy = storageProxy[kv[0]];
      if (proxy?.validate(kv[1])) {
        lichess.log(`storage set ${kv[0]}=${kv[1]}`);
        lichess.storage.set(proxy.storageKey, kv[1]);
        return true;
      }
    } catch (_) {
      console.warn('Invalid base64', data);
    }
    return false;
  },
  unset: (val: string) => {
    lichess.log(`storage unset ${val ? val : 'all'}`);

    if (!val) for (const key in storageProxy) lichess.storage.remove(storageProxy[key].storageKey);
    else if (val in storageProxy) lichess.storage.remove(storageProxy[val].storageKey);
    else return false;
    return true;
  },
};

function processQueryParams() {
  let changed = 0;
  for (const p of location.hash.split('?')[1]?.split('&') ?? []) {
    const op = p.indexOf('=') > -1 ? p.slice(0, p.indexOf('=')) : p;
    if (op in ops) changed += ops[op](p.slice(op.length + 1)) ? 1 : 0;
    else console.warn('Invalid query op', op);
  }
  return changed;
}
