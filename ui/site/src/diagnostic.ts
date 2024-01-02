import { isTouchDevice } from 'common/device';
import { domDialog } from 'common/dialog';
import * as licon from 'common/licon';

export default async function initModule() {
  const logs = await lichess.log.get();
  const text =
    `Browser: ${navigator.userAgent}\n` +
    `Cores: ${navigator.hardwareConcurrency}, ` +
    `Touch: ${isTouchDevice()} ${navigator.maxTouchPoints}, ` +
    `Screen: ${window.screen.width}x${window.screen.height}, ` +
    `Lang: ${navigator.language}\n` +
    `Engine: ${lichess.storage.get('ceval.engine')}, ` +
    `Threads: ${lichess.storage.get('ceval.threads')}` +
    `\n\n${logs ?? ''}`.trim();

  const ops = processQueryParams();
  const flash = ops > 0 ? `<p class="good">${ops} settings applied</p>` : '';
  const dlg = await domDialog({
    class: 'diagnostic',
    cssPath: 'diagnostic',
    htmlText:
      `<h2>Diagnostics</h2>${flash}<pre tabindex="0" class="err">${lichess.escapeHtml(text)}</pre>` +
      '<span><button class="copy button">copy to clipboard</button>' +
      (logs
        ? '&nbsp;&nbsp;<button class="clear button button-empty button-red">clear logs</button></span>'
        : '</span>'),
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
    navigator.clipboard
      .writeText(text)
      .then(() =>
        $('.copy', dlg.view).replaceWith($(`<span>COPIED <i data-icon='${licon.Checkmark}'/></span>`)),
      ),
  );
  dlg.showModal();
}

const storageProxy: { [key: string]: (val?: string) => string } = {
  wsPing: (val?: string) => {
    const storageKey = 'socket.ping.interval';
    if (val === undefined) return storageKey;
    if (parseInt(val) > 249) {
      lichess.storage.set(storageKey, val);
      return val;
    }
    return '';
  },
  wsHost: (val?: string) => {
    const storageKey = 'socket.host';
    if (val === undefined) return storageKey;
    lichess.storage.set(storageKey, val);
    return val;
  },
  forceLSFW: (val?: string) => {
    const storageKey = 'ceval.lsfw.forceEnable';
    if (val === undefined) return storageKey;
    lichess.storage.set(storageKey, val);
    return val;
  },
};

const ops: { [op: string]: (val?: string) => number } = {
  reset: (val: string) => {
    if (!val) {
      for (const key in storageProxy) lichess.storage.remove(storageProxy[key]?.());
      return Object.keys(storageProxy).length;
    } else if (val in storageProxy) {
      lichess.storage.remove(storageProxy[val]?.());
      return 1;
    }
    return 0;
  },
  set: (data: string) => {
    let changed = 0;
    try {
      const kv = atob(data).split('=');
      changed = storageProxy[kv[0]]?.(kv[1] ?? '') ? 1 : 0;
      if (!changed) console.warn(`Invalid set payload '${data}'`, kv);
    } catch (_) {
      console.warn('Invalid base64', data);
    }
    return changed;
  },
};

function processQueryParams() {
  let changed = 0;
  for (const p of location.hash.split('?')[1]?.split('&') ?? []) {
    const op = p.indexOf('=') > -1 ? p.slice(0, p.indexOf('=')) : p;
    if (op in ops) changed += ops[op](p.slice(op.length + 1));
    else console.warn('Invalid query op', op);
  }
  return changed;
}
