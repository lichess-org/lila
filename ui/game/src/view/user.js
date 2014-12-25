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
      config: function(el, isUpdate) {
        if (isUpdate) return;
        el.classList.add('ulpt');
      },
      class: 'text user_link ' + (player.user.online ? 'online is-green' : 'offline') + (klass ? ' ' + klass : ''),
      href: '/@/' + player.user.username,
      target: game.isPlayerPlaying(ctrl.data) ? '_blank' : null,
      'data-icon': 'r',
    }, [
      (player.user.title ? player.user.title + ' ' : '') + player.user.username,
      rating ? ' (' + rating + ')' : '',
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
