var m = require('mithril');
var game = require('game').game;

function ratingDiff(player) {
  if (typeof player.ratingDiff === 'undefined') return null;
  if (player.ratingDiff === 0) return m('span.rp.null', '±0');
  if (player.ratingDiff > 0) return m('span.rp.up', '+' + player.ratingDiff);
  if (player.ratingDiff < 0) return m('span.rp.down', player.ratingDiff);
}

function relayUser(player, klass) {
  return m('span', {
    class: 'text ' + klass,
    'data-icon': '8'
  }, [
    (player.title ? player.title + ' ' : '') + player.name,
    player.rating ? ' (' + player.rating + ')' : ''
  ]);
}

module.exports = function(ctrl, player, klass) {
  var d = ctrl.data;
  if (d.relay) return relayUser(d.relay[player.color], klass);
  var perf = player.user ? player.user.perfs[d.game.perf] : null;
  var rating = player.rating ? player.rating : (perf ? perf.rating : null);
  var playerOnGameIcon = m('span.status.hint--top', {
    'data-hint': 'Player' + (player.onGame ? ' has joined the game' : ' has left the game')
  }, (player.onGame || !ctrl.vm.firstSeconds) ? m('span', {
    'data-icon': (player.onGame ? '3' : '0')
  }) : null)
  return player.user ? [
    m('a', {
      class: 'text ulpt user_link ' + (player.user.online ? 'online' : 'offline') + (klass ? ' ' + klass : ''),
      href: '/@/' + player.user.username,
      target: game.isPlayerPlaying(d) ? '_blank' : '_self',
      title: player.provisional ? 'Provisional rating' : null
    }, [
      m('i', {
        class: 'line' + (player.user.patron ? ' patron' : '')
      }),
      (player.user.title ? player.user.title + ' ' : '') + player.user.username,
      rating ? ' (' + rating + (player.provisional ? '?' : '') + ')' : '',
      ratingDiff(player),
      player.engine ? m('span[data-icon=j]', {
        title: ctrl.trans('thisPlayerUsesChessComputerAssistance')
      }) : null
    ]),
    playerOnGameIcon
  ] : m('span.user_link', [
    player.name || 'Anonymous',
    d.game.source == 'relay' ? null : playerOnGameIcon
  ]);
}
