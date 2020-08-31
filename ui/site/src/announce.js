lichess.announce = (() => {
  let timeout;
  const kill = () => {
    if (timeout) clearTimeout(timeout);
    timeout = undefined;
    $('#announce').remove();
  };
  const set = d => {
    if (!d) return;
    kill();
    if (d.msg) {
      $('body').append(
        '<div id="announce" class="announce">' +
        lichess.escapeHtml(d.msg) +
        (d.date ? '<time class="timeago" datetime="' + d.date + '"></time>' : '') +
        '<div class="actions"><a class="close">X</a></div>' +
        '</div>'
      ).find('#announce .close').click(kill);
      timeout = setTimeout(kill, d.date ? new Date(d.date) - Date.now() : 5000);
      if (d.date) lichess.pubsub.emit('content_loaded');
    }
  };
  set($('body').data('announce'));
  return set;
})();
