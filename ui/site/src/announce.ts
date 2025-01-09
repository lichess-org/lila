import { escapeHtml } from 'common/string';
import { pubsub } from './pubsub';

let timeout: Timeout | undefined;
const kill = () => {
  if (timeout) clearTimeout(timeout);
  timeout = undefined;
  $('#announce').remove();
};
export const announce = (d: LishogiAnnouncement): void => {
  if (!d) return;
  kill();
  if (d.msg) {
    $('body')
      .append(
        '<div id="announce" class="announce">' +
          escapeHtml(d.msg) +
          (d.date ? '<time class="timeago" datetime="' + d.date + '"></time>' : '') +
          '<div class="actions"><a class="close">X</a></div>' +
          '</div>'
      )
      .find('#announce .close')
      .click(kill);
    timeout = setTimeout(kill, d.date ? new Date(d.date).getTime() - Date.now() : 5000);
    if (d.date) pubsub.emit('content_loaded');
  }
};
