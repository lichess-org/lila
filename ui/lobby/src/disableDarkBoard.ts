import { domDialog } from 'common/dialog';
import { isChrome } from 'common/device';
import { load as loadDasher } from 'dasher';

export default function disableDarkBoard() {
  if (!document.body.classList.contains('dark-board') || !isChrome() || !lichess.once('disableDarkBoard'))
    return;
  loadDasher().then(m => m.subs.background.set('dark'));
  domDialog({
    htmlText:
      '<div><h2>Dark board theme disabled</h2>' +
      '<hr>' +
      '<p>Due to a new Chrome bug also affecting Edge, the dark board theme has been temporarily disabled.</p>' +
      '<p>It was causing visual artifacts such as missing pieces.</p>' +
      '<p>You can re-enable the dark board theme from your preference menu if you wish.</p>' +
      '<hr><button class="button dbtd-close" style="padding: 1em 5em">OK</button>' +
      '</div>',
    show: 'modal',
    action: {
      selector: 'button.dbtd-close',
    },
  });
}
