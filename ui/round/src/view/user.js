var m = require('mithril');
var round = require('../round');

module.exports = function(ctrl, player, klass) {
  return player.user ? [
    m('a', {
      config: function(el, isUpdate) {
        if (isUpdate) return;
        el.classList.add('ulpt');
      },
      class: 'user_link ' + (player.user.online ? 'online is-green' : 'offline') + (klass ? ' ' + klass : ''),
      href: ctrl.router.User.show(player.user.username).url,
      target: round.playable(ctrl.data) ? '_blank' : null,
      'data-icon': 'r',
    }, [
      (player.user.title ? player.user.title + ' ' : '') + player.user.username,
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
