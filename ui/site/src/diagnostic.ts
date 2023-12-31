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

  processQueryParams();

  const dlg = await domDialog({
    class: 'diagnostic',
    cssPath: 'diagnostic',
    htmlText:
      `<h2>Diagnostics</h2><pre tabindex="0" class="err">${lichess.escapeHtml(text)}</pre>` +
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
  for (const p of location.hash.split('?')[1]?.split('&') ?? []) {
    const [op, value] = [p.slice(0, p.indexOf('=')), p.slice(p.indexOf('=') + 1)];
    if (op !== 'set' || !value) continue;
    const kvPair = atob(value).split('=');
    if (kvPair[0] === 'pingInterval') {
      const interval = parseInt(kvPair[1]);
      if (interval > 249) lichess.storage.set('socket.ping.interval', `${interval}`);
    }
  }
}
