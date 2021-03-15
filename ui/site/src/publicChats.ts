import { text, form } from 'common/xhr';
import modal from 'common/modal';

lichess.load.then(() => {
  let autoRefreshEnabled = true;
  let autoRefreshOnHold = false;

  const renderButton = () =>
    $('.auto-refresh').toggleClass('active', autoRefreshEnabled).toggleClass('hold', autoRefreshOnHold);

  const reloadNow = () =>
    text('/mod/public-chat').then(html => {
      // Reload only the chat grid portions of the page
      $(html).find('#communication').appendTo($('#comm-wrap').empty());
      onPageReload();
    });

  const onPageReload = () => {
    $('#communication').append(
      $('<a class="auto-refresh button">Auto refresh</a>').on('click', () => {
        autoRefreshEnabled = !autoRefreshEnabled;
        renderButton();
      })
    );
    renderButton();

    $('#communication .chat').each(function () {
      this.scrollTop = 99999;
    });

    $('#communication')
      .on('mouseenter', '.chat', () => {
        autoRefreshOnHold = true;
        $('.auto-refresh').addClass('hold');
      })
      .on('mouseleave', '.chat', () => {
        autoRefreshOnHold = false;
        $('.auto-refresh').removeClass('hold');
      });

    $('#communication').on('click', '.line:not(.lichess)', function (this: HTMLDivElement) {
      const $l = $(this);
      const roomId = $l.parents('.game').data('room');
      const chan = $l.parents('.game').data('chan');
      const $wrap = modal($('.timeout-modal'));
      $wrap.find('.username').text($l.find('.user-link').text());
      $wrap.find('.text').text($l.text().split(' ').slice(1).join(' '));
      $wrap.on('click', '.button', function (this: HTMLButtonElement) {
        modal.close();
        text('/mod/public-chat/timeout', {
          method: 'post',
          body: form({
            roomId,
            chan,
            userId: $wrap.find('.username').text().toLowerCase(),
            reason: this.value,
            text: $wrap.find('.text').text(),
          }),
        }).then(_ => reloadNow());
      });
    });
  };
  onPageReload();

  setInterval(function () {
    if (!autoRefreshEnabled || document.visibilityState === 'hidden' || autoRefreshOnHold) return;
    reloadNow();
  }, 5000);
});
