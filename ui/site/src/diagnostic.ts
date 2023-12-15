import { isTouchDevice } from 'common/device';
import { domDialog } from 'common/dialog';

export default async function initModule() {
  const logs = await lichess.log.get();
  const text =
    `Browser: ${navigator.userAgent}\n` +
    `Cores: ${navigator.hardwareConcurrency}\n` +
    `Touch: ${isTouchDevice()} ${navigator.maxTouchPoints}\n` +
    `Screen: ${window.screen.width}x${window.screen.height}\n` +
    `Lang: ${navigator.language}` +
    (logs ? `\n\n${logs}` : '');

  const dlg = await domDialog({
    class: 'diagnostic',
    cssPath: 'diagnostic',
    htmlText:
      `<h2>Diagnostics</h2><pre tabindex="0" class="err">${lichess.escapeHtml(text)}</pre>` +
      (logs ? `<button class="clear button">Clear Logs</button>` : ''),
  });
  const select = () =>
    setTimeout(() => {
      const range = document.createRange();
      range.selectNodeContents(dlg.view.querySelector('.err')!);
      window.getSelection()?.removeAllRanges();
      window.getSelection()?.addRange(range);
    }, 0);
  $('.err', dlg.view).on('focus', select);
  $('.clear', dlg.view).on('click', () => lichess.log.clear().then(lichess.reload));
  dlg.showModal();
}
