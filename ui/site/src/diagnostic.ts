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
    (logs ? `\n\n${logs}` : '');

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

function processQueryParams() {
  let changed = 0;
  for (const p of location.hash.split('?')[1]?.split('&') ?? []) {
    const op = p.indexOf('=') > -1 ? p.slice(0, p.indexOf('=')) : p;
    if (op in operations) changed += operations[op](p.slice(op.length + 1));
    else console.error('Invalid query op', op);
  }
  return changed;
}

const operations: { [op: string]: (val?: string) => number } = {
  reset: (val: string) => {
    if (!val) {
      lichess.storage.remove('socket.ping.interval');
      lichess.storage.remove('socket.host');
      return 2;
    } else if (val === 'wsPing') lichess.storage.remove('socket.ping.interval');
    else if (val === 'wsHost') lichess.storage.remove('socket.host');
    else return 0;
    return 1;
  },
  set: (data: string) => {
    try {
      const kv = atob(data).split('=');
      if (kv[0] === 'wsPing') {
        const interval = parseInt(kv[1]);
        if (interval > 249) {
          lichess.storage.set('socket.ping.interval', `${interval}`);
          return 1;
        }
      } else if (kv[0] === 'wsHost' && kv[1]?.length > 4) {
        lichess.storage.set('socket.host', kv[1]);
        return 1;
      }
      console.error('Invalid query set payload', kv);
    } catch (_) {
      console.error('Invalid base64', data);
    }
    return 0;
  },
};
