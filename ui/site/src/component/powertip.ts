{
  let hoverable;
  function isHoverable() {
    if (typeof hoverable === 'undefined')
      hoverable = !lichess.hasTouchEvents /* Firefox <= 63 */ || !!getComputedStyle(document.body).getPropertyValue('--hoverable');
    return hoverable;
  }

  const containedIn = (el, container) => container && container.contains(el);

  const inCrosstable = el => containedIn(el, document.querySelector('.crosstable'));

  function onPowertipPreRender(id, preload) {
    return function() {
      const url = ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '');
      if (preload) preload(url);
      $.ajax({
        url: url + '/mini',
        success: function(html) {
          $('#' + id).html(html);
          lichess.pubsub.emit('content_loaded');
        }
      });
    };
  };

  const uptA = (url, icon) => '<a class="btn-rack__btn" href="' + url + '" data-icon="' + icon + '"></a>';

  const userPowertip = (el, pos) => {
    pos = pos || el.getAttribute('data-pt-pos') || (
      inCrosstable(el) ? 'n' : 's'
    );
    $(el).removeClass('ulpt').powerTip({
      intentPollInterval: 200,
      placement: pos,
      smartPlacement: true,
      closeDelay: 200
    }).data('powertip', ' ').on({
      powerTipRender: onPowertipPreRender('powerTip', (url) => {
        const u = url.substr(3);
        const name = $(el).data('name') || $(el).html();
        $('#powerTip').html('<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' + name + '</span></div></div><div class="upt__actions btn-rack">' +
          uptA('/@/' + u + '/tv', '1') +
          uptA('/inbox/new?user=' + u, 'c') +
          uptA('/?user=' + u + '#friend', 'U') +
          '<a class="btn-rack__btn relation-button" disabled></a></div>');
      })
    });
  };

  function gamePowertip(el) {
    $(el).removeClass('glpt').powerTip({
      intentPollInterval: 200,
      placement: inCrosstable(el) ? 'n' : 'w',
      smartPlacement: true,
      closeDelay: 200,
      popupId: 'miniGame'
    }).on({
      powerTipPreRender: onPowertipPreRender('miniGame')
    }).data('powertip', lichess.spinnerHtml);
  };

  function powerTipWith(el, ev, f) {
    if (lichess.isHoverable()) {
      f(el);
      $.powerTip.show(el, ev);
    }
  };

  function onIdleForAll(par, sel, fun) {
    lichess.requestIdleCallback(() =>
      Array.prototype.forEach.call(par.querySelectorAll(sel), el => fun(el)) // do not codegolf to `fun`
    )
  }

  lichess.powertip = {
    mouseover(e) {
      var t = e.target,
        cl = t.classList;
      if (cl.contains('ulpt')) powerTipWith(t, e, userPowertip);
      else if (cl.contains('glpt')) powerTipWith(t, e, gamePowertip);
    },
    manualGameIn(parent) {
      onIdleForAll(parent, '.glpt', gamePowertip);
    },
    manualGame: gamePowertip,
    manualUser: userPowertip,
    manualUserIn(parent) {
      onIdleForAll(parent, '.ulpt', userPowertip);
    }
  };
}
