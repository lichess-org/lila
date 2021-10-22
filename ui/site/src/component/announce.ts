import { escapeHtml } from './functions';

let timeout: Timeout | undefined;

const kill = () => {
  if (timeout) clearTimeout(timeout);
  timeout = undefined;
  $('#announce').remove();
};

const announce = (d: LichessAnnouncement, i18n?: I18nDict) => {
  kill();
  const msg = (d.i18nKey && i18n?.[d.i18nKey]) ?? d.msg;

  if (msg) {
    $('body')
      .append(
        '<div id="announce" class="announce">' +
          escapeHtml(msg) +
          (d.date ? '<time class="timeago" datetime="' + d.date + '"></time>' : '') +
          '<div class="actions"><a class="close">×</a></div>' +
          '</div>'
      )
      .find('#announce .close')
      .on('click', kill);
    timeout = setTimeout(kill, d.date ? new Date(d.date).getTime() - Date.now() : 5000);
    if (d.date) lichess.contentLoaded();
  }
};

export default announce;
