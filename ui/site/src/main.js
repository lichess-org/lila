const $friendsBox = $('#friend_box');

lichess.setSocketDefaults($friendsBox);

$(() => {

  lichess.moduleLaunchers();

  lichess.pubsub.on('content_loaded', lichess.miniBoard.initAll);
  lichess.pubsub.on('content_loaded', lichess.miniGame.initAll);

  lichess.requestIdleCallback(() => {

    $friendsBox.friends();

    $('#main-wrap')
      .on('click', '.autoselect', function() {
        $(this).select();
      })
      .on('click', 'button.copy', function() {
        $('#' + $(this).data('rel')).select();
        document.execCommand('copy');
        $(this).attr('data-icon', 'E');
      });

    $('body').on('click', 'a.relation-button', function() {
      var $a = $(this).addClass('processing').css('opacity', 0.3);
      $.ajax({
        url: $a.attr('href'),
        type: 'post',
        success: function(html) {
          if (html.includes('relation-actions')) $a.parent().replaceWith(html);
          else $a.replaceWith(html);
        }
      });
      return false;
    });

    $('.mselect .button').on('click', function() {
      const $p = $(this).parent();
      $p.toggleClass('shown');
      setTimeout(function() {
        const handler = function(e) {
          if ($.contains($p[0], e.target)) return;
          $p.removeClass('shown');
          $('html').off('click', handler);
        };
        $('html').on('click', handler);
      }, 10);
    });

    document.body.addEventListener('mouseover', lichess.powertip.mouseover);

    { // timeago
      const renderTimeago = () =>
        requestAnimationFrame(() =>
          lichess.timeago.render([].slice.call(document.getElementsByClassName('timeago'), 0, 99))
        );

      const setTimeago = interval => {
        renderTimeago();
        setTimeout(() => setTimeago(interval * 1.1), interval);
      }
      setTimeago(1200);
      lichess.pubsub.on('content_loaded', renderTimeago);
    }

    if (!window.customWS) setTimeout(() => {
      if (!lichess.socket)
        lichess.socket = lichess.StrongSocket("/socket/v5", false);
    }, 300);

    lichess.topBar();

    window.addEventListener('resize', () => lichess.dispatchEvent(document.body, 'chessground.resize'));

    $('.user-autocomplete').each(function() {
      const opts = {
        focus: 1,
        friend: $(this).data('friend'),
        tag: $(this).data('tag')
      };
      if ($(this).attr('autofocus')) lichess.userAutocomplete($(this), opts);
      else $(this).one('focus', function() {
        lichess.userAutocomplete($(this), opts);
      });
    });

    lichess.loadInfiniteScroll('.infinitescroll');

    $('a.delete, input.delete').click(() => confirm('Delete?'));
    $('input.confirm, button.confirm').click(function() {
      return confirm($(this).attr('title') || 'Confirm this action?');
    });

    $('#main-wrap').on('click', 'a.bookmark', function() {
      const t = $(this).toggleClass("bookmarked");
      $.post(t.attr("href"));
      const count = (parseInt(t.text(), 10) || 0) + (t.hasClass("bookmarked") ? 1 : -1);
      t.find('span').html(count > 0 ? count : "");
      return false;
    });

    // still bind esc even in form fields
    Mousetrap.prototype.stopCallback = function(e, el, combo) {
      return combo != 'esc' && (el.isContentEditable || el.tagName == 'INPUT' || el.tagName == 'SELECT' || el.tagName == 'TEXTAREA');
    };
    Mousetrap.bind('esc', function() {
      var $oc = $('#modal-wrap .close');
      if ($oc.length) $oc.trigger('click');
      else {
        var $input = $(':focus');
        if ($input.length) $input.trigger('blur');
      }
      return false;
    });

    if (!lichess.storage.get('grid')) setTimeout(() => {
      if (getComputedStyle(document.body).getPropertyValue('--grid'))
        lichess.storage.set('grid', 1);
      else
        $.get(lichess.assetUrl('oops/browser.html'), html => $('body').prepend(html))
    }, 3000);

    /* A disgusting hack for a disgusting browser
     * Edge randomly fails to rasterize SVG on page load
     * A different SVG must be loaded so a new image can be rasterized */
    if (navigator.userAgent.includes('Edge/')) setTimeout(() => {
      const sprite = $('#piece-sprite');
      sprite.attr('href', sprite.attr('href').replace('.css', '.external.css'));
    }, 1000);

    // prevent zoom when keyboard shows on iOS
    if (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) {
      const el = document.querySelector('meta[name=viewport]');
      el.setAttribute('content', el.getAttribute('content') + ',maximum-scale=1.0');
    }

    lichess.miniBoard.initAll();
    lichess.miniGame.initAll();

    $('.chat__members').watchers();

    if (location.hash === '#blind' && !$('body').hasClass('blind-mode'))
      $.post('/toggle-blind-mode', {
        enable: 1,
        redirect: '/'
      }, lichess.reload);

    lichess.serviceWorker();
  });
});
