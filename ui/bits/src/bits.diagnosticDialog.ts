import { isTouchDevice } from 'lib/device';
import { domDialog } from 'lib/view';
import * as licon from 'lib/licon';
import { escapeHtml, myUserId } from 'lib';
import { storage } from 'lib/storage';
import { log } from 'lib/permalog';

interface DiagnosticOpts {
  text: string;
  header?: string;
  submit?: string;
  plaintext?: boolean;
}

export async function initModule(opts?: DiagnosticOpts): Promise<void> {
  const ops = opts ? 0 : processQueryParams();
  const logs = !opts && (await log.get());
  const text =
    opts?.text ??
    `Browser: ${navigator.userAgent}\n` +
      ('userAgentData' in navigator
        //@ts-ignore userAgentData not documented in TypeScript https://developer.mozilla.org/en-US/docs/Web/API/Navigator/userAgentData
        ? `Brand: "${navigator.userAgentData.brands.map(b => `${b.brand} ${b.version}`).join('; ')}", `
        : '') +
      `Cores: ${navigator.hardwareConcurrency}, ` +
      `Touch: ${isTouchDevice()} ${navigator.maxTouchPoints}, ` +
      `Screen: ${window.screen.width}x${window.screen.height}, ` +
      ('lichessTools' in window ? 'Extension: Lichess Tools, ' : '') +
      `Page lang: ${site.displayLocale}, ` +
      `Browser lang: ${navigator.language}, ` +
      `Engine: ${storage.get('ceval.engine')}, ` +
      `Threads: ${storage.get('ceval.threads')}, ` +
      `Blindfold: ${storage.boolean('blindfold.' + (myUserId() || 'anon')).get()}, ` +
      `Pieces: ${document.body.dataset.pieceSet}` +
      (logs ? `\n\n${logs}` : '');
  const escaped = escapeHtml(text);
  const flash = ops > 0 ? `<p class="good">Changes applied</p>` : '';
  const submit = myUserId()
    ? $html`
      <form method="post" action="/diagnostic">
        <input type="hidden" name="text" value="${escaped}"/>
        <input type="hidden" name="plaintext" value="${opts?.plaintext ?? false}"/>
        <button type="submit" class="button">${opts?.submit ?? 'send to lichess'}</button>
      </form>`
    : '';
  const clear = logs ? `<button class="button button-empty button-red clear">clear logs</button>` : '';
  const copy = `<button class="button copy" data-icon="${licon.Clipboard}"> copy</button>`;
  const dlg = await domDialog({
    class: 'diagnostic',
    css: [{ hashed: 'bits.diagnosticDialog' }],
    modal: true,
    focus: '.copy',
    htmlText: $html`
      <h2>${opts?.header ?? 'Diagnostics'}</h2>${flash}
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
  $('.clear', dlg.view).on('click', () => log.clear().then(() => dlg.close()));
  $('.copy', dlg.view).on('click', () =>
    navigator.clipboard.writeText(text).then(() => {
      const copied = $(`<div data-icon="${licon.Checkmark}" class="good"> COPIED</div>`);
      $('.copy', dlg.view).before(copied);
      setTimeout(() => copied.remove(), 2000);
    }),
  );
  dlg.show();
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
        log(`storage set ${kv[0]}=${kv[1]}`);
        storage.set(proxy.storageKey, kv[1]);
        return true;
      }
    } catch (_) {
      console.warn('Invalid base64', data);
    }
    return false;
  },
  unset: (val: string) => {
    log(`storage unset ${val ? val : 'all'}`);

    if (!val) for (const key in storageProxy) storage.remove(storageProxy[key].storageKey);
    else if (val in storageProxy) storage.remove(storageProxy[val].storageKey);
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
