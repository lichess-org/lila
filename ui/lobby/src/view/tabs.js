var m = require('mithril');
var util = require('chessground').util;

function tab(ctrl, key, active, content) {
  var attrs = {
    onmousedown: util.partial(ctrl.setTab, key)
  }
  if (key === active) attrs.class = 'active';
  return m('a', attrs, content);
}

module.exports = function(ctrl) {
  var myTurnPovsNb = ctrl.data.nowPlaying.filter(function(p) {
    return p.isMyTurn;
  }).length;
  var active = ctrl.vm.tab;
  return [
    tab(ctrl, 'real_time', active, ctrl.trans('realTime')),
    tab(ctrl, 'seeks', active, ctrl.trans('correspondence')),
    (active === 'now_playing' || ctrl.data.nbNowPlaying > 0) ? tab(ctrl, 'now_playing', active, [
      ctrl.trans('nbGamesInPlay', ctrl.data.nbNowPlaying),
      myTurnPovsNb > 0 ? m('span.unread', myTurnPovsNb) : null
    ]) : null
  ];
};
