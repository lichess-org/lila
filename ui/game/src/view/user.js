var m = require('mithril');
var game = require('../game');

function ratingDiff(player) {
  if (typeof player.ratingDiff === 'undefined') return null;
  if (player.ratingDiff === 0) return m('span.rp.null', 0);
  if (player.ratingDiff > 0) return m('span.rp.up', '+' + player.ratingDiff);
  if (player.ratingDiff < 0) return m('span.rp.down', player.ratingDiff);
}

module.exports = function(ctrl, player, klass) {
  var perf = player.user ? player.user.perfs[ctrl.data.game.perf] : null;
  var rating = player.rating ? player.rating : (perf ? perf.rating : null);
  return player.user ? [
    m('a', {
      class: 'text ulpt user_link ' + (player.user.online ? 'online is-green' : 'offline') + (klass ? ' ' + klass : ''),
      href: '/@/' + player.user.username,
      target: game.isPlayerPlaying(ctrl.data) ? '_blank' : '_self',
      'data-icon': 'r',
      title: player.provisional ? 'Provisional rating' : null
    }, [
      (player.user.title ? player.user.title + ' ' : '') + player.user.username,
      rating ? ' (' + rating + (player.provisional ? '?' : '') + ')' : '',
      ratingDiff(player),
      player.engine ? m('span[data-icon=j]', {
        title: ctrl.trans('thisPlayerUsesChessComputerAssistance')
      }) : null
    ]),
    m('span.status')
  ] : m('span.user_link', [
    'Anonymous',
    m('span.status')
  ]);
}
