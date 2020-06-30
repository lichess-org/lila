module.exports = function(cfg, element) {
  cfg.pools = [ // mirrors modules/pool/src/main/PoolList.scala
    {id:"1+0",lim:1,inc:0,perf:"Bullet"},
    {id:"2+1",lim:2,inc:1,perf:"Bullet"},
    {id:"3+0",lim:3,inc:0,perf:"Blitz"},
    {id:"3+2",lim:3,inc:2,perf:"Blitz"},
    {id:"5+0",lim:5,inc:0,perf:"Blitz"},
    {id:"5+3",lim:5,inc:3,perf:"Blitz"},
    {id:"10+0",lim:10,inc:0,perf:"Rapid"},
    {id:"10+5",lim:10,inc:5,perf:"Rapid"},
    {id:"15+10",lim:15,inc:10,perf:"Rapid"},
    {id:"30+0",lim:30,inc:0,perf:"Classical"},
    {id:"30+20",lim:30,inc:20,perf:"Classical"}
  ];
  let lobby;
  const nbRoundSpread = spreadNumber(
    document.querySelector('#nb_games_in_play > strong'),
    8,
    function() {
      return lichess.socket.pingInterval();
    }),
  nbUserSpread = spreadNumber(
    document.querySelector('#nb_connected_players > strong'),
    10,
    function() {
      return lichess.socket.pingInterval();
    }),
  getParameterByName = function(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
  },
  onFirstConnect = function() {
    var gameId = getParameterByName('hook_like');
    if (!gameId) return;
    $.post(`/setup/hook/${lichess.sri}/like/${gameId}?rr=${lobby.setup.ratingRange() || ''}`);
    lobby.setTab('real_time');
    history.replaceState(null, null, '/');
  },
  filterStreams = function() {
    var langs = navigator.languages;
    if (!langs) return; // tss... https://developer.mozilla.org/en-US/docs/Web/API/NavigatorLanguage/languages
    langs = langs.map(function(l) {
      return l.slice(0, 2).toLowerCase();
    });
    langs.push($('html').attr('lang'));
    $('.lobby__streams a, .event-spotlight').each(function() {
      var match = $(this).text().match(/\[(\w{2})\]/mi);
      if (match && !langs.includes(match[1].toLowerCase())) $(this).hide();
    });
  };
  filterStreams();
  lichess.socket = lichess.StrongSocket(
    '/lobby/socket/v4',
    false, {
      receive: function(t, d) {
        lobby.socketReceive(t, d);
      },
      events: {
        n: function(nbUsers, msg) {
          nbUserSpread(msg.d);
          setTimeout(function() {
            nbRoundSpread(msg.r);
          }, lichess.socket.pingInterval() / 2);
        },
        reload_timeline: function() {
          $.ajax({
            url: '/timeline',
            success: function(html) {
              $('.timeline').html(html);
              lichess.pubsub.emit('content_loaded');
            }
          });
        },
        streams: function(html) {
          $('.lobby__streams').html(html);
          filterStreams();
        },
        featured: function(o) {
          $('.lobby__tv').html(o.html);
          lichess.pubsub.emit('content_loaded');
        },
        redirect: function(e) {
          lobby.leavePool();
          lobby.setRedirecting();
          window.lichess.redirect(e);
        },
        tournaments: function(data) {
          $("#enterable_tournaments").html(data);
          lichess.pubsub.emit('content_loaded');
        },
        simuls: function(data) {
          $("#enterable_simuls").html(data).parent().toggle($('#enterable_simuls tr').length > 0);
          lichess.pubsub.emit('content_loaded');
        },
        fen: function(e) {
          lichess.StrongSocket.defaults.events.fen(e);
          lobby.gameActivity(e.id);
        }
      },
      options: {
        name: 'lobby',
        onFirstConnect: onFirstConnect
      }
    });

  cfg.blindMode = $('body').hasClass('blind-mode');
  cfg.trans = lichess.trans(cfg.i18n);
  cfg.socketSend = lichess.socket.send;
  cfg.element = element;
  lobby = LichessLobby.start(cfg);

  const $startButtons = $('.lobby__start'),
  clickEvent = cfg.blindMode ? 'click' : 'mousedown';

  $startButtons.find('a:not(.disabled)').on(clickEvent, function() {
    $(this).addClass('active').siblings().removeClass('active');
    lichess.loadCssPath('lobby.setup');
    lobby.leavePool();
    $.ajax({
      url: $(this).attr('href'),
      success: function(html) {
        lobby.setup.prepareForm($.modal(html, 'game-setup', () => {
          $startButtons.find('.active').removeClass('active');
        }));
        lichess.pubsub.emit('content_loaded');
      },
      error: function(res) {
        if (res.status == 400) alert(res.responseText);
        lichess.reload();
      }
    });
    return false;
  }).on('click', function() {
    return false;
  });

  if (['#ai', '#friend', '#hook'].includes(location.hash)) {
    $startButtons
      .find('.config_' + location.hash.replace('#', ''))
      .each(function() {
        $(this).attr("href", $(this).attr("href") + location.search);
      }).trigger(clickEvent);

    if (location.hash === '#hook') {
      if (/time=realTime/.test(location.search))
        lobby.setTab('real_time');
      else if (/time=correspondence/.test(location.search))
        lobby.setTab('seeks');
    }

    history.replaceState(null, null, '/');
  }
};

function spreadNumber(el, nbSteps, getDuration) {
  let previous, displayed;
  const display = function(prev, cur, it) {
    var val = lichess.numberFormat(Math.round(((prev * (nbSteps - 1 - it)) + (cur * (it + 1))) / nbSteps));
    if (val !== displayed) {
      el.textContent = val;
      displayed = val;
    }
  };
  let timeouts = [];
  return function(nb, overrideNbSteps) {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    let prev = previous === 0 ? 0 : (previous || nb);
    previous = nb;
    let interv = Math.abs(getDuration() / nbSteps);
    for (let i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
  };
}
