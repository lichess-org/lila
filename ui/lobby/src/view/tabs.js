var m = require('mithril');
var util = require('./util');

function tab(ctrl, key, active, content) {
  var attrs = {
    config: util.bindOnce('mousedown', lichess.partial(ctrl.setTab, key))
  }
  if (key === active) attrs.class = 'active';
  else if (key === 'pools' && ctrl.vm.poolMember) attrs.class = 'glow';
  return m('a', attrs, content);
}

module.exports = function(ctrl) {
  var myTurnPovsNb = ctrl.data.nowPlaying.filter(function(p) {
    return p.isMyTurn;
  }).length;
  var active = ctrl.vm.tab;
  return [
    tab(ctrl, 'pools', active, ctrl.trans.noarg('quickPairing')),
    tab(ctrl, 'real_time', active, ctrl.trans.noarg('lobby')),
    tab(ctrl, 'seeks', active, ctrl.trans.noarg('correspondence')),
    (active === 'now_playing' || ctrl.data.nbNowPlaying > 0) ? tab(ctrl, 'now_playing', active, [
      ctrl.trans('nbGamesInPlay', ctrl.data.nbNowPlaying),
      myTurnPovsNb > 0 ? m('span.unread', myTurnPovsNb) : null
    ]) : null
  ];
};
