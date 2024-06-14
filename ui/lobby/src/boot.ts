export default function boot(cfg, element) {
  let lobby;
  const nbRoundSpread = spreadNumber('#nb_games_in_play > strong', 8),
    nbUserSpread = spreadNumber('#nb_connected_players > strong', 10),
    getParameterByName = name => {
      var match = RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
      return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
    },
    onFirstConnect = () => {
      var gameId = getParameterByName('hook_like');
      if (!gameId) return;
      $.post(`/setup/hook/${window.lishogi.sri}/like/${gameId}?rr=${lobby.setup.ratingRange() || ''}`);
      lobby.setTab('real_time', false);
      history.replaceState(null, '', '/');
    };
  window.lishogi.socket = window.lishogi.StrongSocket('/lobby/socket/v4', false, {
    receive: function (t, d) {
      lobby.socketReceive(t, d);
    },
    events: {
      n: function (_nbUsers, msg) {
        nbUserSpread(msg.d);
        setTimeout(function () {
          nbRoundSpread(msg.r);
        }, window.lishogi.socket.pingInterval() / 2);
      },
      reload_timeline: function () {
        $.ajax({
          url: '/timeline',
          success: function (html) {
            $('.timeline').html(html);
            window.lishogi.pubsub.emit('content_loaded');
          },
        });
      },
      featured: function (o) {
        $('.lobby__tv').html(o.html);
        window.lishogi.pubsub.emit('content_loaded');
      },
      redirect: function (e) {
        lobby.setRedirecting();
        window.lishogi.redirect(e);
      },
      tournaments: function (data) {
        $('#enterable_tournaments').html(data);
        window.lishogi.pubsub.emit('content_loaded');
      },
      sfen: function (e) {
        window.lishogi.StrongSocket.defaults.events.sfen(e);
        lobby.gameActivity(e.id);
      },
    },
    options: {
      name: 'lobby',
      onFirstConnect: onFirstConnect,
    },
  });

  cfg.blindMode = $('body').hasClass('blind-mode');
  cfg.trans = window.lishogi.trans(cfg.i18n);
  cfg.socketSend = window.lishogi.socket.send;
  cfg.element = element;
  cfg.variant = getParameterByName('variant');
  cfg.sfen = getParameterByName('sfen');

  lobby = window.LishogiLobby.start(cfg);

  const $startButtons = $('.lobby__start'),
    clickEvent = cfg.blindMode ? 'click' : 'mousedown';

  $startButtons
    .find('a:not(.disabled)')
    .on(clickEvent, function () {
      $(this).addClass('active').siblings().removeClass('active');
      window.lishogi.loadCssPath('lobby.setup');
      let url = (this as HTMLAnchorElement).href;
      if (this.dataset.hrefAddon) {
        url += this.dataset.hrefAddon;
        delete this.dataset.hrefAddon;
      }
      $.ajax({
        url: url,
        success: function (html) {
          lobby.setup.prepareForm(
            $.modal(html, 'game-setup', () => {
              $startButtons.find('.active').removeClass('active');
            })
          );
          window.lishogi.pubsub.emit('content_loaded');
        },
        error: function (res) {
          if (res.status == 400) alert(res.responseText);
          window.lishogi.reload();
        },
      });
      return false;
    })
    .on('click', function () {
      return false;
    });

  if (['#ai', '#friend', '#hook'].includes(location.hash)) {
    $startButtons
      .find('.config_' + location.hash.replace('#', ''))
      .each(function () {
        this.dataset.hrefAddon = location.search;
      })
      .trigger(clickEvent);

    if (location.hash === '#hook') {
      if (/time=realTime/.test(location.search)) lobby.setTab('real_time');
      else if (/time=correspondence/.test(location.search)) lobby.setTab('seeks');
    }

    history.replaceState(null, '', '/');
  }
}

function spreadNumber(selector: string, nbSteps: number) {
  const el = document.querySelector(selector) as HTMLElement;
  let previous = parseInt(el.getAttribute('data-count')!);
  const display = function (prev, cur, it) {
    el.textContent = window.lishogi.numberFormat(Math.round((prev * (nbSteps - 1 - it) + cur * (it + 1)) / nbSteps));
  };
  let timeouts: number[] = [];
  return function (nb, overrideNbSteps?) {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    const interv = Math.abs(window.lishogi.socket.pingInterval() / nbSteps);
    const prev = previous || nb;
    previous = nb;
    for (let i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
  };
}
