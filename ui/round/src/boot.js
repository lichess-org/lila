module.exports = function(cfg, element) {
  var data = cfg.data;
  lichess.openInMobileApp(data.game.id);
  var round, chat;
  if (data.tournament) $('body').data('tournament-id', data.tournament.id);
  lichess.socket = lichess.StrongSocket(
    data.url.socket,
    data.player.version, {
      options: {
        name: "round"
      },
      params: {
        userTv: data.userTv && data.userTv.id
      },
      receive: function(t, d) {
        round.socketReceive(t, d);
      },
      events: {
        crowd: function(e) {
          $watchers.watchers("set", e.watchers);
        },
        tvSelect: function(o) {
          if (data.tv && data.tv.channel == o.channel) lichess.reload();
          else $('#tv_channels a.' + o.channel + ' span').html(
            o.player ? [
              o.player.title,
              o.player.name,
              '(' + o.player.rating + ')'
            ].filter(function(x) {
              return x;
            }).join('&nbsp') : 'Anonymous');
        },
        end: function() {
          var url = '/' + (data.tv ? ['tv', data.tv.channel, data.game.id, data.player.color, 'sides'] : [data.game.id, data.player.color, 'sides', data.player.spectator ? 'watcher' : 'player']).join('/');
          $.ajax({
            url: url,
            success: function(html) {
              var $html = $(html);
              $('#site_header div.side').replaceWith($html.find('>.side'));
              $('#lichess div.crosstable').replaceWith($html.find('>.crosstable'));
              lichess.pubsub.emit('content_loaded')();
              startTournamentClock();
            }
          });
        },
        tournamentStanding: function(id) {
          if (data.tournament && id === data.tournament.id) $.ajax({
            url: '/tournament/' + id + '/game-standing',
            success: function(html) {
              $('#site_header div.game_tournament').replaceWith(html);
              startTournamentClock();
            }
          });
        }
      }
    });

  var startTournamentClock = function() {
    $("div.game_tournament div.clock").each(function() {
      $(this).clock({
        time: parseFloat($(this).data("time"))
      });
    });
  };
  var getPresetGroup = function(d) {
    if (d.player.spectator) return null;
    if (d.steps.length < 4) return 'start';
    else if (d.game.status.id >= 30) return 'end';
    return null;
  };
  cfg.element = element.querySelector('.round');
  cfg.socketSend = lichess.socket.send;
  cfg.onChange = function(d) {
    if (chat) chat.preset.setGroup(getPresetGroup(d));
  };
  // cfg.isGuineaPig = $('body').data('guineapig');
  round = LichessRound(cfg);
  if (cfg.chat) {
    cfg.chat.preset = getPresetGroup(cfg.data);
    cfg.chat.parseMoves = true;
    lichess.makeChat('chat', cfg.chat, function(c) {
      chat = c;
    });
  }
  $('.crosstable', element).prependTo($('.underboard .center', element)).removeClass('none');
  var $watchers = $('#site_header div.watchers').watchers();
  startTournamentClock();
  $('#now_playing').find('.move_on input').change(function() {
    round.moveOn.toggle();
  }).prop('checked', round.moveOn.get()).on('click', '>a', function() {
    lichess.hasToReload = true;
    return true;
  });
  if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
    history.replaceState(null, null, '/' + data.game.id);
  if (!data.player.spectator && data.game.status.id < 25) lichess.topMenuIntent();
};
