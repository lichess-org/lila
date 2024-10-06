import { escapeHtml } from 'common';
import { pubsub } from 'common/pubsub';

let timeout: Timeout | undefined;

const kill = () => {
  if (timeout) clearTimeout(timeout);
  timeout = undefined;
  $('#announce').remove();
};

const announce = (d: LichessAnnouncement) => {
  kill();
  if (d.msg) {
    $('body')
      .append(
        '<div id="announce" class="announce">' +
          escapeHtml(d.msg) +
          (d.date ? '<time class="timeago" datetime="' + d.date + '"></time>' : '') +
          '<div class="actions"><a class="close">×</a></div>' +
          '</div>',
      )
      .find('#announce .close')
      .on('click', kill);
    const millis = d.date ? new Date(d.date).getTime() - Date.now() : 5000;
    if (millis > 0) timeout = setTimeout(kill, millis);
    else kill();
    if (d.date) pubsub.emit('content-loaded');
  }
};

export default announce;
